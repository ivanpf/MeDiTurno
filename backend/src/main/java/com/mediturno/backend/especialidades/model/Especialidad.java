package com.mediturno.backend.especialidades.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.mediturno.backend.common.model.EstadoActivacion;

@Entity
@Table(name = "especialidad")
public class Especialidad {

    @Id
    @Column(name = "codigo", length = 20)
    private String codigo;

    @Column(name = "requiere_pase", nullable = false)
    private boolean requierePase;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", nullable = true, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoActivacion estado;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public boolean isRequierePase() {
        return requierePase;
    }

    public void setRequierePase(boolean requierePase) {
        this.requierePase = requierePase;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoActivacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoActivacion estado) {
        this.estado = estado;
    }
}
