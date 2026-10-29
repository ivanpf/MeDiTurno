package com.mediturno.backend.pacientes.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * CU-44: nombre, apellido, correo electrónico, fecha de nacimiento (obligatorios), teléfono y domicilio (opcionales).
 * La cédula no se puede modificar y por eso no figura. Se envían todos los campos: los opcionales vacíos borran el dato.
 * Los obligatorios que falten se informan juntos (400 con la lista en {@code detalles}). El formato y la unicidad del
 * correo los valida el servicio.
 */
public record ModificarPacienteDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido admite hasta 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Size(max = 255, message = "El correo electrónico admite hasta 255 caracteres")
        String correo,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Size(max = 30, message = "El teléfono admite hasta 30 caracteres")
        String telefono,

        @Size(max = 255, message = "El domicilio admite hasta 255 caracteres")
        String domicilio) {
}
