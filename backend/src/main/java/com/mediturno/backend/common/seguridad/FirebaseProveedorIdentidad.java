package com.mediturno.backend.common.seguridad;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementación con Firebase. Si no se encuentra el archivo con la clave de servicio, el backend arranca igual
 * pero rechaza todos los tokens (se avisa en el log).
 */
@Component
public class FirebaseProveedorIdentidad implements ProveedorIdentidad {

    private static final Logger log = LoggerFactory.getLogger(FirebaseProveedorIdentidad.class);

    private final FirebaseAuth firebaseAuth;

    public FirebaseProveedorIdentidad(@Value("${mediturno.firebase.credenciales}") String rutaCredenciales) {
        this.firebaseAuth = crearFirebaseAuth(rutaCredenciales);
    }

    private static FirebaseAuth crearFirebaseAuth(String ruta) {
        if (!Files.isRegularFile(Path.of(ruta))) {
            log.warn("No se encontró la clave de Firebase en '{}': no se podrá iniciar sesión. Ver README.", ruta);
            return null;
        }
        try (InputStream clave = new FileInputStream(ruta)) {
            FirebaseOptions opciones = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(clave))
                // Con el transporte por defecto (Apache) falla al descargar las claves públicas de Google ("Not in GZIP format")
                .setHttpTransport(new NetHttpTransport())
                .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                ? FirebaseApp.initializeApp(opciones)
                : FirebaseApp.getInstance();
            return FirebaseAuth.getInstance(app);
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo leer la clave de Firebase '{}': {}", ruta, e.getMessage());
            return null;
        }
    }

    @Override
    public Optional<String> verificarToken(String token) {
        return verificarIdentidad(token).map(IdentidadVerificada::uid);
    }

    @Override
    public Optional<IdentidadVerificada> verificarIdentidad(String token) {
        if (firebaseAuth == null) {
            return Optional.empty();
        }
        try {
            FirebaseToken verificado = firebaseAuth.verifyIdToken(token);
            return Optional.of(new IdentidadVerificada(verificado.getUid(), verificado.getEmail()));
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            log.warn("Token rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void revocarSesiones(String uid) {
        if (firebaseAuth == null) {
            return;
        }
        try {
            firebaseAuth.revokeRefreshTokens(uid);
        } catch (FirebaseAuthException e) {
            log.warn("No se pudieron revocar las sesiones de {}: {}", uid, e.getMessage());
        }
    }

    @Override
    public String crearCuenta(String correo, String contrasena) {
        if (firebaseAuth == null) {
            throw new ProveedorIdentidadException("Firebase no está configurado");
        }
        try {
            UserRecord.CreateRequest pedido = new UserRecord.CreateRequest().setEmail(correo).setPassword(contrasena);
            return firebaseAuth.createUser(pedido).getUid();
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new CorreoYaRegistradoEnProveedorException();
            }
            throw new ProveedorIdentidadException("Firebase no pudo crear la cuenta: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminarCuenta(String uid) {
        if (firebaseAuth == null) {
            return;
        }
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException e) {
            log.error("No se pudo borrar la cuenta {} que quedó sin vincular: {}", uid, e.getMessage());
        }
    }
}
