package com.mediturno.backend.turnos.model;

import java.time.LocalDate;
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

import com.mediturno.backend.usuarios.model.Funcionario;

@Entity
@Table(name = "suspension_turno")
public class SuspensionTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suspension_turno")
    private Long idSuspensionTurno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_turno_semanal", nullable = false)
    private TurnoSemanal turnoSemanal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Column(name = "periodo", nullable = true, length = 100)
    private String periodo;

    @Column(name = "motivo", nullable = true, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_tipo", nullable = false, length = 20)
    private TipoSuspension suspensionTipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = true)
    private LocalDate fechaFin;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_rehabilitacion", nullable = true)
    private LocalDateTime fechaRehabilitacion;

    public Long getIdSuspensionTurno() {
        return idSuspensionTurno;
    }

    public void setIdSuspensionTurno(Long idSuspensionTurno) {
        this.idSuspensionTurno = idSuspensionTurno;
    }

    public TurnoSemanal getTurnoSemanal() {
        return turnoSemanal;
    }

    public void setTurnoSemanal(TurnoSemanal turnoSemanal) {
        this.turnoSemanal = turnoSemanal;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public TipoSuspension getSuspensionTipo() {
        return suspensionTipo;
    }

    public void setSuspensionTipo(TipoSuspension suspensionTipo) {
        this.suspensionTipo = suspensionTipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getFechaRehabilitacion() {
        return fechaRehabilitacion;
    }

    public void setFechaRehabilitacion(LocalDateTime fechaRehabilitacion) {
        this.fechaRehabilitacion = fechaRehabilitacion;
    }
}
