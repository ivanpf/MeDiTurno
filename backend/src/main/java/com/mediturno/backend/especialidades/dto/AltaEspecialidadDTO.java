package com.mediturno.backend.especialidades.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mediturno.backend.common.model.EstadoActivacion;

/**
 * CU-28: código, nombre, descripción (opcional), requierePase y estado.
 * Los campos obligatorios que falten se informan juntos (respuesta 400 con la lista en {@code detalles}).
 */
public record AltaEspecialidadDTO(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código admite hasta 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
        String descripcion,

        @NotNull(message = "Indique si requiere pase previo")
        Boolean requierePase,

        @NotNull(message = "El estado es obligatorio")
        EstadoActivacion estado) {
}
