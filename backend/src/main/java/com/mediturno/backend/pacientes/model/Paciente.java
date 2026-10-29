package com.mediturno.backend.pacientes.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Usuario;

@Entity
@Table(name = "paciente")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Paciente extends Usuario {

    @Column(name = "domicilio", nullable = true, length = 255)
    private String domicilio;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    /** Último administrador que modificó los datos del paciente (CU-44); nulo si nunca se modificó. */
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

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }
}
