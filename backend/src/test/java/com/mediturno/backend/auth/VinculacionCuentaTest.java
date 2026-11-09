package com.mediturno.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.IdentidadVerificada;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.ProveedorAcceso;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class VinculacionCuentaTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ProveedorIdentidad proveedor = mock(ProveedorIdentidad.class);
    private final AuthService servicio = new AuthService(usuarios, proveedor, Clock.systemUTC());
    private final Paciente paciente = new Paciente();

    @BeforeEach
    void preparar() {
        paciente.setIdUsuario(12L);
        paciente.setCedula("1.234.567-8");
        paciente.setCorreo("ana@ejemplo.com");
        paciente.setEstado(EstadoActivacion.ACTIVO);
        when(proveedor.verificarIdentidad("token-valido"))
            .thenReturn(Optional.of(new IdentidadVerificada("uid-firebase", "ana@ejemplo.com", false, "password")));
        when(usuarios.buscarPorCorreoConLock("ana@ejemplo.com")).thenReturn(Optional.of(paciente));
        when(usuarios.findByFirebaseUid("uid-firebase")).thenReturn(Optional.empty());
        when(usuarios.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void vinculaElUidSoloDespuesDeVerificarLaIdentidad() {
        servicio.vincularCuentaExistente("token-valido");

        assertEquals("uid-firebase", paciente.getFirebaseUid());
        assertEquals(ProveedorAcceso.PASSWORD, paciente.getProveedorAcceso());
        verify(usuarios).saveAndFlush(paciente);
    }

    @Test
    void rechazaCuentaGoogle() {
        when(proveedor.verificarIdentidad("token-google"))
            .thenReturn(Optional.of(new IdentidadVerificada("uid-google", "ana@ejemplo.com", true, "google.com")));

        assertThrows(ResponseStatusException.class,
            () -> servicio.vincularCuentaExistente("token-google"));

        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void noVinculaSiElCorreoFirebaseNoTienePerfilLocal() {
        when(proveedor.verificarIdentidad("token-ajeno"))
            .thenReturn(Optional.of(new IdentidadVerificada("uid-ajeno", "otro@ejemplo.com", true, "password")));

        assertThrows(ResponseStatusException.class,
            () -> servicio.vincularCuentaExistente("token-ajeno"));

        verify(usuarios, never()).saveAndFlush(any());
    }
}