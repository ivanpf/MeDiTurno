package com.mediturno.backend.common.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import com.mediturno.backend.usuarios.model.Usuario;

/**
 * Comprueba la anotación {@link RolesPermitidos} de cada endpoint contra el rol de la persona que dejó
 * {@link FiltroAutenticacion}. En las rutas públicas no hay persona en la solicitud y no se comprueba nada.
 */
@Component
public class InterceptorRoles implements HandlerInterceptor {

    static final String MENSAJE_SIN_PERMISO = "No tiene permiso para realizar esta operación";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)
                || !(request.getAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO) instanceof Usuario usuario)) {
            return true;
        }
        RolesPermitidos rolesPermitidos = metodo.getMethodAnnotation(RolesPermitidos.class);
        if (rolesPermitidos == null) {
            rolesPermitidos = metodo.getBeanType().getAnnotation(RolesPermitidos.class);
        }
        if (rolesPermitidos == null || rolesPermitidos.value().length == 0) {
            return true;
        }
        for (Class<? extends Usuario> rol : rolesPermitidos.value()) {
            if (rol.isInstance(usuario)) {
                return true;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, MENSAJE_SIN_PERMISO);
    }
}
