package com.mediturno.backend.turnos.repository;

import com.mediturno.backend.turnos.model.SuspensionTurno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuspensionTurnoRepository extends JpaRepository<SuspensionTurno, Long> {
    /** Suspensiones del turno que todavía no se rehabilitaron. */
    @Query("select s from SuspensionTurno s where s.turnoSemanal.idTurnoSemanal = :idTurnoSemanal and s.fechaRehabilitacion is null")
    List<SuspensionTurno> buscarSinRehabilitarDelTurno(@Param("idTurnoSemanal") Long idTurnoSemanal);
}
