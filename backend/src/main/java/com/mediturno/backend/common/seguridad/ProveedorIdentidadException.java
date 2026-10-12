package com.mediturno.backend.common.seguridad;

/** El proveedor de identidad no pudo completar la operación (no está disponible o la rechazó). */
public class ProveedorIdentidadException extends RuntimeException {

    public ProveedorIdentidadException(String mensaje) {
        super(mensaje);
    }

    public ProveedorIdentidadException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
