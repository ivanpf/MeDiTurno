package com.mediturno.backend.pacientes.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import com.mediturno.backend.usuarios.model.Usuario;

@Entity
@Table(name = "paciente")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Paciente extends Usuario {

    @Column(name = "domicilio", nullable = true, length = 255)
    private String domicilio;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

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
