package com.mediturno.backend.turnos.repository;

import com.mediturno.backend.turnos.model.SuspensionTurno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspensionTurnoRepository extends JpaRepository<SuspensionTurno, Long> {
    List<SuspensionTurno> findByTurnoSemanalIdTurnoSemanalAndFechaRehabilitacionIsNull(Long id);
}
