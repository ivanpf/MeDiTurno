package com.mediturno.backend.common.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.Usuario;

/**
 * Identidad del usuario autenticado en la solicitud actual. La resuelve {@link FiltroAutenticacion} a partir del token.
 * Los controladores usan esta clase para saber quién hace la llamada, en lugar de recibir el id por la URL.
 * Los métodos por rol devuelven el id y responden 403 si la persona no tiene ese rol. Qué roles pueden usar cada
 * endpoint se declara con {@link RolesPermitidos}.
 */
@Component
public class UsuarioActual {

    public Usuario obtenerUsuarioAutenticado() {
        Object usuario = RequestContextHolder.currentRequestAttributes()
            .getAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
        if (usuario instanceof Usuario u) {
            return u;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, FiltroAutenticacion.MENSAJE_NO_AUTENTICADO);
    }

    public Long obtenerIdUsuarioAutenticado() {
        return obtenerUsuarioAutenticado().getIdUsuario();
    }

    public Long obtenerIdPacienteAutenticado() {
        return idSiEs(Paciente.class);
    }

    public Long obtenerIdMedicoAutenticado() {
        return idSiEs(Medico.class);
    }

    public Long obtenerIdFuncionarioAutenticado() {
        return idSiEs(Funcionario.class);
    }

    public Long obtenerIdAdministradorAutenticado() {
        return idSiEs(Administrador.class);
    }

    private Long idSiEs(Class<? extends Usuario> rol) {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (!rol.isInstance(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta operación");
        }
        return usuario.getIdUsuario();
    }
}
