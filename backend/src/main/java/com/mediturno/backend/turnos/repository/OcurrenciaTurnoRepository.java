package com.mediturno.backend.turnos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.turnos.model.OcurrenciaTurno;

public interface OcurrenciaTurnoRepository extends JpaRepository<OcurrenciaTurno, Long> {
}
