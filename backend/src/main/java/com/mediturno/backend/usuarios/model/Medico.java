package com.mediturno.backend.usuarios.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import com.mediturno.backend.especialidades.model.Especialidad;

@Entity
@Table(name = "medico")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Medico extends UsuarioInterno {

    @Column(name = "matricula", nullable = false, unique = true, length = 30)
    private String matricula;

    @ManyToMany
    @JoinTable(
        name = "medico_especialidad",
        joinColumns = @JoinColumn(name = "id_medico"),
        inverseJoinColumns = @JoinColumn(name = "codigo_especialidad"))
    private Set<Especialidad> especialidades = new HashSet<>();

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Set<Especialidad> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(Set<Especialidad> especialidades) {
        this.especialidades = especialidades;
    }
}
