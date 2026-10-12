package com.mediturno.backend.especialidades.dto;

import com.mediturno.backend.common.model.EstadoActivacion;

/** Especialidad tal como la ven las pantallas (alta, listado y modificación). */
public record EspecialidadResponseDTO(
        String codigo,
        String nombre,
        String descripcion,
        boolean requierePase,
        EstadoActivacion estado) {
}
