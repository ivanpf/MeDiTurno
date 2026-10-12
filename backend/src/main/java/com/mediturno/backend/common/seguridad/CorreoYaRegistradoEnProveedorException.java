package com.mediturno.backend.common.seguridad;

/** El proveedor de identidad ya tiene una cuenta con ese correo. */
public class CorreoYaRegistradoEnProveedorException extends RuntimeException {

    public CorreoYaRegistradoEnProveedorException() {
        super("Ya existe una cuenta con ese correo en el proveedor de identidad");
    }
}
