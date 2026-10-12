package com.mediturno.backend.notificaciones;

import java.util.List;

import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.notificaciones.dto.NotificacionResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mediturno.backend.common.seguridad.RolesPermitidos;

/**
 * Endpoints de: CU-6.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioActual usuarioActual;

    public NotificacionController(NotificacionService notificacionService, UsuarioActual usuarioActual) {
        this.notificacionService = notificacionService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public List<NotificacionResponseDTO> listarNotificaciones() {
        return notificacionService.listarNotificaciones(usuarioActual.obtenerIdUsuarioAutenticado());
    }

    @PostMapping("/{idNotificacion}/lectura")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeida(@PathVariable Long idNotificacion) {
        notificacionService.marcarLeida(idNotificacion);
    }
}
