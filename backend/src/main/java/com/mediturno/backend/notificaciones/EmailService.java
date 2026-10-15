package com.mediturno.backend.notificaciones;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.exception.NoImplementadoException;

/**
 * Lógica de negocio de: CU-4, CU-8, CU-13, CU-16, CU-23, CU-34.
 * Implementado: el aviso de suspensión de un turno (CU-13). Los demás métodos todavía lanzan NoImplementadoException.
 * El servidor de correo (SMTP) se configura con las variables MAIL_HOST, MAIL_PORT, MAIL_USUARIO, MAIL_CLAVE y MAIL_REMITENTE
 * (ver application.yaml). En desarrollo, compose levanta Mailpit, que recibe los correos sin enviarlos a nadie
 * (se ven en http://localhost:8025).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender enviadorDeCorreo;
    private final String remitente;

    public EmailService(JavaMailSender enviadorDeCorreo, @Value("${mediturno.correo.remitente}") String remitente) {
        this.enviadorDeCorreo = enviadorDeCorreo;
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

    /**
     * CU-13. Avisa al paciente que su reserva fue anulada porque se suspendió el turno. {@code datos} lleva el nombre del
     * paciente ({@code paciente}) y el texto del aviso ({@code mensaje}). Si no se puede enviar, lanza una excepción: quien
     * lo llama decide qué hacer (el aviso queda registrado como fallido y la suspensión no se revierte). Devuelve el asunto.
     */
    public String enviarAvisoSuspensionTurno(String destino, Map<String, Object> datos) {
        String asunto = "MeDiTurno: su reserva fue anulada porque el turno fue suspendido";
        String texto = "Estimado/a " + datos.get("paciente") + ":\n\n" + datos.get("mensaje") + "\n\nMeDiTurno";
        return enviar(destino, asunto, texto);
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
            enviadorDeCorreo.send(mensaje);
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

    private String enviar(String destino, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        enviadorDeCorreo.send(mensaje);
        return asunto;
    }
}
