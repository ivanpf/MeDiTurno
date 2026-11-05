package com.mediturno.backend.consultas;

import java.util.List;
import java.util.ArrayList;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import com.mediturno.backend.consultas.dto.TurnoAtencionDTO;
import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.consultas.model.HistoriaClinica;
import com.mediturno.backend.consultas.repository.ConsultaRepository;
import com.mediturno.backend.consultas.repository.HistoriaClinicaRepository;
import com.mediturno.backend.notificaciones.NotificacionService;
import com.mediturno.backend.ordenes.model.EstadoOrden;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.mediturno.backend.ordenes.repository.OrdenRepository;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.turnos.DisponibilidadService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.criteria.Predicate;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.consultas.dto.ConsultaResponseDTO;
import com.mediturno.backend.consultas.dto.RegistrarConsultaDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-11, CU-21, CU-22, CU-23, CU-26.
 * Implementados: CU-11, CU-21, CU-22, CU-23 y CU-26.
 */
@Service
public class ConsultaService {
    private static final Logger log = LoggerFactory.getLogger(ConsultaService.class);

    private final OcurrenciaTurnoRepository ocurrencias;
    private final ReservaRepository reservas;
    private final OrdenRepository ordenes;
    private final DisponibilidadService disponibilidad;
    private final ConsultaRepository consultas;
    private final HistoriaClinicaRepository historias;
    private final NotificacionService notificaciones;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public ConsultaService(OcurrenciaTurnoRepository ocurrencias, ReservaRepository reservas,
            OrdenRepository ordenes, DisponibilidadService disponibilidad, ConsultaRepository consultas,
            HistoriaClinicaRepository historias, NotificacionService notificaciones,
            PlatformTransactionManager administradorDeTransacciones, Clock reloj) {
        this.ocurrencias = ocurrencias;
        this.reservas = reservas;
        this.ordenes = ordenes;
        this.disponibilidad = disponibilidad;
        this.consultas = consultas;
        this.historias = historias;
        this.notificaciones = notificaciones;
        this.transaccion = new TransactionTemplate(administradorDeTransacciones);
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

    /**
     * CU-23. Registra la consulta realizada con la orden indicada, en una sola transacción: guarda la consulta en la historia
     * clínica del paciente (la abre si todavía no tenía), pasa la reserva a ATENDIDA y la orden a UTILIZADA. La reserva se
     * bloquea: dos registros simultáneos de la misma orden no generan dos consultas. La orden tiene que existir, ser de un
     * turno del médico y estar emitida (si no, 422; el mismo mensaje en todos los casos, para no revelar órdenes ajenas), y la
     * consulta no puede estar ya registrada (409). Los campos obligatorios se validan en el DTO (400).
     * El aviso al paciente se manda después de confirmar el registro: si falla, no lo revierte.
     */
    public ConsultaResponseDTO registrarConsulta(Long idMedico, Long numeroOrden, RegistrarConsultaDTO datos) {
        ConsultaResponseDTO registrada = transaccion.execute(estado -> guardarConsulta(idMedico, numeroOrden, datos));
        try {
            notificaciones.notificarConsultaRegistrada(registrada.idConsulta());
        } catch (RuntimeException e) {
            log.error("La consulta {} se registró, pero no se pudo avisar al paciente: {}", registrada.idConsulta(), e.getMessage());
        }
        return registrada;
    }

    private ConsultaResponseDTO guardarConsulta(Long idMedico, Long numeroOrden, RegistrarConsultaDTO datos) {
        var orden = ordenes.findById(numeroOrden)
            .filter(o -> o.getReserva().getOcurrenciaTurno().getTurnoSemanal().getMedico().getIdUsuario().equals(idMedico))
            .orElseThrow(ConsultaService::ordenNoValida);
        Reserva reserva = reservas.buscarConLock(orden.getReserva().getIdReserva()).orElseThrow(ConsultaService::ordenNoValida);
        if (consultas.existsByOrdenNumeroOrden(numeroOrden)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La consulta de esta orden ya fue registrada.");
        }
        if (reserva.getReservaEstado() != EstadoReserva.ORDEN_EMITIDA || orden.getOrdenEstado() != EstadoOrden.EMITIDA) {
            throw ordenNoValida();
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Paciente paciente = reserva.getPaciente();
        HistoriaClinica historia = historias.findFirstByPacienteIdUsuarioOrderByIdHistoriaClinicaAsc(paciente.getIdUsuario())
            .orElseGet(() -> abrirHistoriaClinica(paciente, ahora));
        historia.setFechaActualizacion(ahora);

        var turno = reserva.getOcurrenciaTurno().getTurnoSemanal();
        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);
        consulta.setMedico(turno.getMedico());
        consulta.setHistoriaClinica(historia);
        consulta.setReserva(reserva);
        consulta.setOrden(orden);
        consulta.setFecha(ahora);
        consulta.setMotivo(datos.motivo().trim());
        consulta.setObservaciones(datos.observaciones().trim());
        consulta.setIndicaciones(datos.indicaciones() == null || datos.indicaciones().isBlank() ? null : datos.indicaciones().trim());
        consulta.setFechaRegistro(ahora);
        consultas.saveAndFlush(consulta);

        reserva.setReservaEstado(EstadoReserva.ATENDIDA);
        orden.setOrdenEstado(EstadoOrden.UTILIZADA);
        return convertirAConsultaResponseDTO(consulta, turno.getEspecialidad().getNombre());
    }

    private HistoriaClinica abrirHistoriaClinica(Paciente paciente, LocalDateTime ahora) {
        HistoriaClinica historia = new HistoriaClinica();
        historia.setPaciente(paciente);
        historia.setFechaApertura(ahora);
        return historias.save(historia);
    }

    private static ResponseStatusException ordenNoValida() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La atención no cuenta con una orden válida para continuar.");
    }

    private ConsultaResponseDTO convertirAConsultaResponseDTO(Consulta c, String especialidad) {
        return new ConsultaResponseDTO(c.getIdConsulta(), c.getOrden().getNumeroOrden(), c.getReserva().getIdReserva(),
            c.getPaciente().getIdUsuario(), c.getPaciente().nombreCompleto(), c.getMedico().getIdUsuario(),
            c.getMedico().nombreCompleto(), especialidad, c.getFecha(), c.getMotivo(), c.getObservaciones(),
            c.getIndicaciones(), c.getFechaRegistro());
    }

    /** CU-11. Devuelve solo atenciones finalizadas de la historia clínica del paciente autenticado. */
    @Transactional(readOnly = true)
    public List<ConsultaResponseDTO> consultarHistoriaClinicaPropia(Long idPaciente, CriteriosBusquedaDTO criterios) {
        if (criterios.fechaDesde() != null && criterios.fechaHasta() != null
                && criterios.fechaDesde().isAfter(criterios.fechaHasta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El período indicado no es válido.");
        }
        if (criterios.ordenarPor() != null && !criterios.ordenarPor().isBlank()
                && !criterios.ordenarPor().trim().equalsIgnoreCase("fecha")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ordenamiento debe ser por fecha.");
        }

        LocalDateTime desde = criterios.fechaDesde() == null ? null : criterios.fechaDesde().atStartOfDay();
        LocalDateTime hasta = criterios.fechaHasta() == null ? null : criterios.fechaHasta().atTime(23, 59, 59, 999_999_999);
        String codigoEspecialidad = criterios.codigoEspecialidad() == null || criterios.codigoEspecialidad().isBlank()
            ? null : criterios.codigoEspecialidad().trim();
        Sort.Direction direccion = Boolean.FALSE.equals(criterios.descendente()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Specification<Consulta> filtros = (consulta, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            var reserva = consulta.join("reserva");
            var turno = reserva.join("ocurrenciaTurno").join("turnoSemanal");
            condiciones.add(cb.equal(consulta.get("paciente").get("idUsuario"), idPaciente));
            condiciones.add(cb.equal(reserva.get("reservaEstado"), EstadoReserva.ATENDIDA));
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(consulta.get("fecha"), desde));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThanOrEqualTo(consulta.get("fecha"), hasta));
            }
            if (codigoEspecialidad != null) {
                condiciones.add(cb.equal(turno.get("especialidad").get("codigo"), codigoEspecialidad));
            }
            if (criterios.idMedico() != null) {
                condiciones.add(cb.equal(consulta.get("medico").get("idUsuario"), criterios.idMedico()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };

        return consultas.findAll(filtros, Sort.by(direccion, "fecha").and(Sort.by(Sort.Direction.DESC, "idConsulta")))
            .stream()
            .map(c -> convertirAConsultaResponseDTO(c, c.getReserva().getOcurrenciaTurno()
                .getTurnoSemanal().getEspecialidad().getNombre()))
            .toList();
    }

    /** CU-11. Un identificador ajeno se trata igual que uno inexistente. */
    @Transactional(readOnly = true)
    public ConsultaResponseDTO consultarDetalleHistoriaClinicaPropia(Long idPaciente, Long idConsulta) {
        Consulta consulta = consultas.buscarAtencionFinalizadaDePaciente(idPaciente, idConsulta, EstadoReserva.ATENDIDA)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No se encontró la atención solicitada."));
        return convertirAConsultaResponseDTO(consulta, consulta.getReserva().getOcurrenciaTurno()
            .getTurnoSemanal().getEspecialidad().getNombre());
    }

    /** CU-22. Solo lectura: requiere una reserva u orden válida del paciente con el médico para el día de atención. */
    @Transactional(readOnly = true)
    public List<ConsultaResponseDTO> consultarHistoriaClinicaDuranteAtencion(Long idMedico, Long idPaciente) {
        validarContextoDeAtencion(idMedico, idPaciente);
        return consultas.buscarAntecedentesDePaciente(idPaciente, LocalDateTime.now(reloj)).stream()
            .map(c -> convertirAConsultaResponseDTO(c, c.getReserva().getOcurrenciaTurno()
                .getTurnoSemanal().getEspecialidad().getNombre()))
            .toList();
    }

    /** CU-22. Revalida el acceso al abrir cada detalle; no admite consultas de otro paciente. */
    @Transactional(readOnly = true)
    public ConsultaResponseDTO consultarDetalleDuranteAtencion(Long idMedico, Long idPaciente, Long idConsulta) {
        validarContextoDeAtencion(idMedico, idPaciente);
        Consulta consulta = consultas.buscarAtencionFinalizadaDePaciente(idPaciente, idConsulta, EstadoReserva.ATENDIDA)
            .filter(c -> !c.getFecha().isAfter(LocalDateTime.now(reloj)))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la atención solicitada."));
        return convertirAConsultaResponseDTO(consulta, consulta.getReserva().getOcurrenciaTurno()
            .getTurnoSemanal().getEspecialidad().getNombre());
    }

    private void validarContextoDeAtencion(Long idMedico, Long idPaciente) {
        if (idPaciente == null || idPaciente < 1
                || !reservas.existeAtencionAutorizada(idMedico, idPaciente, LocalDate.now(reloj))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No existe una atención autorizada para consultar la historia clínica de este paciente. "
                + "Seleccione una reserva vigente u orden emitida válida de un turno propio del día de hoy.");
        }
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
                paciente.getIdUsuario(), paciente.nombreCompleto(), r.getReservaEstado(),
                orden == null ? null : orden.getNumeroOrden(), orden == null ? null : orden.getOrdenEstado());
        }).toList();
    }

    /** CU-26. Devuelve únicamente las atenciones registradas por el médico autenticado. */
    @Transactional(readOnly = true)
    public List<ConsultaResponseDTO> consultarHistorialAtencionesPropias(Long idMedico, CriteriosBusquedaDTO criterios) {
        if (criterios.fechaDesde() != null && criterios.fechaHasta() != null
                && criterios.fechaDesde().isAfter(criterios.fechaHasta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El período indicado no es válido.");
        }
        if (criterios.ordenarPor() != null && !criterios.ordenarPor().isBlank()
                && !criterios.ordenarPor().trim().equalsIgnoreCase("fecha")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ordenamiento debe ser por fecha.");
        }

        LocalDateTime desde = criterios.fechaDesde() == null ? null : criterios.fechaDesde().atStartOfDay();
        LocalDateTime hasta = criterios.fechaHasta() == null ? null
            : criterios.fechaHasta().atTime(23, 59, 59, 999_999_999);
        String codigoEspecialidad = criterios.codigoEspecialidad() == null || criterios.codigoEspecialidad().isBlank()
            ? null : criterios.codigoEspecialidad().trim();
        Sort.Direction direccion = Boolean.FALSE.equals(criterios.descendente()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Specification<Consulta> filtros = (consulta, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(consulta.get("medico").get("idUsuario"), idMedico));
            if (desde != null) condiciones.add(cb.greaterThanOrEqualTo(consulta.get("fecha"), desde));
            if (hasta != null) condiciones.add(cb.lessThanOrEqualTo(consulta.get("fecha"), hasta));
            if (codigoEspecialidad != null) {
                var turno = consulta.join("reserva").join("ocurrenciaTurno").join("turnoSemanal");
                condiciones.add(cb.equal(turno.get("especialidad").get("codigo"), codigoEspecialidad));
            }
            if (criterios.texto() != null && !criterios.texto().isBlank()) {
                condiciones.add(com.mediturno.backend.common.busqueda.FiltrosBusqueda.coincidenciasDePersona(
                    consulta.join("paciente"), cb, criterios.texto()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };

        return consultas.findAll(filtros, Sort.by(direccion, "fecha").and(Sort.by(Sort.Direction.DESC, "idConsulta")))
            .stream()
            .map(c -> convertirAConsultaResponseDTO(c, c.getReserva().getOcurrenciaTurno()
                .getTurnoSemanal().getEspecialidad().getNombre()))
            .toList();
    }
    @Transactional(readOnly = true)
    public ConsultaResponseDTO consultarDetalleAtencionPropia(Long idMedico, Long idConsulta) {
        Consulta consulta = consultas.findByIdConsultaAndMedicoIdUsuario(idConsulta, idMedico)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La atención no existe"));
        return convertirAConsultaResponseDTO(consulta,
            consulta.getReserva().getOcurrenciaTurno().getTurnoSemanal().getEspecialidad().getNombre());
    }

}
