package com.pocauth.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Verifica el header "Authorization: Bearer <idToken>" en cada request.
// Si el token es valido, deja al usuario autenticado en el SecurityContext
// (con su claim "rol" convertido en un GrantedAuthority tipo ROLE_xxx).
@Component
public class FiltroAutenticacionFirebase extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring("Bearer ".length());

            try {
                FirebaseToken decodificado = FirebaseAuth.getInstance().verifyIdToken(idToken);

                Object rolClaim = decodificado.getClaims().get("rol");
                List<GrantedAuthority> autoridades = rolClaim != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + rolClaim.toString().toUpperCase()))
                        : List.of();

                var autenticacion = new UsernamePasswordAuthenticationToken(decodificado, null, autoridades);
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (Exception e) {
                // Token invalido o expirado: se deja sin autenticar,
                // Spring Security se encarga de responder 401/403 segun la ruta.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
