package com.mediturno.backend.reservas;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.ordenes.model.EstadoOrden;
import com.mediturno.backend.ordenes.model.Orden;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.ordenes.repository.OrdenRepository;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.pases.PaseService;
import com.mediturno.backend.reservas.dto.ReservaRequestDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import com.mediturno.backend.reservas.dto.ReservaCancelacionDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.DisponibilidadService;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-8, CU-9, CU-10, CU-15, CU-19.
 * CU-8, CU-9, CU-10 y CU-15 implementados. CU-19 conserva su respuesta 501.
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

    /** CU-9. La fecha y hora de referencia son las de inicio de la ocurrencia, no las del número. */
    @Transactional
    public void cancelarReserva(Long idReserva, Long idPaciente) {
        validarFormatoIdentificador(idReserva);
        Long idOcurrencia = reservaRepository.buscarOcurrenciaPropia(idReserva, idPaciente)
            .orElseThrow(ReservaService::reservaPropiaNoEncontrada);
        // Mismo orden de bloqueo que reserva y suspensión: primero la ocurrencia, después la reserva.
        ocurrenciaTurnoRepository.buscarConLock(idOcurrencia).orElseThrow(ReservaService::reservaPropiaNoEncontrada);
        Reserva reserva = reservaRepository.buscarConLock(idReserva)
            .filter(r -> r.getPaciente().getIdUsuario().equals(idPaciente))
            .orElseThrow(ReservaService::reservaPropiaNoEncontrada);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        validarCancelacion(reserva, ahora);
        reserva.setReservaEstado(EstadoReserva.CANCELADA);
        reserva.setFechaCancelacion(ahora);
        reserva.setMotivoCancelacion("Cancelada por el paciente");
        // La reserva se conserva en el historial. Las CANCELADA liberan el número en la consulta y el índice de CU-8.
    }

    /** CU-9, antes de pedir confirmación: valida la propiedad, el estado y el plazo sin modificar datos. */
    @Transactional(readOnly = true)
    public ReservaCancelacionDTO vistaPreviaCancelacion(Long idReserva, Long idPaciente) {
        validarFormatoIdentificador(idReserva);
        Reserva reserva = reservaRepository.findByIdReservaAndPacienteIdUsuario(idReserva, idPaciente)
            .orElseThrow(ReservaService::reservaPropiaNoEncontrada);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        validarCancelacion(reserva, ahora);
        return datosDeCancelacion(reserva, ahora);
    }

    /** Listado de apoyo para CU-9; el historial con búsqueda de CU-10 sigue separado. */
    @Transactional(readOnly = true)
    public List<ReservaCancelacionDTO> listarProximasPropias(Long idPaciente) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return reservaRepository.buscarProximasPropias(idPaciente, ahora.toLocalDate()).stream()
            .map(r -> datosDeCancelacion(r, ahora)).toList();
    }

    private ReservaCancelacionDTO datosDeCancelacion(Reserva reserva, LocalDateTime ahora) {
        String motivo = motivoNoCancelable(reserva, ahora);
        return new ReservaCancelacionDTO(convertirAReservaResponseDTO(reserva, null), motivo == null,
            motivo, reserva.getFechaCancelacion());
    }

    private static String motivoNoCancelable(Reserva reserva, LocalDateTime ahora) {
        if (reserva.getReservaEstado() != EstadoReserva.VIGENTE) {
            return "La reserva no puede cancelarse porque ya no está vigente.";
        }
        OcurrenciaTurno ocurrencia = reserva.getOcurrenciaTurno();
        LocalDateTime inicio = LocalDateTime.of(ocurrencia.getFecha(), ocurrencia.getHoraInicio());
        if (inicio.isBefore(ahora.plusHours(48))) {
            return "La reserva no puede cancelarse: deben faltar al menos 48 horas para el inicio del turno.";
        }
        return null;
    }

    private static void validarCancelacion(Reserva reserva, LocalDateTime ahora) {
        String motivo = motivoNoCancelable(reserva, ahora);
        if (motivo != null) throw new ResponseStatusException(HttpStatus.CONFLICT, motivo);
    }

    private static ResponseStatusException reservaPropiaNoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la reserva solicitada.");
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
    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> consultarHistorialReservas(Long idPaciente, CriteriosBusquedaDTO criterios) {
        if (criterios.fechaDesde() != null && criterios.fechaHasta() != null
                && criterios.fechaDesde().isAfter(criterios.fechaHasta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El período indicado no es válido.");
        }
        EstadoReserva estado = interpretarEstadoReserva(criterios.estado());
        Comparator<Reserva> orden = comparadorHistorial(criterios.ordenarPor(),
            !Boolean.FALSE.equals(criterios.descendente()));
        LocalDate desde = criterios.fechaDesde();
        LocalDate hasta = criterios.fechaHasta();
        String codigoEspecialidad = textoOpcional(criterios.codigoEspecialidad());

        Specification<Reserva> filtros = (reserva, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            var ocurrencia = reserva.join("ocurrenciaTurno");
            var turno = ocurrencia.join("turnoSemanal");
            condiciones.add(cb.equal(reserva.get("paciente").get("idUsuario"), idPaciente));
            if (desde != null) condiciones.add(cb.greaterThanOrEqualTo(ocurrencia.get("fecha"), desde));
            if (hasta != null) condiciones.add(cb.lessThanOrEqualTo(ocurrencia.get("fecha"), hasta));
            if (criterios.idMedico() != null) {
                condiciones.add(cb.equal(turno.get("medico").get("idUsuario"), criterios.idMedico()));
            }
            if (codigoEspecialidad != null) {
                condiciones.add(cb.equal(turno.get("especialidad").get("codigo"), codigoEspecialidad));
            }
            if (estado != null) condiciones.add(cb.equal(reserva.get("reservaEstado"), estado));
            return cb.and(condiciones.toArray(Predicate[]::new));
        };

        List<Reserva> resultado = reservaRepository.findAll(filtros);
        resultado.sort(orden);
        List<Long> idsReserva = resultado.stream().map(Reserva::getIdReserva).toList();
        Map<Long, Orden> ordenes = idsReserva.isEmpty() ? Map.of() : ordenRepository.buscarPorReservas(idsReserva)
            .stream().collect(Collectors.toMap(o -> o.getReserva().getIdReserva(), Function.identity()));
        return resultado.stream().map(reserva -> convertirAReservaHistorialResponseDTO(reserva, ordenes.get(reserva.getIdReserva())))
            .toList();
    }

    private ReservaResponseDTO convertirAReservaHistorialResponseDTO(Reserva reserva, Orden orden) {
        OcurrenciaTurno ocurrencia = reserva.getOcurrenciaTurno();
        TurnoSemanal turno = ocurrencia.getTurnoSemanal();
        Paciente paciente = reserva.getPaciente();
        return new ReservaResponseDTO(reserva.getIdReserva(), reserva.getNumero(),
            disponibilidadService.horaEstimada(ocurrencia, reserva.getNumero()), reserva.getReservaEstado(),
            paciente.getIdUsuario(), paciente.nombreCompleto(), paciente.getCedula(), turno.getMedico().nombreCompleto(),
            turno.getEspecialidad().getNombre(), ocurrencia.getFecha(), ocurrencia.getHoraInicio(), ocurrencia.getHoraFin(),
            turno.getConsultorio().getNombre(), orden == null ? null : orden.getNumeroOrden(),
            orden == null ? null : orden.getOrdenEstado());
    }

    private static EstadoReserva interpretarEstadoReserva(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return EstadoReserva.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado de reserva indicado no es válido.");
        }
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static Comparator<Reserva> comparadorHistorial(String ordenarPor, boolean descendente) {
        Comparator<Reserva> porFecha = Comparator.comparing((Reserva r) -> r.getOcurrenciaTurno().getFecha())
            .thenComparing(r -> r.getOcurrenciaTurno().getHoraInicio())
            .thenComparing(Reserva::getNumero);
        Comparator<Reserva> elegido;
        String clave = ordenarPor == null ? "" : ordenarPor.trim().toLowerCase();
        switch (clave) {
            case "", "fecha" -> elegido = porFecha;
            case "medico" -> elegido = Comparator.comparing((Reserva r) -> r.getOcurrenciaTurno().getTurnoSemanal()
                .getMedico().getApellido(), String.CASE_INSENSITIVE_ORDER).thenComparing(porFecha);
            case "especialidad" -> elegido = Comparator.comparing((Reserva r) -> r.getOcurrenciaTurno().getTurnoSemanal()
                .getEspecialidad().getNombre(), String.CASE_INSENSITIVE_ORDER).thenComparing(porFecha);
            case "estado" -> elegido = Comparator.comparing(Reserva::getReservaEstado).thenComparing(porFecha);
            case "numero" -> elegido = Comparator.comparing(Reserva::getNumero).thenComparing(porFecha);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El orden indicado no es válido (use fecha, medico, especialidad, estado o numero).");
        }
        elegido = elegido.thenComparing(Reserva::getIdReserva);
        return descendente ? elegido.reversed() : elegido;
    }
}
