package com.mediturno.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.auth.dto.LoginResponseDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.FiltroAutenticacion;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

/** Pruebas de CU-1 y CU-2 sin Firebase ni base de datos: el proveedor de identidad y el repositorio son dobles. */
class AutenticacionTest {

    private ProveedorIdentidad proveedor;
    private UsuarioRepository repositorio;
    private FiltroAutenticacion filtro;
    private FilterChain cadena;

    @BeforeEach
    void preparar() {
        proveedor = mock(ProveedorIdentidad.class);
        repositorio = mock(UsuarioRepository.class);
        filtro = new FiltroAutenticacion(proveedor, repositorio);
        cadena = mock(FilterChain.class);
    }

    private Usuario usuario(Usuario u, EstadoActivacion estado) {
        u.setIdUsuario(7L);
        u.setNombre("Ana");
        u.setApellido("López");
        u.setEstado(estado);
        u.setFirebaseUid("uid-1");
        return u;
    }

    private MockHttpServletResponse llamar(String ruta, String cabecera) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", ruta);
        peticion.setRequestURI(ruta);
        if (cabecera != null) {
            peticion.addHeader("Authorization", cabecera);
        }
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, cadena);
        return respuesta;
    }

    @Test
    void tokenValidoYCuentaActivaPasa() throws Exception {
        when(proveedor.verificarToken("tok")).thenReturn(Optional.of("uid-1"));
        when(repositorio.findByFirebaseUid("uid-1")).thenReturn(Optional.of(usuario(new Paciente(), EstadoActivacion.ACTIVO)));

        MockHttpServletResponse respuesta = llamar("/api/reservas/mias", "Bearer tok");

        assertEquals(200, respuesta.getStatus());
        verify(cadena).doFilter(any(), any());
    }

    @Test
    void sinTokenTokenInvalidoCuentaInexistenteOInactivaDanElMismoError401() throws Exception {
        when(proveedor.verificarToken("malo")).thenReturn(Optional.empty());
        when(proveedor.verificarToken("sin-cuenta")).thenReturn(Optional.of("uid-x"));
        when(repositorio.findByFirebaseUid("uid-x")).thenReturn(Optional.empty());
        when(proveedor.verificarToken("inactivo")).thenReturn(Optional.of("uid-1"));
        when(repositorio.findByFirebaseUid("uid-1")).thenReturn(Optional.of(usuario(new Paciente(), EstadoActivacion.INACTIVO)));

        String esperado = llamar("/api/auth/login", null).getContentAsString();
        assertEquals(401, llamar("/api/auth/login", null).getStatus());
        assertEquals(esperado.replaceAll("\"fecha\":\"[^\"]*\"", ""), llamar("/api/auth/login", "Bearer malo").getContentAsString().replaceAll("\"fecha\":\"[^\"]*\"", ""));
        assertEquals(401, llamar("/api/auth/login", "Bearer sin-cuenta").getStatus());
        assertEquals(401, llamar("/api/auth/login", "Bearer inactivo").getStatus());
    }

    @Test
    void healthEsPublico() throws Exception {
        assertEquals(200, llamar("/api/health", null).getStatus());
        verify(cadena).doFilter(any(), any());
    }

    @Test
    void registroDePacientesYRecuperacionDeAccesoSonPublicos() throws Exception {
        assertEquals(200, llamar("/api/pacientes/registro", null).getStatus());
        assertEquals(200, llamar("/api/auth/recuperacion/abc/contrasena", null).getStatus());
    }

    @Test
    void loginDevuelveElRolSegunLaTabla() {
        when(repositorio.findById(7L)).thenReturn(Optional.of(usuario(new Administrador(), EstadoActivacion.ACTIVO)));

        LoginResponseDTO respuesta = new AuthService(repositorio, proveedor).login(7L);

        assertEquals("admin", respuesta.rol());
        assertEquals("Ana López", respuesta.nombre());
    }

    @Test
    void cerrarSesionRevocaLasSesionesEnFirebase() {
        when(repositorio.findById(7L)).thenReturn(Optional.of(usuario(new Paciente(), EstadoActivacion.ACTIVO)));

        new AuthService(repositorio, proveedor).cerrarSesion(7L);

        verify(proveedor).revocarSesiones("uid-1");
    }
}
