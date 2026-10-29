package com.mediturno.backend.consultorios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mediturno.backend.common.model.EstadoActivacion;

/**
 * CU-47: nombre, ubicación, descripción (opcional) y estado. El código no se puede modificar: sale de la ruta.
 * Se envían los cuatro campos (la descripción vacía la borra).
 */
public record ModificarConsultorioDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "La ubicación es obligatoria")
        @Size(max = 200, message = "La ubicación admite hasta 200 caracteres")
        String ubicacion,

        @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
        String descripcion,

        @NotNull(message = "El estado es obligatorio")
        EstadoActivacion estado) {
}
