package com.mediturno.backend.usuarios.dto;

/**
 * CU-34: cédula, nombre, apellido, correo, rol y, si es médico, especialidades. TODO: definir los campos al implementar el caso de uso.
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
        @Size(max = 100, message = "El correo admite hasta 100 caracteres") 
        String correo,

        @NotNull(message = "El rol es obligatorio")
        RolUsuarioInterno rol,

        List<Long> especialidadIds
) {
    
    public enum RolUsuarioInterno {
        ADMINISTRADOR,
        FUNCIONARIO,
        MEDICO
    }
}