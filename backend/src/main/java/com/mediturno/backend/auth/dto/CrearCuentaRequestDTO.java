package com.mediturno.backend.auth.dto;

/** Datos para crear la identidad (correo y contraseña) en el proveedor de autenticación. Uso interno (CU-7). */
public record CrearCuentaRequestDTO(String correo, String contrasena) {
}
