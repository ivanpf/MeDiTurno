package com.mediturno.backend.auth;

import com.mediturno.backend.auth.dto.CrearCuentaRequestDTO;
import java.time.Clock;
import java.time.LocalDateTime;

import com.mediturno.backend.auth.dto.LoginResponseDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.seguridad.CorreoYaRegistradoEnProveedorException;
import com.mediturno.backend.common.seguridad.IdentidadVerificada;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.common.seguridad.ProveedorIdentidadException;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String MENSAJE_CUENTA_DE_ACCESO = "No se pudo completar el registro de la cuenta de acceso. Intente nuevamente.";

    private final UsuarioRepository usuarioRepository;
    private final ProveedorIdentidad proveedorIdentidad;
    private final Clock reloj;

    public AuthService(UsuarioRepository usuarioRepository, ProveedorIdentidad proveedorIdentidad, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.proveedorIdentidad = proveedorIdentidad;
        this.reloj = reloj;
    }

    /**
     * CU-1. Se llama con la persona ya autenticada por el filtro (token válido y cuenta activa), tanto si entró con
     * correo y contraseña como con Google. Registra el acceso y devuelve su rol.
     */
    @Transactional
    public LoginResponseDTO login(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No fue posible iniciar sesión. Verifique sus datos e intente nuevamente."));
        usuario.setUltimoAcceso(LocalDateTime.now(reloj));
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

    /** CU-7. Uso interno: crea la identidad (correo y contraseña) en el proveedor de autenticación y devuelve su uid. */
    public String crearCuenta(CrearCuentaRequestDTO datos) {
        try {
            return proveedorIdentidad.crearCuenta(datos.correo(), datos.contrasena());
        } catch (CorreoYaRegistradoEnProveedorException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo electrónico. La cuenta no pudo crearse.");
        } catch (ProveedorIdentidadException e) {
            log.error("No se pudo crear la identidad: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MENSAJE_CUENTA_DE_ACCESO);
        }
    }

    /**
     * CU-44. Uso interno: cambia el correo de la cuenta de acceso de una persona para que coincida con el de sus datos.
     * 409 si otra cuenta ya usa ese correo; 503 si el proveedor no pudo hacerlo.
     */
    public void actualizarCorreo(String idProveedor, String correo) {
        try {
            proveedorIdentidad.actualizarCorreo(idProveedor, correo);
        } catch (CorreoYaRegistradoEnProveedorException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta de acceso con ese correo electrónico.");
        } catch (ProveedorIdentidadException e) {
            log.error("No se pudo cambiar el correo de la cuenta {}: {}", idProveedor, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo actualizar el correo de la cuenta de acceso. Intente nuevamente.");
        }
    }

    /** CU-7. Uso interno: verifica el token que el front obtuvo al iniciar sesión con Google y devuelve su uid y correo. */
    public IdentidadVerificada obtenerIdentidadVerificada(String tokenAcceso) {
        return proveedorIdentidad.verificarIdentidad(tokenAcceso)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MENSAJE_CUENTA_DE_ACCESO));
    }

    /** CU-7. Uso interno: deshace la creación de una identidad cuando el registro no pudo completarse. */
    public void eliminarCuenta(String idProveedor) {
        proveedorIdentidad.eliminarCuenta(idProveedor);
    }
}
