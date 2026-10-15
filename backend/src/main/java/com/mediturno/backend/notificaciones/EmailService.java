package com.mediturno.backend.notificaciones;

import java.util.Map;
import java.util.UUID;

import com.mediturno.backend.common.exception.NoImplementadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-4, CU-8, CU-13, CU-16, CU-23, CU-34.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailService(JavaMailSender mailSender,
            @Value("${mediturno.email.remitente:no-reply@mediturno.local}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    /** CU-8. */
    public String enviarRecordatorioConsulta(String destino, Map<String, Object> datos) {
        throw new NoImplementadoException();
    }

    /** CU-23. */
    public String enviarAvisoHistoriaClinica(String destino) {
        throw new NoImplementadoException();
    }

    /** CU-13. */
    public String enviarAvisoSuspensionTurno(String destino, Map<String, Object> datos) {
        throw new NoImplementadoException();
    }

    /** CU-34. Envía la contraseña inicial generada para la cuenta interna. */
    public String enviarCredencialesTemporales(String destino, String contrasenaTemporal) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject("Credenciales de acceso a MeDiTurno");
        mensaje.setText("Se creó su cuenta de MeDiTurno.\n\n"
                + "Correo: " + destino + "\n"
                + "Contraseña temporal: " + contrasenaTemporal + "\n\n"
                + "Guarde estas credenciales en un lugar seguro.");
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.error("No se pudieron enviar las credenciales temporales a {}", destino, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo enviar el correo con las credenciales temporales. Intente nuevamente.");
        }
        return UUID.randomUUID().toString();
    }

    /** CU-4. */
    public String enviarRecuperacionAcceso(String destino, String token) {
        throw new NoImplementadoException();
    }

    /** CU-16. */
    public String enviarOrdenPDF(String pdfBase64, String destino) {
        throw new NoImplementadoException();
    }
}
