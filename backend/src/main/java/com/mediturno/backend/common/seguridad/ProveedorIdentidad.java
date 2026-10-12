package com.mediturno.backend.common.seguridad;

import java.util.Optional;

/**
 * Proveedor externo de identidad (Firebase Authentication). Es una interfaz para poder reemplazarlo en los tests.
 */
public interface ProveedorIdentidad {

    /** Devuelve el uid de la persona si el token es válido y no venció; vacío en cualquier otro caso. */
    Optional<String> verificarToken(String token);

    /** Igual que {@link #verificarToken}, pero devuelve también el correo de la cuenta (CU-7 con Google). */
    Optional<IdentidadVerificada> verificarIdentidad(String token);

    /** Invalida los tokens de renovación de la persona: sus sesiones dejan de renovarse (CU-2). */
    void revocarSesiones(String uid);

    /**
     * Crea una cuenta con correo y contraseña (CU-7) y devuelve su uid.
     *
     * @throws CorreoYaRegistradoEnProveedorException si ya existe una cuenta con ese correo
     * @throws ProveedorIdentidadException            si el proveedor no pudo crearla
     */
    String crearCuenta(String correo, String contrasena);

    /** Borra una cuenta creada en el proveedor. Se usa para deshacer un registro que no se pudo completar. */
    void eliminarCuenta(String uid);
}
