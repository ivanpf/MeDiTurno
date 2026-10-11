package com.mediturno.backend.auth;

import com.mediturno.backend.auth.dto.CrearCuentaRequestDTO;
import com.mediturno.backend.auth.dto.CrearCuentaResponseDTO;
import java.time.LocalDateTime;

import com.mediturno.backend.auth.dto.LoginResponseDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-1, CU-2, CU-3, CU-4, CU-7.
 * La contraseña y el inicio con Google los resuelve Firebase Authentication en el front; el backend verifica el token
 * (FiltroAutenticacion). Los métodos que todavía no están implementados lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ProveedorIdentidad proveedorIdentidad;

    public AuthService(UsuarioRepository usuarioRepository, ProveedorIdentidad proveedorIdentidad) {
        this.usuarioRepository = usuarioRepository;
        this.proveedorIdentidad = proveedorIdentidad;
    }

    /**
     * CU-1. Se llama con la persona ya autenticada por el filtro (token válido y cuenta activa), tanto si entró con
     * correo y contraseña como con Google. Registra el acceso y devuelve su rol.
     */
    @Transactional
    public LoginResponseDTO login(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No fue posible iniciar sesión. Verifique sus datos e intente nuevamente."));
        usuario.setUltimoAcceso(LocalDateTime.now());
        return new LoginResponseDTO(usuario.getIdUsuario(), usuario.getNombre() + " " + usuario.getApellido(), identificarRol(usuario));
    }

    /** CU-2. Revoca las sesiones de la persona en Firebase; el front además cierra la sesión local. */
    @Transactional(readOnly = true)
    public void cerrarSesion(Long idUsuario) {
        usuarioRepository.findById(idUsuario)
            .map(Usuario::getFirebaseUid)
            .ifPresent(proveedorIdentidad::revocarSesiones);
    }

    private static String identificarRol(Usuario usuario) {
        if (usuario instanceof Paciente) return "paciente";
        if (usuario instanceof Medico) return "medico";
        if (usuario instanceof Funcionario) return "funcionario";
        return "admin";
    }

    /** CU-3. */
    public boolean cambiarContrasena(Long idUsuario, String actual, String nueva) {
        throw new NoImplementadoException();
    }

    /** CU-4. */
    public void solicitarRecuperacionAcceso(String correo) {
        throw new NoImplementadoException();
    }

    /** CU-4. Sale del diagrama de secuencia de CU-4; no figura en el diagrama de clases. */
    public void validarTokenRecuperacion(String token) {
        throw new NoImplementadoException();
    }

    /** CU-4. */
    public boolean restablecerContrasena(String token, String nuevaContrasena) {
        throw new NoImplementadoException();
    }

    /** CU-7. Uso interno (crea la identidad en el proveedor de autenticación). */
    public CrearCuentaResponseDTO crearCuenta(CrearCuentaRequestDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-7. Uso interno (verifica el token del proveedor de autenticación). */
    public String obtenerIdentidadVerificada(String tokenAcceso) {
        throw new NoImplementadoException();
    }
}
