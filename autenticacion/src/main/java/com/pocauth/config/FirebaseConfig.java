package com.pocauth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;

// Inicializa el Admin SDK de Firebase usando el archivo de credenciales
// de la cuenta de servicio (descargado desde la consola de Firebase).
@Component
public class FirebaseConfig {

    @Value("${firebase.credenciales}")
    private String rutaCredenciales;

    @PostConstruct
    public void inicializar() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (FileInputStream credenciales = new FileInputStream(rutaCredenciales)) {
            FirebaseOptions opciones = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credenciales))
                    .build();
            FirebaseApp.initializeApp(opciones);
        }
    }
}
