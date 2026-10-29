package com.mediturno.backend.consultorios.dto;

import com.mediturno.backend.common.model.EstadoActivacion;

/**
 * Consultorio tal como lo ven las pantallas (alta, listado y modificación). {@code cantidadTurnos} son los turnos semanales
 * vigentes que lo usan (CU-39); queda en nulo donde no se calcula (alta y modificación).
 */
public record ConsultorioResponseDTO(
        String codigo,
        String nombre,
        String ubicacion,
        String descripcion,
        EstadoActivacion estado,
        Integer cantidadTurnos) {

    public ConsultorioResponseDTO(String codigo, String nombre, String ubicacion, String descripcion, EstadoActivacion estado) {
        this(codigo, nombre, ubicacion, descripcion, estado, null);
    }
}
