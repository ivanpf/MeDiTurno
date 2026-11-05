package com.mediturno.backend.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CU-5: solo datos de contacto. El domicilio corresponde a pacientes. */
public record PerfilDTO(
        @NotBlank(message = "Ingrese el correo electrónico")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 255) String correo,
        @Size(max = 30) String telefono,
        @Size(max = 255) String domicilio) {
}
