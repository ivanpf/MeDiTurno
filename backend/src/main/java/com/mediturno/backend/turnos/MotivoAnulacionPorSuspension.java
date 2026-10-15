package com.mediturno.backend.turnos;

/**
 * Motivo con el que se anulan las reservas alcanzadas por una suspensión (queda en {@code motivo_cancelacion}). Identifica la
 * suspensión, así después se sabe qué reservas anuló y a quiénes hay que avisar.
 */
public final class MotivoAnulacionPorSuspension {

    private MotivoAnulacionPorSuspension() {
    }

    public static String de(Long idSuspensionTurno) {
        return "Turno suspendido (suspensión #" + idSuspensionTurno + ")";
    }
}
