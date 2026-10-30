package com.mediturno.backend.especialidades.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mediturno.backend.common.model.EstadoActivacion;

/**
 * CU-43: nombre, descripción (opcional), requierePase y estado. El código no se puede modificar: sale de la ruta.
 * Se envían los cuatro campos (la descripción vacía la borra).
 */
public record ModificarEspecialidadDTO(
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
