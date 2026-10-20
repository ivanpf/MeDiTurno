package com.mediturno.backend.reservas;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.ordenes.repository.OrdenRepository;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.pases.PaseService;
import com.mediturno.backend.reservas.dto.ReservaRequestDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.DisponibilidadService;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-8, CU-9, CU-10, CU-15, CU-19.
 * CU-8 y CU-15 implementados. Los demás casos de uso conservan su respuesta 501.
 */
@Service
public class ReservaService {
    private final OcurrenciaTurnoRepository ocurrenciaTurnoRepository;
    private final ReservaRepository reservaRepository;
    private final PacienteRepository pacienteRepository;
    private final OrdenRepository ordenRepository;
    private final PaseService paseService;
    private final DisponibilidadService disponibilidadService;
    private final Clock reloj;

    public ReservaService(OcurrenciaTurnoRepository ocurrenciaTurnoRepository,
            ReservaRepository reservaRepository,
            PacienteRepository pacienteRepository,
            OrdenRepository ordenRepository,
            PaseService paseService,
            DisponibilidadService disponibilidadService, Clock reloj) {
        this.ocurrenciaTurnoRepository = ocurrenciaTurnoRepository;
        this.reservaRepository = reservaRepository;
        this.pacienteRepository = pacienteRepository;
        this.ordenRepository = ordenRepository;
        this.paseService = paseService;
        this.disponibilidadService = disponibilidadService;
        this.reloj = reloj;
    }

    /** CU-8. */
    @Transactional
    public ReservaResponseDTO reservarOrden(Long idPaciente, ReservaRequestDTO datos) {
        Paciente paciente = pacienteRepository.findById(idPaciente).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El paciente no existe"));
        if (paciente.getEstado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está habilitada");
        }
        // Serializa todas las reservas de una misma ocurrencia antes de comprobar el número.
        OcurrenciaTurno ocurrencia = ocurrenciaTurnoRepository.buscarConLock(datos.idOcurrenciaTurno()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El turno no existe"));
        if (!disponibilidadService.estaHabilitada(ocurrencia)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya no está disponible");
        }
        if (datos.numero() < 1 || datos.numero() > ocurrencia.getCupos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número no pertenece al turno");
        }
        if (!disponibilidadService.esNumeroFuturo(ocurrencia, datos.numero())
                || reservaRepository.buscarNumerosOcupados(ocurrencia.getIdOcurrenciaTurno()).contains(datos.numero())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El número ya no está disponible");
        }
        Reserva reserva = new Reserva();
        Especialidad especialidad = ocurrencia.getTurnoSemanal().getEspecialidad();
        if (especialidad.isRequierePase()) {
            reserva.setPase(paseService.obtenerPaseParaReserva(idPaciente, especialidad.getCodigo()));
        }
        reserva.setPaciente(paciente);
        reserva.setOcurrenciaTurno(ocurrencia);
        reserva.setNumero(datos.numero());
        reserva.setReservaEstado(EstadoReserva.VIGENTE);
        reserva.setFechaReserva(LocalDateTime.now(reloj));
        try {
            reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException error) {
            // Segunda barrera: la restricción parcial de PostgreSQL protege el mismo número.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No fue posible reservar el número seleccionado");
        }
        return convertirAReservaResponseDTO(reserva, null);
    }

    /** CU-9. */
    public void cancelarReserva(Long idReserva, Long idPaciente) {
        throw new NoImplementadoException();
    }

    /**
     * CU-15. Muestra los datos de la reserva con cualquier estado: una cancelada, anulada o ya atendida se muestra igual, y la
     * pantalla no deja emitir la orden. Con un identificador que no es válido (vacío, cero o negativo) responde 400 sin consultar
     * la base; si no existe, 404.
     */
    @Transactional(readOnly = true)
    public ReservaResponseDTO buscarReservaPorId(Long idReserva) {
        validarFormatoIdentificador(idReserva);
        Reserva reserva = reservaRepository.findById(idReserva).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El identificador ingresado no existe"));
        Long numeroOrden = ordenRepository.findByReservaIdReserva(idReserva).map(o -> o.getNumeroOrden()).orElse(null);
        return convertirAReservaResponseDTO(reserva, numeroOrden);
    }

    /** Flujo 2.A: el identificador tiene que ser un número entero positivo. */
    private static void validarFormatoIdentificador(Long idReserva) {
        if (idReserva == null || idReserva < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador ingresado no es válido");
        }
    }

    private ReservaResponseDTO convertirAReservaResponseDTO(Reserva reserva, Long numeroOrden) {
        OcurrenciaTurno ocurrencia = reserva.getOcurrenciaTurno();
        TurnoSemanal turno = ocurrencia.getTurnoSemanal();
        Paciente paciente = reserva.getPaciente();
        return new ReservaResponseDTO(reserva.getIdReserva(), reserva.getNumero(),
                disponibilidadService.horaEstimada(ocurrencia, reserva.getNumero()), reserva.getReservaEstado(),
                paciente.nombreCompleto(), paciente.getCedula(), turno.getMedico().nombreCompleto(),
                turno.getEspecialidad().getNombre(), ocurrencia.getFecha(), ocurrencia.getHoraInicio(), ocurrencia.getHoraFin(),
                turno.getConsultorio().getNombre(), numeroOrden);
    }

    /** CU-19. */
    public List<ReservaResponseDTO> listarReservasParaAtencion(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-10. */
    public List<ReservaResponseDTO> consultarHistorialReservas(Long idPaciente, CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }
}
