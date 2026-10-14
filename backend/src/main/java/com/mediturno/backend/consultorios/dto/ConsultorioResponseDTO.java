package com.mediturno.backend.consultorios.dto;

import com.mediturno.backend.common.model.EstadoActivacion;

/** Consultorio tal como lo ven las pantallas (alta, listado y modificación). */
public record ConsultorioResponseDTO(
        String codigo,
        String nombre,
        String ubicacion,
        String descripcion,
        EstadoActivacion estado) {
}
