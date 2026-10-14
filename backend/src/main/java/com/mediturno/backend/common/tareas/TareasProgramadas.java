package com.mediturno.backend.common.tareas;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mediturno.backend.turnos.GeneradorOcurrenciasService;

/**
 * Tareas que el backend ejecuta solo, sin que nadie las pida. Hoy: generar las ocurrencias de los turnos semanales.
 * Para agregar otra: un método con {@code @Scheduled} que llame a un servicio (la lógica va en el servicio, no acá).
 * La hora usa la zona horaria del servidor (America/Montevideo en los contenedores).
 */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final GeneradorOcurrenciasService generadorOcurrencias;
    /** Hasta dónde se puede reservar: es hasta donde hay que tener generadas las fechas de los turnos. */
    private final int semanasHaciaAdelante;

    public TareasProgramadas(GeneradorOcurrenciasService generadorOcurrencias,
            @Value("${mediturno.reservas.semanas-adelante}") int semanasHaciaAdelante) {
        this.generadorOcurrencias = generadorOcurrencias;
        this.semanasHaciaAdelante = semanasHaciaAdelante;
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
            int creadas = generadorOcurrencias.generarOcurrencias(LocalDateTime.now(), semanasHaciaAdelante * 7);
            log.info("Ocurrencias de turnos: se crearon {} para las próximas {} semanas", creadas, semanasHaciaAdelante);
        } catch (RuntimeException e) {
            log.error("Falló la generación de ocurrencias de turnos: {}", e.getMessage(), e);
        }
    }
}
