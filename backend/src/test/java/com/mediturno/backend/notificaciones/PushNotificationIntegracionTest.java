package com.mediturno.backend.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.notificaciones.repository.TokenDispositivoRepository;
import com.mediturno.backend.usuarios.model.Medico;
import tools.jackson.databind.ObjectMapper;

/** Registro por usuario, petición Expo y limpieza de dispositivos desinstalados. */
class PushNotificationIntegracionTest extends PostgresIntegracionTest {

    private static final HttpServer EXPO = iniciarExpoFalso();
    private static volatile String cuerpoRecibido;
    private static volatile String autorizacionRecibida;
    private static volatile String aceptacionRecibida;
    private static volatile String codificacionRecibida;
    private static volatile String tipoContenidoRecibido;

    @Autowired PushNotificationService servicio;
    @Autowired TokenDispositivoRepository tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configuracionExpo(DynamicPropertyRegistry registro) {
        registro.add("mediturno.push.url", () -> "http://localhost:" + EXPO.getAddress().getPort() + "/push/send");
        registro.add("mediturno.push.access-token", () -> "token-de-prueba");
    }

    @BeforeEach
    void limpiarCaptura() {
        cuerpoRecibido = null;
        autorizacionRecibida = null;
        aceptacionRecibida = null;
        codificacionRecibida = null;
        tipoContenidoRecibido = null;
    }

    @AfterAll
    static void cerrarExpoFalso() {
        EXPO.stop(0);
    }

    @Test
    void reasignaTokenAlUltimoUsuarioYEliminaSoloLosDelUsuario() {
        Medico primero = medico(usuarioDePrueba(), EstadoActivacion.ACTIVO);
        Medico segundo = medico(usuarioDePrueba(), EstadoActivacion.ACTIVO);
        String token = "ExponentPushToken[reassign-" + UUID.randomUUID() + "]";

        servicio.registrar(primero.getIdUsuario(), token);
        servicio.registrar(segundo.getIdUsuario(), token);

        assertEquals(segundo.getIdUsuario(), jdbc.queryForObject(
            "select id_usuario from token_dispositivo where token = ?", Long.class, token));
        servicio.eliminar(primero.getIdUsuario());
        assertEquals(1, tokens.buscarTokensPorUsuario(segundo.getIdUsuario()).size());
        servicio.eliminar(segundo.getIdUsuario());
        assertFalse(jdbc.queryForObject("select exists(select 1 from token_dispositivo where token = ?)", Boolean.class, token));
    }

    @Test
    void enviaMensajeExpoAutenticadoYLimpiaDeviceNotRegistered() throws Exception {
        Medico paciente = medico(usuarioDePrueba(), EstadoActivacion.ACTIVO);
        String token = "ExponentPushToken[invalid-" + UUID.randomUUID() + "]";
        servicio.registrar(paciente.getIdUsuario(), token);

        servicio.enviarPush(paciente.getIdUsuario(), "Aviso", "Mensaje", Map.of("reserva", 12));

        assertNotNull(cuerpoRecibido);
        var mensajes = objectMapper.readTree(cuerpoRecibido);
        assertEquals(1, mensajes.size());
        assertEquals(token, mensajes.get(0).get("to").asText());
        assertEquals("Aviso", mensajes.get(0).get("title").asText());
        assertEquals("Mensaje", mensajes.get(0).get("body").asText());
        assertEquals("default", mensajes.get(0).get("sound").asText());
        assertEquals(12, mensajes.get(0).get("data").get("reserva").asInt());
        assertEquals("Bearer token-de-prueba", autorizacionRecibida);
        assertEquals("application/json", aceptacionRecibida);
        assertEquals("gzip, deflate", codificacionRecibida);
        assertEquals("application/json", tipoContenidoRecibido);
        assertFalse(jdbc.queryForObject("select exists(select 1 from token_dispositivo where token = ?)", Boolean.class, token));
    }

    private static HttpServer iniciarExpoFalso() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/push/send", intercambio -> {
                cuerpoRecibido = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                autorizacionRecibida = intercambio.getRequestHeaders().getFirst("Authorization");
                aceptacionRecibida = intercambio.getRequestHeaders().getFirst("Accept");
                codificacionRecibida = intercambio.getRequestHeaders().getFirst("Accept-encoding");
                tipoContenidoRecibido = intercambio.getRequestHeaders().getFirst("Content-Type");
                byte[] respuesta = """
                    {"data":[{"status":"error","details":{"error":"DeviceNotRegistered"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().set("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, respuesta.length);
                intercambio.getResponseBody().write(respuesta);
                intercambio.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String usuarioDePrueba() {
        return "push-" + UUID.randomUUID().toString().substring(0, 12);
    }
}
