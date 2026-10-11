package com.mediturno.backend.turnos.model;

import java.time.LocalDate;
import java.time.LocalTime;

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

@Entity
@Table(name = "ocurrencia_turno")
public class OcurrenciaTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ocurrencia_turno")
    private Long idOcurrenciaTurno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_turno_semanal", nullable = false)
    private TurnoSemanal turnoSemanal;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "cupos", nullable = false)
    private Integer cupos;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "ocurrencia_turno_estado", nullable = false, length = 20)
    private EstadoOcurrenciaTurno ocurrenciaTurnoEstado;

    public Long getIdOcurrenciaTurno() {
        return idOcurrenciaTurno;
    }

    public void setIdOcurrenciaTurno(Long idOcurrenciaTurno) {
        this.idOcurrenciaTurno = idOcurrenciaTurno;
    }

    public TurnoSemanal getTurnoSemanal() {
        return turnoSemanal;
    }

    public void setTurnoSemanal(TurnoSemanal turnoSemanal) {
        this.turnoSemanal = turnoSemanal;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Integer getCupos() {
        return cupos;
    }

    public void setCupos(Integer cupos) {
        this.cupos = cupos;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public EstadoOcurrenciaTurno getOcurrenciaTurnoEstado() {
        return ocurrenciaTurnoEstado;
    }

    public void setOcurrenciaTurnoEstado(EstadoOcurrenciaTurno ocurrenciaTurnoEstado) {
        this.ocurrenciaTurnoEstado = ocurrenciaTurnoEstado;
    }
}
