package com.mediturno.backend.turnos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.turnos.model.SuspensionTurno;

public interface SuspensionTurnoRepository extends JpaRepository<SuspensionTurno, Long> {
}
