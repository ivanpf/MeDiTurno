package com.mediturno.backend.consultorios.dto;

import com.mediturno.backend.common.model.EstadoActivacion;

/**
 * DTO para la respuesta del caso de uso CU-37.
 */
public record ConsultorioResponseDTO(
    String codigo,
    String ubicacion,
    String nombre,
    String descripcion,
    EstadoActivacion estado
) {}