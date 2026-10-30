package com.mediturno.backend.usuarios.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * Lo que tienen en común los administradores, los funcionarios y los médicos (los usuarios internos que gestiona el
 * administrador en CU-34 a CU-36, CU-45 y CU-46). No es una entidad: sus campos están en la tabla de cada rol.
 */
@MappedSuperclass
public abstract class UsuarioInterno extends Usuario {

    /** Último administrador que modificó, activó o inactivó la cuenta (CU-45 y CU-46); nulo si nunca se modificó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_administrador_modificacion")
    private Administrador administradorModificacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    public Administrador getAdministradorModificacion() {
        return administradorModificacion;
    }

    public void setAdministradorModificacion(Administrador administradorModificacion) {
        this.administradorModificacion = administradorModificacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
