package com.mediturno.backend.ordenes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.documentos.DocumentosService;
import com.mediturno.backend.notificaciones.EmailService;
import com.mediturno.backend.ordenes.dto.EnvioOrdenResponseDTO;
import com.mediturno.backend.ordenes.dto.OrdenResponseDTO;
import com.mediturno.backend.ordenes.model.EstadoOrden;
import com.mediturno.backend.ordenes.model.Orden;
import com.mediturno.backend.ordenes.repository.OrdenRepository;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.DisponibilidadService;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.usuarios.repository.FuncionarioRepository;

/**
 * Lógica de negocio de: CU-16. Implementada: la emisión de la orden, su PDF y su envío por correo.
 */
@Service
public class OrdenService {

    /** La orden se emite hasta este tiempo antes de la hora estimada de atención (regla de CU-16). */
    private static final int MINUTOS_DE_ANTICIPACION = 15;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final OrdenRepository ordenRepository;
    private final ReservaRepository reservaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final DisponibilidadService disponibilidadService;
    private final DocumentosService documentosService;
    private final EmailService emailService;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public OrdenService(OrdenRepository ordenRepository, ReservaRepository reservaRepository,
            FuncionarioRepository funcionarioRepository, DisponibilidadService disponibilidadService,
            DocumentosService documentosService, EmailService emailService, PlatformTransactionManager administradorDeTransacciones,
            Clock reloj) {
        this.ordenRepository = ordenRepository;
        this.reservaRepository = reservaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.disponibilidadService = disponibilidadService;
        this.documentosService = documentosService;
        this.emailService = emailService;
        this.transaccion = new TransactionTemplate(administradorDeTransacciones);
        this.reloj = reloj;
    }

    /**
     * CU-16. Emite la orden de una reserva vigente, en una sola transacción: guarda la orden con la fecha y hora de emisión y el
     * funcionario responsable, y pasa la reserva a ORDEN_EMITIDA. La reserva se bloquea: dos emisiones simultáneas no generan dos
     * órdenes. Fuera del horario permitido (flujo 2.A) no se guarda nada y responde 409.
     */
    @Transactional
    public OrdenResponseDTO emitirOrden(Long idReserva, Long idFuncionario) {
        Reserva reserva = reservaRepository.buscarConLock(idReserva).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe"));
        revalidarReservaParaEmision(reserva);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        validarHorarioEmision(reserva, ahora);

        Orden orden = crearOrden(reserva, idFuncionario, ahora);
        ordenRepository.saveAndFlush(orden);
        reserva.setReservaEstado(EstadoReserva.ORDEN_EMITIDA);
        return new OrdenResponseDTO(orden.getNumeroOrden(), idReserva, orden.getOrdenEstado(), orden.getFechaEmision());
    }

    /** CU-16. PDF de una orden ya emitida, para imprimirlo o enviarlo por correo. 404 si la orden no existe. */
    @Transactional(readOnly = true)
    public byte[] generarOrdenPDF(Long numeroOrden) {
        return generarPDF(buscarOrden(numeroOrden));
    }

    private Orden buscarOrden(Long numeroOrden) {
        return ordenRepository.findById(numeroOrden).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La orden no existe"));
    }

    /** Hay que llamarlo dentro de una transacción: lee las relaciones de la orden. */
    private byte[] generarPDF(Orden orden) {
        Reserva reserva = orden.getReserva();
        return documentosService.generarPDF(orden, disponibilidadService.horaEstimada(reserva.getOcurrenciaTurno(), reserva.getNumero()));
    }

    /**
     * CU-16, paso 6 (opcional). Manda el PDF de la orden al correo del paciente de la reserva. Si el correo falla responde 503. El
     * envío va fuera de una transacción: no mantiene una conexión a la base abierta mientras espera al servidor de correo.
     */
    public EnvioOrdenResponseDTO enviarOrdenPorCorreo(Long numeroOrden) {
        Adjunto adjunto = transaccion.execute(estado -> {
            Orden orden = buscarOrden(numeroOrden);
            return new Adjunto(orden.getPaciente().getCorreo(), generarPDF(orden));
        });
        emailService.enviarOrdenPDF(Base64.getEncoder().encodeToString(adjunto.pdf()), adjunto.destino());
        return new EnvioOrdenResponseDTO(adjunto.destino());
    }

    private record Adjunto(String destino, byte[] pdf) {
    }

    /** Solo se emite sobre una reserva vigente: no sobre una cancelada, anulada, vencida, atendida ni con orden ya emitida. */
    private static void revalidarReservaParaEmision(Reserva reserva) {
        switch (reserva.getReservaEstado()) {
            case VIGENTE -> { }
            case ORDEN_EMITIDA -> throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya tiene una orden emitida");
            case CANCELADA -> throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva está cancelada: no se puede emitir la orden");
            case ANULADA -> throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva fue anulada: no se puede emitir la orden");
            case VENCIDA -> throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva está vencida: no se puede emitir la orden");
            case ATENDIDA -> throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya fue atendida: no se puede emitir la orden");
        }
    }

    /**
     * Paso 2: la orden se emite el día de la consulta y hasta 15 minutos antes de la hora estimada de atención del número. Antes
     * de ese día o pasado el límite, no.
     */
    private void validarHorarioEmision(Reserva reserva, LocalDateTime ahora) {
        OcurrenciaTurno ocurrencia = reserva.getOcurrenciaTurno();
        LocalDate fecha = ocurrencia.getFecha();
        if (ahora.toLocalDate().isBefore(fecha)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La orden no puede emitirse en este momento: se emite el día de la consulta (" + fecha.format(FECHA) + ")");
        }
        LocalDateTime limite = fecha.atTime(disponibilidadService.horaEstimada(ocurrencia, reserva.getNumero()))
                .minusMinutes(MINUTOS_DE_ANTICIPACION);
        if (ahora.isAfter(limite)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La orden no puede emitirse en este momento: se emite hasta " + MINUTOS_DE_ANTICIPACION
                    + " minutos antes de la hora estimada de atención (hasta las " + limite.format(HORA) + ")");
        }
    }

    private Orden crearOrden(Reserva reserva, Long idFuncionario, LocalDateTime ahora) {
        Orden orden = new Orden();
        orden.setPaciente(reserva.getPaciente());
        orden.setFuncionario(funcionarioRepository.getReferenceById(idFuncionario));
        orden.setReserva(reserva);
        orden.setFechaEmision(ahora);
        orden.setOrdenEstado(EstadoOrden.EMITIDA);
        return orden;
    }
}
