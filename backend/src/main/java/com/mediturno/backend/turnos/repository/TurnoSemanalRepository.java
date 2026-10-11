package com.mediturno.backend.turnos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.turnos.model.TurnoSemanal;

public interface TurnoSemanalRepository extends JpaRepository<TurnoSemanal, Long> {
}
