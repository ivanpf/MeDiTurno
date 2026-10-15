package com.mediturno.backend.common.tareas;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mediturno.backend.turnos.DisponibilidadService;
import com.mediturno.backend.turnos.GeneradorOcurrenciasService;

/**
 * Tareas que el backend ejecuta solo, sin que nadie las pida. Hoy: generar las ocurrencias de los turnos semanales.
 * Para agregar otra: un método con {@code @Scheduled} que llame a un servicio (la lógica va en el servicio, no acá).
 * La hora sale del Clock de la aplicación (zona America/Montevideo, ver FechaConfig).
 */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final GeneradorOcurrenciasService generadorOcurrenciasService;
    private final DisponibilidadService disponibilidadService;
    private final Clock reloj;

    public TareasProgramadas(GeneradorOcurrenciasService generadorOcurrenciasService, DisponibilidadService disponibilidadService, Clock reloj) {
        this.generadorOcurrenciasService = generadorOcurrenciasService;
        this.disponibilidadService = disponibilidadService;
        this.reloj = reloj;
    }

    /** Todos los días a las 2 de la mañana (configurable con mediturno.ocurrencias.cron). */
    @Scheduled(cron = "${mediturno.ocurrencias.cron:0 0 2 * * *}")
    public void generarOcurrenciasDiarias() {
        generarOcurrencias();
    }

    /** También al arrancar, para que un día sin servicio no deje fechas sin generar. */
    @EventListener(ApplicationReadyEvent.class)
    public void generarOcurrenciasAlArrancar() {
        generarOcurrencias();
    }

    private void generarOcurrencias() {
        try {
            // Se generan hasta donde se puede reservar (la misma ventana que usa CU-8).
            int dias = disponibilidadService.diasDeReserva();
            int creadas = generadorOcurrenciasService.generarOcurrencias(LocalDateTime.now(reloj), dias);
            log.info("Ocurrencias de turnos: se crearon {} para los próximos {} días", creadas, dias);
        } catch (RuntimeException e) {
            log.error("Falló la generación de ocurrencias de turnos: {}", e.getMessage(), e);
        }
    }
}
