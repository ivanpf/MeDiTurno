package com.mediturno.backend.notificaciones;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.notificaciones.model.EstadoNotificacion;
import com.mediturno.backend.notificaciones.model.TokenDispositivo;
import com.mediturno.backend.notificaciones.repository.NotificacionRepository;
import com.mediturno.backend.notificaciones.repository.TokenDispositivoRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

/**
 * Lógica de negocio de: CU-6, CU-8, CU-13, CU-23.
 * Los avisos push salen directo por Firebase Cloud Messaging (FCM) con la misma clave de servicio que usa la autenticación;
 * la app móvil registra el token nativo de FCM de su dispositivo. El envío es de mejor esfuerzo: nunca lanza una excepción
 * ni revierte la operación que lo originó.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    /** Canal de notificaciones que crea la app móvil (notificacionesPush.js). */
    private static final String CANAL_ANDROID = "default";

    private final TokenDispositivoRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final Clock reloj;

    public PushNotificationService(TokenDispositivoRepository tokenRepository, UsuarioRepository usuarioRepository,
            NotificacionRepository notificacionRepository, Clock reloj) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
        this.reloj = reloj;
    }

    /** CU-6. Un dispositivo pertenece a la última persona que inició sesión en él. */
    @Transactional
    public void registrar(Long idUsuario, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token del dispositivo es obligatorio");
        }
        TokenDispositivo existente = tokenRepository.findByToken(token).orElseGet(TokenDispositivo::new);
        existente.setToken(token);
        existente.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        existente.setFechaRegistro(LocalDateTime.now(reloj));
        tokenRepository.save(existente);
    }

    /** CU-6. Borra los tokens de la persona (al cerrar sesión). */
    @Transactional
    public void eliminar(Long idUsuario) {
        tokenRepository.deleteByUsuarioIdUsuario(idUsuario);
    }

    /** Uso interno. Indica si la persona tiene algún dispositivo registrado (si no, no hay aviso push que mandar ni registrar). */
    public boolean tieneDispositivos(Long idUsuario) {
        return tokenRepository.existsByUsuarioIdUsuario(idUsuario);
    }

    /**
     * Uso interno. Envía el aviso en un hilo aparte, para no demorar la operación que lo originó, y deja registrado en la
     * notificación del canal PUSH cómo salió: ENVIADA si llegó a algún dispositivo, FALLIDA si no. Nunca lanza una excepción.
     */
    @Async
    public void enviarYRegistrar(Long idNotificacion, Long idUsuario, String titulo, String mensaje) {
        boolean enviado = enviarPush(idUsuario, titulo, mensaje, Map.of());
        try {
            notificacionRepository.actualizarEstado(idNotificacion, enviado ? EstadoNotificacion.ENVIADA : EstadoNotificacion.FALLIDA,
                enviado ? LocalDateTime.now(reloj) : null);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar cómo salió el push de la notificación {}: {}", idNotificacion, e.getMessage());
        }
    }

    /**
     * Uso interno. Mejor esfuerzo: registra el resultado en el log y nunca lanza una excepción.
     * @return si el aviso llegó a Firebase para al menos un dispositivo.
     */
    boolean enviarPush(Long idUsuario, String titulo, String mensaje, Map<String, String> datos) {
        try {
            var tokens = tokenRepository.findByUsuarioIdUsuario(idUsuario).stream().map(TokenDispositivo::getToken).toList();
            if (tokens.isEmpty()) {
                return false;
            }
            if (FirebaseApp.getApps().isEmpty()) {
                log.warn("No se pudo enviar el push al usuario {}: Firebase no está configurado (falta la clave de servicio)", idUsuario);
                return false;
            }
            FirebaseMessaging firebase = FirebaseMessaging.getInstance();
            int enviados = 0;
            for (String token : tokens) {
                if (enviar(firebase, token, titulo, mensaje, datos)) {
                    enviados++;
                }
            }
            log.info("Push enviado al usuario {} ({} de {} dispositivo/s)", idUsuario, enviados, tokens.size());
            return enviados > 0;
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el push al usuario {}: {}", idUsuario, e.getMessage());
            return false;
        }
    }

    private boolean enviar(FirebaseMessaging firebase, String token, String titulo, String mensaje, Map<String, String> datos) {
        Message aviso = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(titulo).setBody(mensaje).build())
            .putAllData(datos)
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder().setChannelId(CANAL_ANDROID).build())
                .build())
            .build();
        try {
            firebase.send(aviso);
            return true;
        } catch (FirebaseMessagingException e) {
            // Dispositivo desinstalado o token que ya no sirve (por ejemplo, uno viejo de otro servicio): se descarta.
            MessagingErrorCode codigo = e.getMessagingErrorCode();
            if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
                tokenRepository.deleteByToken(token);
            }
            log.warn("FCM rechazó el push ({}): {}", codigo, e.getMessage());
            return false;
        }
    }
}
