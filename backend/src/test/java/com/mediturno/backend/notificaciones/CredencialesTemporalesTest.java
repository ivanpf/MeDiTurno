package com.mediturno.backend.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

class CredencialesTemporalesTest {
    @Test
    void utilizaElEnviadorConfigurado() {
        var enviador = mock(JavaMailSender.class);
        var servicio = new EmailService(enviador, "prueba@example.invalid");
        assertNotNull(servicio.enviarCredencialesTemporales("destino@example.invalid", "solo-prueba"));
        var mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(mensaje.capture());
        assertEquals("destino@example.invalid", mensaje.getValue().getTo()[0]);
        assertEquals("prueba@example.invalid", mensaje.getValue().getFrom());
    }

    @Test
    void falloDeCorreoConservaRespuesta503() {
        var enviador = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP no disponible")).when(enviador).send(any(SimpleMailMessage.class));
        var servicio = new EmailService(enviador, "prueba@example.invalid");
        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.enviarCredencialesTemporales("destino@example.invalid", "solo-prueba"));
        assertEquals(503, error.getStatusCode().value());
    }
}
