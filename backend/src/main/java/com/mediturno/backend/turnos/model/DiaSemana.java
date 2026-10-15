package com.mediturno.backend.turnos.model;

import java.time.DayOfWeek;

public enum DiaSemana {
    LUNES(DayOfWeek.MONDAY),
    MARTES(DayOfWeek.TUESDAY),
    MIERCOLES(DayOfWeek.WEDNESDAY),
    JUEVES(DayOfWeek.THURSDAY),
    VIERNES(DayOfWeek.FRIDAY),
    SABADO(DayOfWeek.SATURDAY),
    DOMINGO(DayOfWeek.SUNDAY);

    private final DayOfWeek diaDeJava;

    DiaSemana(DayOfWeek diaDeJava) {
        this.diaDeJava = diaDeJava;
    }

    /** El día equivalente de java.time, para calcular fechas. */
    public DayOfWeek aDayOfWeek() {
        return diaDeJava;
    }
}
