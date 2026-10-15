package com.mediturno.backend.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.notificaciones.dto.NotificacionResponseDTO;
import com.mediturno.backend.notificaciones.model.CanalNotificacion;
import com.mediturno.backend.notificaciones.model.EstadoNotificacion;
import com.mediturno.backend.notificaciones.model.Notificacion;
import com.mediturno.backend.notificaciones.model.TipoNotificacion;
import com.mediturno.backend.notificaciones.repository.NotificacionRepository;
import com.mediturno.backend.usuarios.model.Usuario;

/** CU-6 (parte de notificaciones): lo que muestra la campanita. */
class CampanitaIntegracionTest extends PostgresIntegracionTest {

    @Autowired NotificacionService servicio;
    @Autowired NotificacionRepository notificaciones;

    private Notificacion guardar(Usuario usuario, CanalNotificacion canal, String mensaje, LocalDateTime fecha) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTipo(TipoNotificacion.SUSPENSION_TURNO);
        n.setCanal(canal);
        n.setMensaje(mensaje);
        n.setFecha(fecha);
        n.setEstado(EstadoNotificacion.ENVIADA);
        return notificaciones.saveAndFlush(n);
    }

    @Test
    void listaSoloLasDeLaAplicacionDeLaPersonaYLaMasNuevaPrimero() {
        Usuario ana = medico("9701", EstadoActivacion.ACTIVO);
        Usuario beto = medico("9702", EstadoActivacion.ACTIVO);
        LocalDateTime ahora = LocalDateTime.now();
        guardar(ana, CanalNotificacion.APLICACION, "vieja", ahora.minusDays(1));
        guardar(ana, CanalNotificacion.APLICACION, "nueva", ahora);
        guardar(ana, CanalNotificacion.EMAIL, "correo", ahora);
        guardar(beto, CanalNotificacion.APLICACION, "ajena", ahora);

        List<NotificacionResponseDTO> lista = servicio.listarNotificaciones(ana.getIdUsuario());

        assertEquals(List.of("nueva", "vieja"), lista.stream().map(NotificacionResponseDTO::mensaje).toList());
        assertFalse(lista.get(0).leida());
    }

    @Test
    void marcarLeidaLaDejaLeida() {
        Usuario ana = medico("9703", EstadoActivacion.ACTIVO);
        Notificacion n = guardar(ana, CanalNotificacion.APLICACION, "hola", LocalDateTime.now());

        servicio.marcarLeida(n.getIdNotificacion(), ana.getIdUsuario());

        assertTrue(servicio.listarNotificaciones(ana.getIdUsuario()).get(0).leida());
    }

    @Test
    void noSePuedeMarcarLaDeOtraPersonaNiUnaInexistente() {
        Usuario ana = medico("9704", EstadoActivacion.ACTIVO);
        Usuario beto = medico("9705", EstadoActivacion.ACTIVO);
        Notificacion n = guardar(ana, CanalNotificacion.APLICACION, "privada", LocalDateTime.now());

        ResponseStatusException ajena = assertThrows(ResponseStatusException.class,
            () -> servicio.marcarLeida(n.getIdNotificacion(), beto.getIdUsuario()));
        ResponseStatusException inexistente = assertThrows(ResponseStatusException.class,
            () -> servicio.marcarLeida(-1L, ana.getIdUsuario()));

        assertEquals(HttpStatus.NOT_FOUND, ajena.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, inexistente.getStatusCode());
        assertFalse(servicio.listarNotificaciones(ana.getIdUsuario()).get(0).leida());
    }
}
