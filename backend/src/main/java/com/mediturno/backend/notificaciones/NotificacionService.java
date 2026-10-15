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
import com.mediturno.backend.notificaciones.dto.NotificacionResponseDTO;
import com.mediturno.backend.notificaciones.model.CanalNotificacion;
import com.mediturno.backend.notificaciones.model.EstadoNotificacion;
import com.mediturno.backend.notificaciones.model.Notificacion;
import com.mediturno.backend.notificaciones.model.TipoNotificacion;
import com.mediturno.backend.notificaciones.repository.NotificacionRepository;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.MotivoAnulacionPorSuspension;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.SuspensionTurno;
import com.mediturno.backend.turnos.repository.SuspensionTurnoRepository;

/**
 * Lógica de negocio de: CU-6, CU-8, CU-13, CU-23.
 * Implementado: el aviso de suspensión de un turno (CU-13), en la aplicación y por correo. El aviso push y los demás métodos
 * todavía lanzan NoImplementadoException.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacionRepository notificacionRepository;
    private final SuspensionTurnoRepository suspensionTurnoRepository;
    private final ReservaRepository reservaRepository;
    private final EmailService emailService;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public NotificacionService(NotificacionRepository notificacionRepository, SuspensionTurnoRepository suspensionTurnoRepository,
            ReservaRepository reservaRepository, EmailService emailService, PlatformTransactionManager administradorDeTransacciones,
            Clock reloj) {
        this.notificacionRepository = notificacionRepository;
        this.suspensionTurnoRepository = suspensionTurnoRepository;
        this.reservaRepository = reservaRepository;
        this.emailService = emailService;
        this.transaccion = new TransactionTemplate(administradorDeTransacciones);
        this.reloj = reloj;
    }

    /** CU-8. Uso interno. */
    public void enviarRecordatorioConsulta(Long idReserva) {
        throw new NoImplementadoException();
    }

    /**
     * CU-13. Uso interno. Avisa a cada paciente cuya reserva anuló la suspensión: una notificación en la aplicación y un
     * correo por reserva. El correo se manda fuera de la transacción: si falla, esa notificación queda FALLIDA y las demás
     * siguen. El aviso push lo hará otro caso de uso (necesita la aplicación móvil).
     */
    public void notificarSuspensionTurno(Long idSuspensionTurno) {
        List<Long> correosPendientes = transaccion.execute(estado -> registrarNotificaciones(idSuspensionTurno));
        if (correosPendientes == null) {
            return;
        }
        for (Long idNotificacion : correosPendientes) {
            enviarCorreo(idNotificacion);
        }
    }

    /** Guarda las notificaciones (en la aplicación ya enviadas, por correo pendientes) y devuelve los ids de los correos. */
    private List<Long> registrarNotificaciones(Long idSuspensionTurno) {
        SuspensionTurno suspension = suspensionTurnoRepository.findById(idSuspensionTurno)
            .orElseThrow(() -> new IllegalArgumentException("No existe la suspensión " + idSuspensionTurno));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Long> correos = new ArrayList<>();
        for (Reserva reserva : reservaRepository.buscarAnuladasConMotivo(MotivoAnulacionPorSuspension.de(idSuspensionTurno))) {
            String mensaje = armarMensaje(reserva, suspension);

            Notificacion enLaAplicacion = nueva(reserva, CanalNotificacion.APLICACION, mensaje, ahora);
            enLaAplicacion.setEstado(EstadoNotificacion.ENVIADA);
            enLaAplicacion.setFechaEnvio(ahora);
            notificacionRepository.save(enLaAplicacion);

            correos.add(notificacionRepository.save(nueva(reserva, CanalNotificacion.EMAIL, mensaje, ahora)).getIdNotificacion());
        }
        return correos;
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
            + ocurrencia.getHoraFin().format(HORA) + ", número " + reserva.getNumero() + ") fue anulada porque el turno fue suspendido.";
        if (suspension.getMotivo() != null) {
            texto += " Motivo: " + suspension.getMotivo() + ".";
        }
        return texto + " Puede reservar otro turno desde MeDiTurno.";
    }

    /** Manda un correo pendiente y deja registrado cómo salió. Nunca lanza una excepción. */
    private void enviarCorreo(Long idNotificacion) {
        try {
            CorreoPendiente correo = transaccion.execute(estado -> prepararCorreo(idNotificacion));
            emailService.enviarAvisoSuspensionTurno(correo.destino(), correo.datos());
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

    private record CorreoPendiente(String destino, Map<String, Object> datos) {
    }

    private CorreoPendiente prepararCorreo(Long idNotificacion) {
        Notificacion notificacion = notificacionRepository.findById(idNotificacion).orElseThrow();
        var paciente = notificacion.getUsuario();
        return new CorreoPendiente(paciente.getCorreo(),
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

    /** CU-23. Uso interno. */
    public void notificarConsultaRegistrada(Long idConsulta) {
        throw new NoImplementadoException();
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
