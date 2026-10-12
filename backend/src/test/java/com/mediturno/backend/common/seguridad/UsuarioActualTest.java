package com.mediturno.backend.common.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Usuario;

/** Cada método por rol devuelve el id si la persona tiene ese rol y responde 403 si no. */
class UsuarioActualTest {

    private final UsuarioActual usuarioActual = new UsuarioActual();

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void autenticarComo(Usuario usuario) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (usuario != null) {
            usuario.setIdUsuario(5L);
            peticion.setAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO, usuario);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));
    }

    @Test
    void elAdministradorPuedeUsarLosMetodosDeAdministrador() {
        autenticarComo(new Administrador());

        assertEquals(5L, usuarioActual.obtenerIdAdministradorAutenticado());
        assertEquals(5L, usuarioActual.obtenerIdUsuarioAutenticado());
    }

    @Test
    void unPacienteNoPuedeUsarLosMetodosDeAdministradorNiDeMedico() {
        autenticarComo(new Paciente());

        assertEquals(5L, usuarioActual.obtenerIdPacienteAutenticado());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            usuarioActual::obtenerIdAdministradorAutenticado).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            usuarioActual::obtenerIdMedicoAutenticado).getStatusCode());
    }

    @Test
    void sinUsuarioEnLaSolicitudResponde401() {
        autenticarComo(null);

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
            usuarioActual::obtenerIdUsuarioAutenticado).getStatusCode());
    }
}
