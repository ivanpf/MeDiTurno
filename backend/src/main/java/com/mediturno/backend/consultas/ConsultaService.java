package com.mediturno.backend.consultas;

import java.util.List;
import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Collectors;
import com.mediturno.backend.consultas.dto.TurnoAtencionDTO;
import com.mediturno.backend.ordenes.repository.OrdenRepository;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.turnos.DisponibilidadService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.consultas.dto.ConsultaResponseDTO;
import com.mediturno.backend.consultas.dto.RegistrarConsultaDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-11, CU-21, CU-22, CU-23, CU-26.
 * CU-21 implementado. Los demás casos conservan su respuesta 501.
 */
@Service
public class ConsultaService {
    private final OcurrenciaTurnoRepository ocurrencias;
    private final ReservaRepository reservas;
    private final OrdenRepository ordenes;
    private final DisponibilidadService disponibilidad;
    private final Clock reloj;

    public ConsultaService(OcurrenciaTurnoRepository ocurrencias, ReservaRepository reservas,
            OrdenRepository ordenes, DisponibilidadService disponibilidad, Clock reloj) {
        this.ocurrencias = ocurrencias;
        this.reservas = reservas;
        this.ordenes = ordenes;
        this.disponibilidad = disponibilidad;
        this.reloj = reloj;
    }

    /** CU-21: al abrir la pantalla se muestran los turnos de hoy, según el reloj de la aplicación. */
    @Transactional(readOnly = true)
    public List<TurnoAtencionDTO> consultarTurnosDeAtencion(Long idMedico, LocalDate fecha) {
        return ocurrencias.buscarDelMedicoEnFecha(idMedico, fecha == null ? LocalDate.now(reloj) : fecha).stream()
            .map(o -> new TurnoAtencionDTO(o.getIdOcurrenciaTurno(), o.getFecha(), o.getHoraInicio(), o.getHoraFin(),
                o.getTurnoSemanal().getEspecialidad().getNombre(), o.getTurnoSemanal().getConsultorio().getNombre(),
                o.getOcurrenciaTurnoEstado()))
            .toList();
    }

    /** CU-23. */
    public ConsultaResponseDTO registrarConsulta(Long idMedico, Long numeroOrden, RegistrarConsultaDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-11. */
    public List<ConsultaResponseDTO> consultarHistoriaClinicaPropia(Long idPaciente, CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-22. */
    public List<ConsultaResponseDTO> consultarHistoriaClinicaDuranteAtencion(Long idMedico, Long idPaciente) {
        throw new NoImplementadoException();
    }

    /** CU-21. */
    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> consultarPacientesPorTurno(Long idMedico, Long idOcurrenciaTurno) {
        var ocurrencia = ocurrencias.buscarDelMedico(idOcurrenciaTurno, idMedico)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el turno solicitado."));
        var ordenesPorReserva = ordenes.buscarPorOcurrencia(idOcurrenciaTurno).stream()
            .collect(Collectors.toMap(o -> o.getReserva().getIdReserva(), o -> o));
        return reservas.buscarPacientesPorOcurrencia(idOcurrenciaTurno).stream().map(r -> {
            var paciente = r.getPaciente();
            var orden = ordenesPorReserva.get(r.getIdReserva());
            return new ReservaResponseDTO(r.getIdReserva(), r.getNumero(), disponibilidad.horaEstimada(ocurrencia, r.getNumero()),
                paciente.getIdUsuario(), paciente.getNombre() + " " + paciente.getApellido(), r.getReservaEstado(),
                orden == null ? null : orden.getNumeroOrden(), orden == null ? null : orden.getOrdenEstado());
        }).toList();
    }

    /** CU-26. */
    public List<ConsultaResponseDTO> consultarHistorialAtencionesPropias(Long idMedico, CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }
}
