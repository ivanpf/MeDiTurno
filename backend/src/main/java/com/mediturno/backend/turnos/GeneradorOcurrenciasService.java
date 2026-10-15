package com.mediturno.backend.turnos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;

/**
 * Genera las ocurrencias (las fechas concretas en las que se atiende y se puede reservar) de los turnos semanales.
 * Lo usan la tarea programada (todos los turnos activos, ver {@code TareasProgramadas}) y el alta de un turno (CU-12,
 * solo el turno nuevo). Es seguro repetirlo: solo crea las que todavía no existen.
 * <p>
 * Regla de la fecha de hoy ({@link DisponibilidadService#turnoTodaviaNoEmpezo}): se genera únicamente si el turno todavía
 * no empezó. Las fechas de días posteriores se generan siempre.
 */
@Service
public class GeneradorOcurrenciasService {

    private static final Logger log = LoggerFactory.getLogger(GeneradorOcurrenciasService.class);

    private final TurnoSemanalRepository turnoSemanalRepository;
    private final OcurrenciaTurnoRepository ocurrenciaTurnoRepository;
    private final DisponibilidadService disponibilidadService;
    private final TransactionTemplate transaccion;

    public GeneradorOcurrenciasService(TurnoSemanalRepository turnoSemanalRepository,
            OcurrenciaTurnoRepository ocurrenciaTurnoRepository, DisponibilidadService disponibilidadService,
            PlatformTransactionManager administradorDeTransacciones) {
        this.turnoSemanalRepository = turnoSemanalRepository;
        this.ocurrenciaTurnoRepository = ocurrenciaTurnoRepository;
        this.disponibilidadService = disponibilidadService;
        this.transaccion = new TransactionTemplate(administradorDeTransacciones);
    }

    /**
     * Crea las ocurrencias que falten, desde el momento {@code ahora} hasta {@code diasHaciaAdelante} días después, para
     * todos los turnos activos. Cada turno se procesa en su propia transacción, así que uno con problemas no impide
     * generar los demás. Devuelve cuántas creó.
     */
    public int generarOcurrencias(LocalDateTime ahora, int diasHaciaAdelante) {
        LocalDate hoy = ahora.toLocalDate();
        LocalDate hasta = hoy.plusDays(diasHaciaAdelante);
        int creadas = 0;
        for (Long idTurno : turnoSemanalRepository.idsConVigenciaEntre(EstadoTurnoSemanal.ACTIVO, hoy, hasta)) {
            try {
                Integer delTurno = transaccion.execute(estado -> generarDeUnTurnoActivo(idTurno, ahora, diasHaciaAdelante));
                creadas += delTurno == null ? 0 : delTurno;
            } catch (RuntimeException e) {
                log.error("No se pudieron generar las ocurrencias del turno {}: {}", idTurno, e.getMessage());
            }
        }
        return creadas;
    }

    /**
     * Crea las ocurrencias que falten de un turno. Se ejecuta dentro de la transacción de quien lo llama (por ejemplo, el
     * alta del turno), de modo que el turno y sus fechas se guardan juntos o no se guarda ninguno.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int generarOcurrenciasDe(TurnoSemanal turno, LocalDateTime ahora, int diasHaciaAdelante) {
        LocalDate hoy = ahora.toLocalDate();
        LocalDate hasta = hoy.plusDays(diasHaciaAdelante);
        LocalDate desde = hoy.isAfter(turno.getVigenteDesde()) ? hoy : turno.getVigenteDesde();
        LocalDate limite = turno.getVigenteHasta() != null && turno.getVigenteHasta().isBefore(hasta) ? turno.getVigenteHasta() : hasta;

        Set<LocalDate> existentes = new HashSet<>(ocurrenciaTurnoRepository.fechasExistentes(turno.getIdTurnoSemanal(), desde, limite));
        List<OcurrenciaTurno> nuevas = new ArrayList<>();
        LocalDate fecha = desde.with(TemporalAdjusters.nextOrSame(turno.getDia().aDayOfWeek()));
        for (; !fecha.isAfter(limite); fecha = fecha.plusWeeks(1)) {
            if (!existentes.contains(fecha) && disponibilidadService.turnoTodaviaNoEmpezo(turno, fecha, ahora)) {
                nuevas.add(nuevaOcurrencia(turno, fecha));
            }
        }
        ocurrenciaTurnoRepository.saveAll(nuevas);
        return nuevas.size();
    }

    private int generarDeUnTurnoActivo(Long idTurno, LocalDateTime ahora, int diasHaciaAdelante) {
        TurnoSemanal turno = turnoSemanalRepository.findById(idTurno).orElse(null);
        if (turno == null || turno.getEstado() != EstadoTurnoSemanal.ACTIVO) {
            return 0; // cambió desde que se listó (por ejemplo, lo suspendieron)
        }
        return generarOcurrenciasDe(turno, ahora, diasHaciaAdelante);
    }

    /** La ocurrencia guarda una copia del cupo y del horario del turno: si el turno cambia, las ya creadas no se modifican. */
    private static OcurrenciaTurno nuevaOcurrencia(TurnoSemanal turno, LocalDate fecha) {
        OcurrenciaTurno ocurrencia = new OcurrenciaTurno();
        ocurrencia.setTurnoSemanal(turno);
        ocurrencia.setFecha(fecha);
        ocurrencia.setCupos(turno.getCupoMaximo());
        ocurrencia.setHoraInicio(turno.getHoraInicio());
        ocurrencia.setHoraFin(turno.getHoraFin());
        ocurrencia.setOcurrenciaTurnoEstado(EstadoOcurrenciaTurno.HABILITADA);
        return ocurrencia;
    }
}
