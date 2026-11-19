package com.mediturno.backend.usuarios;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.mediturno.backend.notificaciones.EmailService;

/**
 * Manda los correos con las credenciales temporales de una alta masiva (CU-35) en un hilo aparte: son hasta 100 correos
 * seguidos y la petición no debe esperarlos (el proxy corta a los 120 s). Un correo que falla no frena a los demás: se deja
 * en el log con la dirección para reenviarlo a mano.
 */
@Service
public class EnvioCredencialesService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCredencialesService.class);

    private final EmailService emailService;

    public EnvioCredencialesService(EmailService emailService) {
        this.emailService = emailService;
    }

    /** Uso interno. {@code correos} y {@code contrasenas} van en el mismo orden. Nunca lanza una excepción. */
    @Async
    public void enviar(List<String> correos, List<String> contrasenas) {
        for (int i = 0; i < correos.size(); i++) {
            try {
                emailService.enviarCredencialesTemporales(correos.get(i), contrasenas.get(i));
            } catch (RuntimeException e) {
                log.error("La cuenta de {} se creó, pero no se pudo enviar el correo con las credenciales", correos.get(i), e);
            }
        }
    }
}
