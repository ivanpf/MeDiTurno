package com.mediturno.backend.estadisticas;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.busqueda.FiltrosBusqueda;
import com.mediturno.backend.common.dto.PeriodoDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaOcupacionDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaReservaDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaTurnoDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;

/**
 * Lógica de negocio de: CU-40, CU-41, CU-42. Son consultas de solo lectura; el período se mide por la fecha de atención del
 * turno (ver {@link PeriodoDTO}).
 */
@Service
@Transactional(readOnly = true)
public class EstadisticaService {

    // Sin límite de un lado del período: las fechas extremas que acepta PostgreSQL sirven de tope.
    private static final LocalDate SIN_LIMITE_DESDE = LocalDate.of(1900, 1, 1);
    private static final LocalDate SIN_LIMITE_HASTA = LocalDate.of(9999, 12, 31);

    // Un solo Collator: crearlo en cada comparación del ordenamiento es trabajo de más.
    private static final Collator ALFABETICO = Collator.getInstance(Locale.forLanguageTag("es"));

    /** Las reservas de un turno: todas, las que todavía no se atendieron (vigentes o con orden emitida) y las que conservan su número. */
    private record ReservasDeTurno(long total, long vigentes, long conservan) {
    }

    private static final ReservasDeTurno SIN_RESERVAS = new ReservasDeTurno(0, 0, 0);

    private final ReservaRepository reservaRepository;
    private final OcurrenciaTurnoRepository ocurrenciaTurnoRepository;
    private final TurnoSemanalRepository turnoSemanalRepository;

    public EstadisticaService(ReservaRepository reservaRepository, OcurrenciaTurnoRepository ocurrenciaTurnoRepository,
            TurnoSemanalRepository turnoSemanalRepository) {
        this.reservaRepository = reservaRepository;
        this.ocurrenciaTurnoRepository = ocurrenciaTurnoRepository;
        this.turnoSemanalRepository = turnoSemanalRepository;
    }

    /** CU-40. */
    public List<EstadisticaReservaDTO> estadisticaReservasPorEspecialidad(PeriodoDTO periodo) {
        LocalDate desde = desde(periodo);
        LocalDate hasta = hasta(periodo);
        validarRango(desde, hasta);
        Collection<EstadoReserva> estados = interpretarEstadoReserva(periodo.estado());
        boolean porNombre = ordenPorNombre(periodo.ordenarPor());
        boolean descendente = periodo.descendente() != null ? periodo.descendente() : !porNombre;

        List<Object[]> filas = reservaRepository.contarPorEspecialidad(desde, hasta, estados);
        long total = filas.stream().mapToLong(f -> (Long) f[2]).sum();
        Comparator<EstadisticaReservaDTO> orden = porNombre
            ? Comparator.comparing(EstadisticaReservaDTO::nombreEspecialidad, ALFABETICO)
            : Comparator.comparingLong(EstadisticaReservaDTO::cantidadReservas)
                .thenComparing(EstadisticaReservaDTO::nombreEspecialidad, ALFABETICO.reversed());
        return filas.stream()
            .map(f -> convertirAEstadisticaReservaDTO((String) f[0], (String) f[1], (Long) f[2], total))
            .sorted(descendente ? orden.reversed() : orden)
            .toList();
    }

    /** CU-41. */
    public List<EstadisticaTurnoDTO> estadisticaTurnosMasDemandados(PeriodoDTO periodo) {
        LocalDate desde = desde(periodo);
        LocalDate hasta = hasta(periodo);
        validarRango(desde, hasta);
        boolean descendente = periodo.descendente() == null || periodo.descendente();
        FiltrosBusqueda.elegirOrden(periodo.ordenarPor(), "reservas");

        Map<Long, ReservasDeTurno> reservas = reservasPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), null, true);
        Map<Long, Long> cupos = cuposPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), null);
        // Un turno con fechas en el período entra al ranking aunque no haya recibido reservas (con 0), para ver los menos demandados.
        Set<Long> ids = new HashSet<>(reservas.keySet());
        ids.addAll(cupos.keySet());

        Comparator<EstadisticaTurnoDTO> orden = Comparator.comparingLong(EstadisticaTurnoDTO::cantidadReservas);
        return turnos(ids).stream()
            .map(t -> convertirAEstadisticaTurnoDTO(t, reservas.getOrDefault(t.getIdTurnoSemanal(), SIN_RESERVAS).total()))
            .sorted((descendente ? orden.reversed() : orden).thenComparing(this::desempate))
            .toList();
    }

    /** CU-42. */
    public List<EstadisticaOcupacionDTO> estadisticaOcupacionTurnos(PeriodoDTO periodo) {
        LocalDate desde = desde(periodo);
        LocalDate hasta = hasta(periodo);
        validarRango(desde, hasta);
        boolean descendente = periodo.descendente() == null || periodo.descendente();
        FiltrosBusqueda.elegirOrden(periodo.ordenarPor(), "porcentaje");

        Map<Long, Long> cupos = cuposPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), periodo.codigoConsultorio());
        // Mismas fechas que los cupos ofrecidos: sin las suspendidas.
        Map<Long, ReservasDeTurno> reservas = reservasPorTurno(desde, hasta, periodo.codigoEspecialidad(), periodo.idMedico(), periodo.codigoConsultorio(), false);

        List<EstadisticaOcupacionDTO> resultado = new ArrayList<>();
        for (TurnoSemanal t : turnos(cupos.keySet())) {
            ReservasDeTurno r = reservas.getOrDefault(t.getIdTurnoSemanal(), SIN_RESERVAS);
            resultado.add(convertirAEstadisticaOcupacionDTO(t, cupos.get(t.getIdTurnoSemanal()), r.vigentes(), r.conservan()));
        }
        Comparator<EstadisticaOcupacionDTO> orden = Comparator.comparingDouble(EstadisticaOcupacionDTO::porcentajeOcupacion);
        Comparator<EstadisticaOcupacionDTO> ordenFinal = (descendente ? orden.reversed() : orden)
            .thenComparing(EstadisticaOcupacionDTO::nombreMedico, ALFABETICO)
            .thenComparing(EstadisticaOcupacionDTO::dia)
            .thenComparing(EstadisticaOcupacionDTO::horaInicio);
        resultado.sort(ordenFinal);
        return resultado;
    }

    private Map<Long, ReservasDeTurno> reservasPorTurno(LocalDate desde, LocalDate hasta, String especialidad, Long medico,
            String consultorio, boolean incluirSuspendidas) {
        Map<Long, ReservasDeTurno> porTurno = new HashMap<>();
        for (Object[] f : reservaRepository.contarPorTurno(desde, hasta, vacioANulo(especialidad), medico, vacioANulo(consultorio), incluirSuspendidas)) {
            porTurno.put((Long) f[0], new ReservasDeTurno((Long) f[1], (Long) f[2], (Long) f[3]));
        }
        return porTurno;
    }

    private Map<Long, Long> cuposPorTurno(LocalDate desde, LocalDate hasta, String especialidad, Long medico, String consultorio) {
        Map<Long, Long> porTurno = new HashMap<>();
        for (Object[] f : ocurrenciaTurnoRepository.sumarCuposPorTurno(desde, hasta, vacioANulo(especialidad), medico, vacioANulo(consultorio))) {
            porTurno.put((Long) f[0], (Long) f[1]);
        }
        return porTurno;
    }

    private List<TurnoSemanal> turnos(Set<Long> ids) {
        return ids.isEmpty() ? List.of() : turnoSemanalRepository.buscarConRelaciones(ids);
    }

    /** Ante igual demanda: médico, día y hora de inicio. */
    private int desempate(EstadisticaTurnoDTO a, EstadisticaTurnoDTO b) {
        int porMedico = ALFABETICO.compare(a.nombreMedico(), b.nombreMedico());
        if (porMedico != 0) {
            return porMedico;
        }
        int porDia = a.dia().compareTo(b.dia());
        return porDia != 0 ? porDia : a.horaInicio().compareTo(b.horaInicio());
    }

    private EstadisticaReservaDTO convertirAEstadisticaReservaDTO(String codigo, String nombre, long cantidad, long total) {
        return new EstadisticaReservaDTO(codigo, nombre, cantidad, porcentaje(cantidad, total));
    }

    private EstadisticaTurnoDTO convertirAEstadisticaTurnoDTO(TurnoSemanal t, long cantidadReservas) {
        return new EstadisticaTurnoDTO(t.getIdTurnoSemanal(), t.getEspecialidad().getCodigo(), t.getEspecialidad().getNombre(),
            t.getMedico().getIdUsuario(), t.getMedico().nombreCompleto(), t.getDia(), t.getHoraInicio(), t.getHoraFin(),
            t.getConsultorio().getCodigo(), t.getConsultorio().getNombre(), t.getCupoMaximo(), cantidadReservas);
    }

    private EstadisticaOcupacionDTO convertirAEstadisticaOcupacionDTO(TurnoSemanal t, long ofrecidos, long vigentes, long reservados) {
        return new EstadisticaOcupacionDTO(t.getIdTurnoSemanal(), t.getEspecialidad().getCodigo(), t.getEspecialidad().getNombre(),
            t.getMedico().getIdUsuario(), t.getMedico().nombreCompleto(), t.getDia(), t.getHoraInicio(), t.getHoraFin(),
            t.getConsultorio().getCodigo(), t.getConsultorio().getNombre(), ofrecidos, reservados, vigentes,
            ofrecidos - reservados, porcentaje(reservados, ofrecidos));
    }

    /** Porcentaje con dos decimales; 0 si no hay con qué compararlo. */
    private static double porcentaje(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 10000.0 / total) / 100.0;
    }

    private static LocalDate desde(PeriodoDTO periodo) {
        return periodo.fechaDesde() != null ? periodo.fechaDesde() : SIN_LIMITE_DESDE;
    }

    private static LocalDate hasta(PeriodoDTO periodo) {
        return periodo.fechaHasta() != null ? periodo.fechaHasta() : SIN_LIMITE_HASTA;
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private static boolean ordenPorNombre(String ordenarPor) {
        return "nombre".equals(FiltrosBusqueda.elegirOrden(ordenarPor, "cantidad", "nombre"));
    }

    /** El estado pedido, o todos si no se indicó; 400 si no existe. */
    private static Collection<EstadoReserva> interpretarEstadoReserva(String texto) {
        EstadoReserva estado = FiltrosBusqueda.interpretarEstado(EstadoReserva.class, texto);
        return estado == null ? List.of(EstadoReserva.values()) : List.of(estado);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
