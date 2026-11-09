package com.mediturno.backend.common.seguridad;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

/**
 * Autentica cada llamada a /api: verifica el token de Firebase (cabecera Authorization: Bearer ...), busca a la persona
 * por su uid y comprueba que la cuenta esté activa. Si algo falla responde 401 con un mensaje genérico, que no revela
 * si la cuenta existe ni su estado. Deja al usuario en la solicitud para {@link UsuarioActual}.
 * Son públicas /api/health y las que usa quien todavía no tiene sesión: el registro de pacientes (CU-7) y la
 * recuperación de acceso (CU-4).
 */
@Component
public class FiltroAutenticacion extends OncePerRequestFilter {

    static final String ATRIBUTO_USUARIO = "mediturno.usuario";
    static final String MENSAJE_NO_AUTENTICADO = "No fue posible iniciar sesión. Verifique sus datos e intente nuevamente.";

    private final ProveedorIdentidad proveedorIdentidad;
    private final UsuarioRepository usuarioRepository;

    public FiltroAutenticacion(ProveedorIdentidad proveedorIdentidad, UsuarioRepository usuarioRepository) {
        this.proveedorIdentidad = proveedorIdentidad;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return !ruta.startsWith("/api/")
            || ruta.equals("/api/health")
            || ruta.startsWith("/api/pacientes/registro")
            || ruta.equals("/api/auth/vinculacion")
            || ruta.startsWith("/api/auth/recuperacion")
            || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Optional<Usuario> usuario = tokenDe(request)
            .flatMap(proveedorIdentidad::verificarToken)
            .flatMap(uid -> {
                Optional<Usuario> u = usuarioRepository.findByFirebaseUid(uid);
                if (u.isEmpty()) logger.warn("Token válido pero no hay una persona con firebase_uid " + uid);
                return u;
            })
            .filter(u -> u.getEstado() == EstadoActivacion.ACTIVO);

        if (usuario.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"estado\":401,\"mensaje\":\"" + MENSAJE_NO_AUTENTICADO
                + "\",\"detalles\":[],\"fecha\":\"" + Instant.now() + "\"}");
            return;
        }
        request.setAttribute(ATRIBUTO_USUARIO, usuario.get());
        cadena.doFilter(request, response);
    }

    private static Optional<String> tokenDe(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(cabecera.substring("Bearer ".length()).trim());
    }
}
