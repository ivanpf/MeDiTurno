package com.mediturno.backend.notificaciones;

import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.notificaciones.dto.RegistrarPushDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.mediturno.backend.common.seguridad.RolesPermitidos;

/**
 * Endpoints de: CU-6.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos
@RestController
@RequestMapping("/api/notificaciones/push")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;
    private final UsuarioActual usuarioActual;

    public PushNotificationController(PushNotificationService pushNotificationService, UsuarioActual usuarioActual) {
        this.pushNotificationService = pushNotificationService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrar(@Valid @RequestBody RegistrarPushDTO cuerpo) {
        pushNotificationService.registrar(usuarioActual.obtenerIdUsuarioAutenticado(), cuerpo.token());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar() {
        pushNotificationService.eliminar(usuarioActual.obtenerIdUsuarioAutenticado());
    }
}
