package com.mediturno.backend.common.seguridad;

import org.springframework.stereotype.Component;

import com.mediturno.backend.common.exception.NoImplementadoException;

/**
 * Identidad del usuario autenticado en la solicitud actual (se obtiene del token).
 * Los controladores usan esta clase para saber quién hace la llamada, en lugar de recibir el id por la URL.
 * TODO(CU-1): implementar cuando exista la autenticación.
 */
@Component
public class UsuarioActual {

    public Long obtenerIdSesionActual() {
        throw new NoImplementadoException();
    }

    public Long obtenerIdUsuarioAutenticado() {
        throw new NoImplementadoException();
    }

    public Long obtenerIdPacienteAutenticado() {
        throw new NoImplementadoException();
    }

    public Long obtenerIdMedicoAutenticado() {
        throw new NoImplementadoException();
    }

    public Long obtenerIdFuncionarioAutenticado() {
        throw new NoImplementadoException();
    }
}
