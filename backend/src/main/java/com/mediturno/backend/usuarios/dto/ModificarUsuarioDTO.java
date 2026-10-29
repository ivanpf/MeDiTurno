package com.mediturno.backend.usuarios.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO.RolUsuarioInterno;

/**
 * CU-45: los datos que el administrador puede corregir de un usuario interno (la cédula no se modifica). La matrícula y las
 * especialidades son obligatorias cuando el rol es MEDICO y se ignoran en los demás roles.
 */
public record ModificarUsuarioDTO(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido admite hasta 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 255, message = "El correo electrónico admite hasta 255 caracteres")
        String correo,

        @NotNull(message = "El rol es obligatorio")
        RolUsuarioInterno rol,

        @Size(max = 30, message = "La matrícula admite hasta 30 caracteres")
        String matricula,

        List<String> especialidadIds
) {
}
