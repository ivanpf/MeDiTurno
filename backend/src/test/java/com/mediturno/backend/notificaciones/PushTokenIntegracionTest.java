package com.mediturno.backend.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.notificaciones.repository.TokenDispositivoRepository;
import com.mediturno.backend.usuarios.model.Usuario;

/** Registro y baja de los tokens de FCM de los dispositivos. */
class PushTokenIntegracionTest extends PostgresIntegracionTest {

    @Autowired PushNotificationService servicio;
    @Autowired TokenDispositivoRepository tokens;

    @Test
    void registraElTokenDeLaPersonaYNoLoDuplicaAlRepetirlo() {
        Usuario ana = medico("9801", EstadoActivacion.ACTIVO);

        servicio.registrar(ana.getIdUsuario(), "token-ana");
        servicio.registrar(ana.getIdUsuario(), "token-ana");

        assertEquals(1, tokens.findByUsuarioIdUsuario(ana.getIdUsuario()).size());
    }

    @Test
    void unDispositivoPerteneceALaUltimaPersonaQueInicioSesion() {
        Usuario ana = medico("9802", EstadoActivacion.ACTIVO);
        Usuario beto = medico("9803", EstadoActivacion.ACTIVO);

        servicio.registrar(ana.getIdUsuario(), "token-compartido");
        servicio.registrar(beto.getIdUsuario(), "token-compartido");

        assertTrue(tokens.findByUsuarioIdUsuario(ana.getIdUsuario()).isEmpty());
        assertEquals(1, tokens.findByUsuarioIdUsuario(beto.getIdUsuario()).size());
    }

    @Test
    void eliminarBorraSoloLosTokensDeLaPersona() {
        Usuario ana = medico("9804", EstadoActivacion.ACTIVO);
        Usuario beto = medico("9805", EstadoActivacion.ACTIVO);
        servicio.registrar(ana.getIdUsuario(), "token-ana-2");
        servicio.registrar(beto.getIdUsuario(), "token-beto-2");

        servicio.eliminar(ana.getIdUsuario());

        assertTrue(tokens.findByUsuarioIdUsuario(ana.getIdUsuario()).isEmpty());
        assertEquals(1, tokens.findByUsuarioIdUsuario(beto.getIdUsuario()).size());
    }

    @Test
    void rechazaUnTokenVacio() {
        Usuario ana = medico("9806", EstadoActivacion.ACTIVO);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> servicio.registrar(ana.getIdUsuario(), " "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void enviarPushSinTokensONoConfigurarFirebaseNoLanzaExcepcionYNoCuentaComoEnviado() {
        Usuario ana = medico("9807", EstadoActivacion.ACTIVO);

        assertFalse(servicio.tieneDispositivos(ana.getIdUsuario()));
        assertFalse(servicio.enviarPush(ana.getIdUsuario(), "Titulo", "Mensaje", Map.of()));
        servicio.registrar(ana.getIdUsuario(), "token-sin-firebase");
        assertTrue(servicio.tieneDispositivos(ana.getIdUsuario()));
        assertFalse(servicio.enviarPush(ana.getIdUsuario(), "Titulo", "Mensaje", Map.of()));
    }
}
