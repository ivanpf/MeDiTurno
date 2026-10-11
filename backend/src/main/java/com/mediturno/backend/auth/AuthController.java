package com.mediturno.backend.auth;

import com.mediturno.backend.auth.dto.CambiarContrasenaDTO;
import com.mediturno.backend.auth.dto.GoogleLoginRequestDTO;
import com.mediturno.backend.auth.dto.LoginRequestDTO;
import com.mediturno.backend.auth.dto.LoginResponseDTO;
import com.mediturno.backend.auth.dto.RestablecerContrasenaDTO;
import com.mediturno.backend.auth.dto.SolicitarRecuperacionDTO;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de: CU-1, CU-2, CU-3, CU-4.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioActual usuarioActual;

    public AuthController(AuthService authService, UsuarioActual usuarioActual) {
        this.authService = authService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO credenciales) {
        return authService.login(credenciales);
    }

    @PostMapping("/google")
    public LoginResponseDTO loginConGoogle(@RequestBody GoogleLoginRequestDTO datos) {
        return authService.loginConGoogle(datos);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion() {
        authService.cerrarSesion(usuarioActual.obtenerIdSesionActual());
    }

    @PostMapping("/contrasena")
    public boolean cambiarContrasena(@RequestBody CambiarContrasenaDTO cuerpo) {
        return authService.cambiarContrasena(usuarioActual.obtenerIdUsuarioAutenticado(), cuerpo.actual(), cuerpo.nueva());
    }

    @PostMapping("/recuperacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarRecuperacionAcceso(@RequestBody SolicitarRecuperacionDTO cuerpo) {
        authService.solicitarRecuperacionAcceso(cuerpo.correo());
    }

    @GetMapping("/recuperacion/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validarTokenRecuperacion(@PathVariable String token) {
        authService.validarTokenRecuperacion(token);
    }

    @PostMapping("/recuperacion/{token}/contrasena")
    public boolean restablecerContrasena(@PathVariable String token, @RequestBody RestablecerContrasenaDTO cuerpo) {
        return authService.restablecerContrasena(token, cuerpo.nuevaContrasena());
    }
}
