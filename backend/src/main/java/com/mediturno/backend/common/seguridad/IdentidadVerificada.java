package com.mediturno.backend.common.seguridad;

/** Identidad y claims de autenticación confirmados por Firebase a partir de un token. */
public record IdentidadVerificada(String uid, String correo, boolean correoVerificado, String proveedor) {

	public IdentidadVerificada(String uid, String correo) {
		this(uid, correo, false, null);
	}
}
