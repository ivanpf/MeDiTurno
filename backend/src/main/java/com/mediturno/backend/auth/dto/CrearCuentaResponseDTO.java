package com.mediturno.backend.auth.dto;

/** Resultado de crear la identidad: el uid que le asignó el proveedor de autenticación. Uso interno (CU-7). */
public record CrearCuentaResponseDTO(String idProveedor) {
}
