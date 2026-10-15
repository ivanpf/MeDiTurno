package com.mediturno.backend.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación, en la zona horaria de la institución. Toda fecha u hora de «ahora» sale de acá
 * ({@code LocalDate.now(reloj)}, {@code LocalDateTime.now(reloj)}), nunca de {@code now()} sin reloj: así no depende de
 * cómo esté configurado el servidor y las pruebas pueden fijar la hora (ver ReservaIntegracionTest).
 */
@Configuration
public class FechaConfig {

    public static final ZoneId ZONA = ZoneId.of("America/Montevideo");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA);
    }
}
