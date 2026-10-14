package com.mediturno.backend.turnos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.dto.AltaTurnoSemanalDTO;
import com.mediturno.backend.turnos.dto.OcurrenciaTurnoDTO;
import com.mediturno.backend.turnos.dto.RehabilitacionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoResponseDTO;
import com.mediturno.backend.turnos.dto.TurnoSemanalResponseDTO;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.repository.MedicoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-8, CU-12, CU-13, CU-14, CU-20.
 * Los métodos que todavía no están implementados lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class TurnoSemanalService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurnoSemanalRepository turnoSemanalRepository;
    private final MedicoRepository medicoRepository;
    private final ConsultorioRepository consultorioRepository;
    private final GeneradorOcurrenciasService generadorOcurrencias;
    private final int semanasHaciaAdelante;

    public TurnoSemanalService(TurnoSemanalRepository turnoSemanalRepository, MedicoRepository medicoRepository,
            ConsultorioRepository consultorioRepository, GeneradorOcurrenciasService generadorOcurrencias,
            @Value("${mediturno.reservas.semanas-adelante}") int semanasHaciaAdelante) {
        this.turnoSemanalRepository = turnoSemanalRepository;
        this.medicoRepository = medicoRepository;
        this.consultorioRepository = consultorioRepository;
        this.generadorOcurrencias = generadorOcurrencias;
        this.semanasHaciaAdelante = semanasHaciaAdelante;
    }

    /**
     * CU-12. Crea un turno semanal por cada día elegido, todos o ninguno, y genera sus fechas reservables. Los campos
     * obligatorios los valida el controlador (@Valid). Se bloquean el médico y el consultorio hasta el fin de la transacción: la búsqueda de
     * superposiciones y el guardado no pueden intercalarse con otra alta del mismo médico o consultorio.
     */
    @Transactional
    public List<TurnoSemanalResponseDTO> altaTurnoSemanal(AltaTurnoSemanalDTO datos) {
        validarHorario(datos);
        Medico medico = medicoRepository.buscarConLock(datos.idMedico())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El médico seleccionado no existe."));
        Consultorio consultorio = consultorioRepository.buscarConLock(datos.codigoConsultorio())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El consultorio seleccionado no existe."));
        Especialidad especialidad = validarMedicoEspecialidadYConsultorio(medico, consultorio, datos.codigoEspecialidad());

        List<DiaSemana> dias = datos.dias().stream().distinct().sorted().toList();
        validarSinSuperposiciones(datos, dias);

        List<TurnoSemanal> turnos = new ArrayList<>();
        for (DiaSemana dia : dias) {
            TurnoSemanal turno = new TurnoSemanal();
            turno.setMedico(medico);
            turno.setEspecialidad(especialidad);
            turno.setConsultorio(consultorio);
            turno.setDia(dia);
            turno.setHoraInicio(datos.horaInicio());
            turno.setHoraFin(datos.horaFin());
            turno.setCupoMaximo(datos.cupoMaximo());
            turno.setVigenteDesde(datos.vigenteDesde());
            turno.setEstado(EstadoTurnoSemanal.ACTIVO);
            turnos.add(turno);
        }
        List<TurnoSemanal> guardados = turnoSemanalRepository.saveAll(turnos);

        // Las fechas hasta donde se puede reservar se generan ahora, en la misma transacción: el turno y sus fechas se guardan
        // juntos o no se guarda ninguno. Así un turno de hoy (que todavía no empezó) ya se puede reservar.
        LocalDateTime ahora = LocalDateTime.now();
        for (TurnoSemanal turno : guardados) {
            generadorOcurrencias.generarOcurrenciasDe(turno, ahora, semanasHaciaAdelante * 7);
        }
        return guardados.stream().map(TurnoSemanalService::convertirATurnoSemanalResponseDTO).toList();
    }

    /** Datos inválidos que no se ven mirando un solo campo */
    private static void validarHorario(AltaTurnoSemanalDTO datos) {
        if (!datos.horaFin().isAfter(datos.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior a la hora de inicio.");
        }
    }

    /** El médico tiene que estar activo y tener esa especialidad, y el consultorio tiene que estar activo. */
    private static Especialidad validarMedicoEspecialidadYConsultorio(Medico medico, Consultorio consultorio, String codigoEspecialidad) {
        if (medico.getEstado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El médico seleccionado está inactivo.");
        }
        Especialidad especialidad = medico.getEspecialidades().stream()
            .filter(e -> e.getCodigo().equals(codigoEspecialidad))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                "El médico seleccionado no tiene asignada la especialidad indicada."));
        if (consultorio.getEstado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El consultorio seleccionado está inactivo.");
        }
        return especialidad;
    }

    /** Ningún día puede superponerse con otro turno del mismo médico o del mismo consultorio. */
    private void validarSinSuperposiciones(AltaTurnoSemanalDTO datos, List<DiaSemana> dias) {
        List<String> conflictos = new ArrayList<>();
        for (DiaSemana dia : dias) {
            for (TurnoSemanal existente : turnoSemanalRepository.buscarSuperposiciones(
                    dia, datos.idMedico(), datos.codigoConsultorio(), datos.horaInicio(), datos.horaFin(), datos.vigenteDesde())) {
                conflictos.add(describir(existente));
            }
        }
        if (!conflictos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Superposición con: " + String.join("; ", conflictos) + ".");
        }
    }

    private static String describir(TurnoSemanal t) {
        return nombreDe(t.getMedico()) + " · " + nombreDelDia(t.getDia()) + " " + t.getHoraInicio().format(HORA) + "-"
            + t.getHoraFin().format(HORA) + " · " + t.getConsultorio().getNombre() + " (turno #" + t.getIdTurnoSemanal() + ")";
    }

    private static String nombreDelDia(DiaSemana dia) {
        return switch (dia) {
            case LUNES -> "Lunes";
            case MARTES -> "Martes";
            case MIERCOLES -> "Miércoles";
            case JUEVES -> "Jueves";
            case VIERNES -> "Viernes";
            case SABADO -> "Sábado";
            case DOMINGO -> "Domingo";
        };
    }

    private static String nombreDe(Medico medico) {
        return medico.getApellido() + ", " + medico.getNombre();
    }

    private static TurnoSemanalResponseDTO convertirATurnoSemanalResponseDTO(TurnoSemanal t) {
        return new TurnoSemanalResponseDTO(t.getIdTurnoSemanal(), t.getMedico().getIdUsuario(), nombreDe(t.getMedico()),
            t.getEspecialidad().getCodigo(), t.getEspecialidad().getNombre(), t.getConsultorio().getCodigo(),
            t.getConsultorio().getNombre(), t.getDia(), t.getHoraInicio(), t.getHoraFin(), t.getCupoMaximo(),
            t.getVigenteDesde(), t.getVigenteHasta(), t.getEstado());
    }

    /** CU-13. */
    public SuspensionTurnoResponseDTO suspenderTurno(Long idTurnoSemanal, SuspensionTurnoDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-14. */
    public TurnoSemanalResponseDTO rehabilitarTurno(Long idTurnoSemanal, RehabilitacionTurnoDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-20. */
    public List<TurnoSemanalResponseDTO> buscarTurnosSemanales(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-8. */
    public List<OcurrenciaTurnoDTO> listarOcurrenciasDisponibles(String codigoEspecialidad, Long idPaciente) {
        throw new NoImplementadoException();
    }
}
