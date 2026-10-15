package com.mediturno.backend.notificaciones;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

    /** CU-34. */
    public String enviarCredencialesTemporales(String destino, String contrasenaTemporal) {
        throw new NoImplementadoException();
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
