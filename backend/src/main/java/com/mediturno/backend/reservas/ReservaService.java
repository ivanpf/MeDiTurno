package com.mediturno.backend.reservas;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.pases.PaseService;
import com.mediturno.backend.reservas.dto.ReservaRequestDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.DisponibilidadService;
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
 * CU-8 implementado. Los demás casos de uso conservan su respuesta 501.
 */
@Service
public class ReservaService {
    private final OcurrenciaTurnoRepository ocurrencias;
    private final ReservaRepository reservas;
    private final PacienteRepository pacientes;
    private final PaseService pases;
    private final DisponibilidadService disponibilidad;
    private final Clock reloj;

    public ReservaService(OcurrenciaTurnoRepository ocurrencias,
            ReservaRepository reservas,
            PacienteRepository pacientes,
            PaseService pases,
            DisponibilidadService disponibilidad, Clock reloj) {
        this.ocurrencias = ocurrencias;
        this.reservas = reservas;
        this.pacientes = pacientes;
        this.pases = pases;
        this.disponibilidad = disponibilidad;
        this.reloj = reloj;
    }

    /** CU-8. */
    @Transactional
    public ReservaResponseDTO reservarOrden(Long idPaciente, ReservaRequestDTO datos) {
        var paciente = pacientes.findById(idPaciente).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El paciente no existe"));
        if (paciente.getEstado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está habilitada");
        }
        // Serializa todas las reservas de una misma ocurrencia antes de comprobar el número.
        var ocurrencia = ocurrencias.bloquearPorId(datos.idOcurrenciaTurno()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El turno no existe"));
        if (!disponibilidad.habilitada(ocurrencia)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya no está disponible");
        }
        if (datos.numero() < 1 || datos.numero() > ocurrencia.getCupos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número no pertenece al turno");
        }
        if (!disponibilidad.numeroFuturo(ocurrencia, datos.numero())
                || reservas.numerosOcupados(ocurrencia.getIdOcurrenciaTurno()).contains(datos.numero())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El número ya no está disponible");
        }
        var reserva = new Reserva();
        var especialidad = ocurrencia.getTurnoSemanal().getEspecialidad();
        if (especialidad.isRequierePase()) reserva.setPase(pases.obtenerPaseParaReserva(idPaciente, especialidad.getCodigo()));
        reserva.setPaciente(paciente);
        reserva.setOcurrenciaTurno(ocurrencia);
        reserva.setNumero(datos.numero());
        reserva.setReservaEstado(EstadoReserva.VIGENTE);
        reserva.setFechaReserva(LocalDateTime.now(reloj));
        try {
            reservas.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException error) {
            // Segunda barrera: la restricción parcial de PostgreSQL protege el mismo número.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No fue posible reservar el número seleccionado");
        }
        return new ReservaResponseDTO(reserva.getIdReserva(), reserva.getNumero(), disponibilidad.horaEstimada(ocurrencia, reserva.getNumero()));
    }

    /** CU-9. */
    public void cancelarReserva(Long idReserva, Long idPaciente) {
        throw new NoImplementadoException();
    }

    /** CU-15. */
    public ReservaResponseDTO buscarReservaPorId(Long idReserva) {
        throw new NoImplementadoException();
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
