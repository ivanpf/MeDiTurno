package com.mediturno.backend.auth;

import com.mediturno.backend.auth.dto.CrearCuentaRequestDTO;
import com.mediturno.backend.auth.dto.CrearCuentaResponseDTO;
import com.mediturno.backend.auth.dto.GoogleLoginRequestDTO;
import com.mediturno.backend.auth.dto.LoginRequestDTO;
import com.mediturno.backend.auth.dto.LoginResponseDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-1, CU-2, CU-3, CU-4, CU-7.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class AuthService {

    /** CU-1. */
    public LoginResponseDTO login(LoginRequestDTO credenciales) {
        throw new NoImplementadoException();
    }

    /** CU-1. Sale del diagrama de secuencia de CU-1; no figura en el diagrama de clases. */
    public LoginResponseDTO loginConGoogle(GoogleLoginRequestDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-2. */
    public void cerrarSesion(Long idSesion) {
        throw new NoImplementadoException();
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
