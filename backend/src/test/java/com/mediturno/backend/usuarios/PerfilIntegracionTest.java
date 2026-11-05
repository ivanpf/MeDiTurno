package com.mediturno.backend.usuarios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.*;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

/** CU-5: HTTP, permisos, persistencia y rollback real cuando Firebase rechaza el cambio. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PerfilIntegracionTest extends PostgresIntegracionTest {
    @Autowired PerfilService servicio;
    @Autowired PacienteRepository pacientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired WebApplicationContext contexto;
    @Autowired FiltroAutenticacion filtro;
    @MockitoBean ProveedorIdentidad proveedor;
    private Paciente paciente;
    private Funcionario funcionario;
    private MockMvc mvc;

    @BeforeEach
    void datos() {
        String unico = UUID.randomUUID().toString();
        Paciente p = new Paciente();
        p.setNombre("Ana"); p.setApellido("Perfil"); p.setCedula(unico.substring(0, 18));
        p.setCorreo(unico + "@ejemplo.com"); p.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        p.setEstado(EstadoActivacion.ACTIVO); p.setFechaCreacion(LocalDateTime.now());
        p.setFechaAlta(LocalDateTime.now()); p.setTelefono("099111222"); p.setDomicilio("Calle anterior");
        p.setFirebaseUid("uid-" + unico);
        paciente = pacientes.saveAndFlush(p);
        funcionario = funcionario("perfil-" + UUID.randomUUID().toString().substring(0, 8));
        funcionario.setFirebaseUid("uid-" + funcionario.getCedula());
        funcionario = funcionarios.saveAndFlush(funcionario);
        when(proveedor.verificarToken("paciente")).thenReturn(Optional.of(paciente.getFirebaseUid()));
        when(proveedor.verificarToken("funcionario")).thenReturn(Optional.of(funcionario.getFirebaseUid()));
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
    }

    @AfterEach
    void limpiar() {
        if (paciente != null) pacientes.deleteById(paciente.getIdUsuario());
        if (funcionario != null) funcionarios.deleteById(funcionario.getIdUsuario());
    }

    @Test
    void consultaYActualizaContactoSinModificarIdentidad() throws Exception {
        mvc.perform(get("/api/pacientes/me/perfil").header("Authorization", "Bearer paciente"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.domicilio").value("Calle anterior"));
        mvc.perform(put("/api/pacientes/me/perfil").header("Authorization", "Bearer paciente")
            .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"nuevo-perfil@ejemplo.com\",\"telefono\":\" 099333444 \",\"domicilio\":\"Nueva dirección\",\"cedula\":\"falsa\",\"rol\":\"admin\",\"idUsuario\":1}"))
            .andExpect(status().isNoContent());
        var guardado = pacientes.findById(paciente.getIdUsuario()).orElseThrow();
        assertEquals("nuevo-perfil@ejemplo.com", guardado.getCorreo());
        assertEquals("099333444", guardado.getTelefono());
        assertEquals(paciente.getCedula(), guardado.getCedula());
        assertEquals("Nueva dirección", guardado.getDomicilio());
        verify(proveedor).actualizarCorreo(paciente.getFirebaseUid(), "nuevo-perfil@ejemplo.com");
    }

    @Test
    void permisosYValidacionesSeAplicanEnHttp() throws Exception {
        mvc.perform(get("/api/pacientes/me/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pacientes/me/perfil").header("Authorization", "Bearer funcionario")).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios-internos/me/perfil").header("Authorization", "Bearer paciente")).andExpect(status().isForbidden());
        for (String cuerpo : new String[]{"{}", "{\"correo\":\"invalido\"}", "{\"correo\":\"a@b.com\",\"telefono\":\"" + "x".repeat(31) + "\"}"}) {
            mvc.perform(put("/api/pacientes/me/perfil").header("Authorization", "Bearer paciente")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isBadRequest());
        }
        assertEquals("099111222", pacientes.findById(paciente.getIdUsuario()).orElseThrow().getTelefono());
    }

    @Test
    void internoEditaContactoYNoDomicilio() throws Exception {
        mvc.perform(get("/api/usuarios-internos/me/perfil").header("Authorization", "Bearer funcionario"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("funcionario"));
        servicio.actualizarPerfil(funcionario.getIdUsuario(), new PerfilDTO(funcionario.getCorreo(), "099000111", null));
        assertEquals("099000111", usuarios.findById(funcionario.getIdUsuario()).orElseThrow().getTelefono());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> servicio.actualizarPerfil(
            funcionario.getIdUsuario(), new PerfilDTO(funcionario.getCorreo(), "otro", "Dirección"))).getStatusCode().value());
    }

    @Test
    void correoDuplicadoNoCambiaNingunDato() {
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> servicio.actualizarPerfil(paciente.getIdUsuario(),
            new PerfilDTO(funcionario.getCorreo().toUpperCase(), "otro", null))).getStatusCode().value());
        assertEquals("099111222", pacientes.findById(paciente.getIdUsuario()).orElseThrow().getTelefono());
        verify(proveedor, never()).actualizarCorreo(anyString(), anyString());
    }

    @Test
    void errorDelProveedorRevierteCorreoTelefonoYDomicilio() {
        doThrow(new ProveedorIdentidadException("Sin conexión")).when(proveedor).actualizarCorreo(anyString(), anyString());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> servicio.actualizarPerfil(paciente.getIdUsuario(),
            new PerfilDTO("cambio-perfil@ejemplo.com", "otro", "Otro domicilio"))).getStatusCode().value());
        Paciente guardado = pacientes.findById(paciente.getIdUsuario()).orElseThrow();
        assertEquals(paciente.getCorreo(), guardado.getCorreo());
        assertEquals("099111222", guardado.getTelefono());
        assertEquals("Calle anterior", guardado.getDomicilio());
    }
}
