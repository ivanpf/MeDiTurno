package com.mediturno.backend.notificaciones;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.consultas.repository.ConsultaRepository;
import com.mediturno.backend.notificaciones.dto.NotificacionResponseDTO;
import com.mediturno.backend.notificaciones.model.CanalNotificacion;
import com.mediturno.backend.notificaciones.model.EstadoNotificacion;
import com.mediturno.backend.notificaciones.model.Notificacion;
import com.mediturno.backend.notificaciones.model.TipoNotificacion;
import com.mediturno.backend.notificaciones.repository.NotificacionRepository;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.SuspensionTurno;
import com.mediturno.backend.turnos.DisponibilidadService;
import com.mediturno.backend.turnos.repository.SuspensionTurnoRepository;

/**
 * Lógica de negocio de: CU-6, CU-8, CU-13, CU-23.
 * Implementados: avisos de reserva confirmada (CU-8), suspensión (CU-13) y consulta registrada (CU-23), en la aplicación,
 * por correo cuando corresponde y por push.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacionRepository notificacionRepository;
    private final SuspensionTurnoRepository suspensionTurnoRepository;
    private final ReservaRepository reservaRepository;
    private final ConsultaRepository consultaRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final DisponibilidadService disponibilidadService;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public NotificacionService(NotificacionRepository notificacionRepository, SuspensionTurnoRepository suspensionTurnoRepository,
            ReservaRepository reservaRepository, ConsultaRepository consultaRepository, EmailService emailService,
            PushNotificationService pushNotificationService, DisponibilidadService disponibilidadService,
            PlatformTransactionManager administradorDeTransacciones, Clock reloj) {
        this.notificacionRepository = notificacionRepository;
        this.suspensionTurnoRepository = suspensionTurnoRepository;
        this.reservaRepository = reservaRepository;
        this.consultaRepository = consultaRepository;
        this.emailService = emailService;
        this.pushNotificationService = pushNotificationService;
        this.disponibilidadService = disponibilidadService;
        this.transaccion = new TransactionTemplate(administradorDeTransacciones);
        this.reloj = reloj;
    }

    /** CU-8. Uso interno. */
    public void enviarRecordatorioConsulta(Long idReserva) {
        throw new NoImplementadoException();
    }

    /** CU-8. Guarda el aviso de aplicación y, después del commit, envía el push personal del paciente. */
    public void notificarReservaConfirmada(Long idReserva) {
        AvisoRegistrado aviso = transaccion.execute(estado -> {
            Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + idReserva));
            LocalDateTime ahora = LocalDateTime.now(reloj);
            OcurrenciaTurno ocurrencia = reserva.getOcurrenciaTurno();
            String hora = disponibilidadService.horaEstimada(ocurrencia, reserva.getNumero()).format(HORA);
            String mensaje = "Se confirmó su reserva número " + reserva.getNumero() + " para el "
                + ocurrencia.getFecha().format(FECHA) + " a las " + hora + ". Identificador de reserva: " + reserva.getIdReserva() + ".";

            Notificacion enLaAplicacion = nueva(reserva, CanalNotificacion.APLICACION, mensaje, ahora);
            enLaAplicacion.setTipo(TipoNotificacion.RESERVA_CONFIRMADA);
            enLaAplicacion.setEstado(EstadoNotificacion.ENVIADA);
            enLaAplicacion.setFechaEnvio(ahora);
            notificacionRepository.save(enLaAplicacion);
            return new AvisoRegistrado(null, reserva.getPaciente().getIdUsuario(), mensaje);
        });
        if (aviso != null) {
            pushNotificationService.enviarPush(aviso.idUsuario(), "Reserva confirmada", aviso.mensaje());
        }
    }

    /**
     * CU-13. Uso interno. Recibe las reservas alcanzadas por la suspensión (las anuladas y las que tienen orden emitida, que no
     * se anulan). Avisa a cada paciente en la aplicación, por correo y por push. Los envíos externos ocurren después del commit.
     */
    public void notificarSuspensionTurno(Long idSuspensionTurno, List<Long> idsReservas) {
        List<AvisoRegistrado> avisos = transaccion.execute(estado -> registrarNotificaciones(idSuspensionTurno, idsReservas));
        if (avisos == null) {
            return;
        }
        for (AvisoRegistrado aviso : avisos) {
            enviarCorreo(aviso.idNotificacionCorreo());
            pushNotificationService.enviarPush(aviso.idUsuario(), "Turno suspendido", aviso.mensaje());
        }
    }

    /** Guarda las notificaciones y devuelve los datos necesarios para los envíos externos posteriores al commit. */
    private List<AvisoRegistrado> registrarNotificaciones(Long idSuspensionTurno, List<Long> idsReservas) {
        SuspensionTurno suspension = suspensionTurnoRepository.findById(idSuspensionTurno)
            .orElseThrow(() -> new IllegalArgumentException("No existe la suspensión " + idSuspensionTurno));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<AvisoRegistrado> avisos = new ArrayList<>();
        for (Reserva reserva : reservaRepository.findAllById(idsReservas)) {
            String mensaje = armarMensaje(reserva, suspension);

            Notificacion enLaAplicacion = nueva(reserva, CanalNotificacion.APLICACION, mensaje, ahora);
            enLaAplicacion.setEstado(EstadoNotificacion.ENVIADA);
            enLaAplicacion.setFechaEnvio(ahora);
            notificacionRepository.save(enLaAplicacion);

            Long idCorreo = notificacionRepository.save(nueva(reserva, CanalNotificacion.EMAIL, mensaje, ahora)).getIdNotificacion();
            avisos.add(new AvisoRegistrado(idCorreo, reserva.getPaciente().getIdUsuario(), mensaje));
        }
        return avisos;
    }

    private record AvisoRegistrado(Long idNotificacionCorreo, Long idUsuario, String mensaje) {
    }

    private static Notificacion nueva(Reserva reserva, CanalNotificacion canal, String mensaje, LocalDateTime ahora) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(reserva.getPaciente());
        notificacion.setReserva(reserva);
        notificacion.setTipo(TipoNotificacion.SUSPENSION_TURNO);
        notificacion.setCanal(canal);
        notificacion.setMensaje(mensaje);
        notificacion.setFecha(ahora);
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        return notificacion;
    }

    private static String armarMensaje(Reserva reserva, SuspensionTurno suspension) {
        OcurrenciaTurno ocurrencia = reserva.getOcurrenciaTurno();
        var turno = ocurrencia.getTurnoSemanal();
        var medico = turno.getMedico();
        String texto = "Su reserva de " + turno.getEspecialidad().getNombre() + " con " + medico.getNombre() + " " + medico.getApellido()
            + " para el " + ocurrencia.getFecha().format(FECHA) + " (turno de " + ocurrencia.getHoraInicio().format(HORA) + " a "
            + ocurrencia.getHoraFin().format(HORA) + ", número " + reserva.getNumero() + ") " + (reserva.getReservaEstado() == EstadoReserva.ANULADA
            ? "fue anulada porque el turno fue suspendido."
            : "no se atenderá porque el turno fue suspendido (la orden emitida no podrá usarse).");
        if (suspension.getMotivo() != null) {
            texto += " Motivo: " + suspension.getMotivo() + ".";
        }
        return texto + " Puede reservar otro turno desde MeDiTurno.";
    }

    /** Manda un correo pendiente y deja registrado cómo salió. Nunca lanza una excepción. */
    private void enviarCorreo(Long idNotificacion) {
        try {
            CorreoPendiente correo = transaccion.execute(estado -> prepararCorreo(idNotificacion));
            if (correo.tipo() == TipoNotificacion.CONSULTA_REGISTRADA) {
                emailService.enviarAvisoHistoriaClinica(correo.destino());
            } else {
                emailService.enviarAvisoSuspensionTurno(correo.destino(), correo.datos());
            }
            marcar(idNotificacion, EstadoNotificacion.ENVIADA);
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el correo de la notificación {}: {}", idNotificacion, e.getMessage());
            try {
                marcar(idNotificacion, EstadoNotificacion.FALLIDA);
            } catch (RuntimeException ex) {
                log.error("No se pudo registrar que falló la notificación {}: {}", idNotificacion, ex.getMessage());
            }
        }
    }

    private record CorreoPendiente(TipoNotificacion tipo, String destino, Map<String, Object> datos) {
    }

    private CorreoPendiente prepararCorreo(Long idNotificacion) {
        Notificacion notificacion = notificacionRepository.findById(idNotificacion).orElseThrow();
        var paciente = notificacion.getUsuario();
        return new CorreoPendiente(notificacion.getTipo(), paciente.getCorreo(),
            Map.of("paciente", paciente.getNombre() + " " + paciente.getApellido(), "mensaje", notificacion.getMensaje()));
    }

    private void marcar(Long idNotificacion, EstadoNotificacion estado) {
        transaccion.executeWithoutResult(t -> {
            Notificacion notificacion = notificacionRepository.findById(idNotificacion).orElseThrow();
            notificacion.setEstado(estado);
            if (estado == EstadoNotificacion.ENVIADA) {
                notificacion.setFechaEnvio(LocalDateTime.now(reloj));
            }
        });
    }

    /**
     * CU-23. Uso interno. Avisa al paciente que se incorporó nueva información a su historia clínica, sin exponer contenido
     * clínico: una notificación en la aplicación, un correo y un push. Los envíos externos ocurren después del commit.
     */
    public void notificarConsultaRegistrada(Long idConsulta) {
        AvisoRegistrado aviso = transaccion.execute(estado -> {
            Consulta consulta = consultaRepository.findById(idConsulta)
                .orElseThrow(() -> new IllegalArgumentException("No existe la consulta " + idConsulta));
            LocalDateTime ahora = LocalDateTime.now(reloj);
            String mensaje = "Se incorporó nueva información a su historia clínica a partir de su consulta del "
                + consulta.getFecha().format(FECHA) + ". Puede consultarla desde MeDiTurno.";

            Notificacion enLaAplicacion = nuevaDeConsulta(consulta, CanalNotificacion.APLICACION, mensaje, ahora);
            enLaAplicacion.setEstado(EstadoNotificacion.ENVIADA);
            enLaAplicacion.setFechaEnvio(ahora);
            notificacionRepository.save(enLaAplicacion);

            Long idCorreo = notificacionRepository.save(nuevaDeConsulta(consulta, CanalNotificacion.EMAIL, mensaje, ahora))
                .getIdNotificacion();
            return new AvisoRegistrado(idCorreo, consulta.getPaciente().getIdUsuario(), mensaje);
        });
        if (aviso != null) {
            enviarCorreo(aviso.idNotificacionCorreo());
            pushNotificationService.enviarPush(aviso.idUsuario(), "Nueva información en su historia clínica", aviso.mensaje());
        }
    }

    private static Notificacion nuevaDeConsulta(Consulta consulta, CanalNotificacion canal, String mensaje, LocalDateTime ahora) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(consulta.getPaciente());
        notificacion.setReserva(consulta.getReserva());
        notificacion.setConsulta(consulta);
        notificacion.setOrden(consulta.getOrden());
        notificacion.setTipo(TipoNotificacion.CONSULTA_REGISTRADA);
        notificacion.setCanal(canal);
        notificacion.setMensaje(mensaje);
        notificacion.setFecha(ahora);
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        return notificacion;
    }

    /** CU-6. Las notificaciones de la aplicación de la persona (las últimas 50), la más nueva primero. */
    @Transactional(readOnly = true)
    public List<NotificacionResponseDTO> listarNotificaciones(Long idUsuario) {
        return notificacionRepository
            .findTop50ByUsuarioIdUsuarioAndCanalOrderByFechaDescIdNotificacionDesc(idUsuario, CanalNotificacion.APLICACION)
            .stream().map(this::convertirANotificacionResponseDTO).toList();
    }

    /** CU-6. Marca como leída una notificación de la persona. Si no es suya responde 404, igual que si no existiera. */
    @Transactional
    public void marcarLeida(Long idNotificacion, Long idUsuario) {
        Notificacion notificacion = notificacionRepository.findById(idNotificacion)
            .filter(n -> n.getUsuario().getIdUsuario().equals(idUsuario) && n.getCanal() == CanalNotificacion.APLICACION)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la notificación"));
        notificacion.setEstado(EstadoNotificacion.LEIDA);
    }

    private NotificacionResponseDTO convertirANotificacionResponseDTO(Notificacion n) {
        return new NotificacionResponseDTO(n.getIdNotificacion(), n.getTipo(), n.getMensaje(), n.getFecha(),
            n.getEstado() == EstadoNotificacion.LEIDA, n.getReserva() == null ? null : n.getReserva().getIdReserva());
    }
}
