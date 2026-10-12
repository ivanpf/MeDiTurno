package com.mediturno.backend.common.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Usuario;

/** La anotación del método reemplaza a la de la clase; sin valores entra cualquier rol; sin persona (ruta pública) no se comprueba. */
class InterceptorRolesTest {

    @RolesPermitidos(Administrador.class)
    static class ControladorDePrueba {
        public void soloAdministrador() {
        }

        @RolesPermitidos({Funcionario.class, Paciente.class})
        public void funcionarioOPaciente() {
        }

        @RolesPermitidos
        public void cualquierRol() {
        }
    }

    private final InterceptorRoles interceptor = new InterceptorRoles();

    private boolean pasa(Usuario usuario, String metodo) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (usuario != null) {
            peticion.setAttribute(FiltroAutenticacion.ATRIBUTO_USUARIO, usuario);
        }
        HandlerMethod handler = new HandlerMethod(new ControladorDePrueba(), ControladorDePrueba.class.getMethod(metodo));
        return interceptor.preHandle(peticion, new MockHttpServletResponse(), handler);
    }

    @Test
    void laAnotacionDeLaClaseValeParaSusMetodos() throws Exception {
        assertTrue(pasa(new Administrador(), "soloAdministrador"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> pasa(new Paciente(), "soloAdministrador"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void laAnotacionDelMetodoReemplazaALaDeLaClase() throws Exception {
        assertTrue(pasa(new Funcionario(), "funcionarioOPaciente"));
        assertTrue(pasa(new Paciente(), "funcionarioOPaciente"));
        assertThrows(ResponseStatusException.class, () -> pasa(new Administrador(), "funcionarioOPaciente"));
    }

    @Test
    void sinValoresEntraCualquierRol() throws Exception {
        assertTrue(pasa(new Paciente(), "cualquierRol"));
        assertTrue(pasa(new Administrador(), "cualquierRol"));
    }

    @Test
    void sinPersonaEnLaSolicitudNoSeComprueba() throws Exception {
        assertTrue(pasa(null, "soloAdministrador"));
    }
}
