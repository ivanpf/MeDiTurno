package com.mediturno.backend.reservas.dto;

import java.time.LocalTime;

/**
 * Confirmación CU-8. Los campos adicionales de consulta e historial se incorporarán con esos casos de uso.
 */
public record ReservaResponseDTO(Long idReserva, Integer numero, LocalTime horaEstimada) {
}
