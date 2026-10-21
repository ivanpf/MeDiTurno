package com.mediturno.backend.consultas.dto;

import java.time.LocalDateTime;

/**
 * Una consulta registrada por el médico (CU-23; también la devuelven CU-11, CU-22 y CU-26): paciente, médico, especialidad,
 * fecha y datos clínicos. {@code idConsulta} es la clave de la consulta y {@code numeroOrden} la orden con la que se atendió.
 */
public record ConsultaResponseDTO(
        Long idConsulta,
        Long numeroOrden,
        Long idReserva,
        Long idPaciente,
        String nombrePaciente,
        Long idMedico,
        String nombreMedico,
        String especialidad,
        LocalDateTime fecha,
        String motivo,
        String observaciones,
        String indicaciones,
        LocalDateTime fechaRegistro) {
}
