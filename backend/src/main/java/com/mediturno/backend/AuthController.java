package com.mediturno.backend;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login de la app.
 *
 * OJO: esto NO es autenticacion segura. No hay token ni sesion: el servidor
 * responde quien es el usuario y la app se lo cree. Cualquiera que sepa la URL
 * puede llamar al resto de los endpoints sin haber pasado por aca. Alcanza para
 * el POC; para produccion hay que agregar JWT o sesiones.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    /**
     * Spring inyecta solo estos dos parametros al crear el controlador: no hay
     * que instanciar nada a mano.
     */
    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    /** Lo que manda la app. Un record es una clase inmutable de una linea. */
    public record LoginRequest(String email, String password) {
    }

    /** Lo que devuelve el servidor. Nunca incluye la contrasena. */
    public record LoginResponse(Long id, String email, String nombre) {
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest datos) {
        if (datos.email() == null || datos.password() == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Usuario> encontrado = usuarios.findByEmail(datos.email().trim().toLowerCase());

        // Se responde lo mismo si el email no existe o si la clave esta mal: si
        // el mensaje fuera distinto, se podria averiguar que emails hay dados de
        // alta probando de a uno.
        if (encontrado.isEmpty() || !encoder.matches(datos.password(), encontrado.get().getPassword())) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = encontrado.get();
        return ResponseEntity.ok(new LoginResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre()));
    }
}
