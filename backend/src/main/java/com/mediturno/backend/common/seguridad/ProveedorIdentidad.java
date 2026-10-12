package com.mediturno.backend.common.seguridad;

import java.util.Optional;

/**
 * Proveedor externo de identidad (Firebase Authentication). Es una interfaz para poder reemplazarlo en los tests.
 */
public interface ProveedorIdentidad {

    /** Devuelve el uid de la persona si el token es válido y no venció; vacío en cualquier otro caso. */
    Optional<String> verificarToken(String token);

    /** Invalida los tokens de renovación de la persona: sus sesiones dejan de renovarse (CU-2). */
    void revocarSesiones(String uid);
}
