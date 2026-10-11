package com.mediturno.backend.pases.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.pacientes.model.Paciente;

@Entity
@Table(name = "pase")
public class Pase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pase")
    private Long idPase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_consulta", nullable = false)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especialidad_origen", nullable = false)
    private Especialidad especialidadOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especialidad_destino", nullable = false)
    private Especialidad especialidadDestino;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "pase_estado", nullable = false, length = 20)
    private EstadoPase paseEstado;

    public Long getIdPase() {
        return idPase;
    }

    public void setIdPase(Long idPase) {
        this.idPase = idPase;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Consulta getConsulta() {
        return consulta;
    }

    public void setConsulta(Consulta consulta) {
        this.consulta = consulta;
    }

    public Especialidad getEspecialidadOrigen() {
        return especialidadOrigen;
    }

    public void setEspecialidadOrigen(Especialidad especialidadOrigen) {
        this.especialidadOrigen = especialidadOrigen;
    }

    public Especialidad getEspecialidadDestino() {
        return especialidadDestino;
    }

    public void setEspecialidadDestino(Especialidad especialidadDestino) {
        this.especialidadDestino = especialidadDestino;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public EstadoPase getPaseEstado() {
        return paseEstado;
    }

    public void setPaseEstado(EstadoPase paseEstado) {
        this.paseEstado = paseEstado;
    }
}
