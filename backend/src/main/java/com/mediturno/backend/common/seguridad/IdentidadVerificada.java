package com.mediturno.backend.common.seguridad;

/** Identidad que el proveedor confirmó a partir de un token: su uid y el correo de la cuenta. */
public record IdentidadVerificada(String uid, String correo) {
}
