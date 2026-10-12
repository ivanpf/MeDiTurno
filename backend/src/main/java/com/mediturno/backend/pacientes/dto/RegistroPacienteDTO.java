package com.mediturno.backend.pacientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mediturno.backend.usuarios.model.ProveedorAcceso;

/**
 * CU-7: la persona se identifica con la cédula y el correo que figuran en el padrón (los dos tienen que coincidir).
 * El método de registro es {@code proveedor}:
 * <ul>
 *   <li>PASSWORD: hace falta {@code contrasena}; el backend crea la cuenta en Firebase con el correo del padrón.</li>
 *   <li>GOOGLE: hace falta {@code tokenAcceso}, el token que Firebase le dio al front al iniciar sesión con Google;
 *       esa cuenta de Google tiene que usar el mismo correo del padrón.</li>
 * </ul>
 * Teléfono y domicilio son opcionales y, si se completan, reemplazan a los del padrón.
 */
public record RegistroPacienteDTO(
        @NotBlank(message = "La cédula es obligatoria")
        String cedula,

        @NotNull(message = "Indique el método de registro")
        ProveedorAcceso proveedor,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Size(max = 255, message = "El correo electrónico admite hasta 255 caracteres")
        String correo,

        @Size(max = 128, message = "La contraseña admite hasta 128 caracteres")
        String contrasena,

        String tokenAcceso,

        @Size(max = 30, message = "El teléfono admite hasta 30 caracteres")
        String telefono,

        @Size(max = 255, message = "El domicilio admite hasta 255 caracteres")
        String domicilio) {
}
