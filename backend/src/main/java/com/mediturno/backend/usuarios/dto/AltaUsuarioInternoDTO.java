package com.mediturno.backend.usuarios.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * CU-34: datos personales y rol; matrícula y especialidades son obligatorias para médicos.
 */
public record AltaUsuarioInternoDTO(

        @NotBlank(message = "La cédula es obligatoria")
        @Size(max = 20, message = "La cédula admite hasta 20 caracteres")
        String cedula,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres") 
        String nombre,
       
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido admite hasta 100 caracteres") 
        String apellido,
         
        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 255, message = "El correo electrónico admite hasta 255 caracteres")
        String correo,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotNull(message = "El rol es obligatorio")
        RolUsuarioInterno rol,

        @Size(max = 30, message = "La matrícula admite hasta 30 caracteres")
        String matricula,

        List<String> especialidadIds
) {
    public enum RolUsuarioInterno {
        ADMINISTRADOR,
        FUNCIONARIO,
        MEDICO
    }
}