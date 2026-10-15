package com.mediturno.backend.turnos.repository;

import com.mediturno.backend.turnos.model.SuspensionTurno;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuspensionTurnoRepository extends JpaRepository<SuspensionTurno, Long> {
    /** Suspensiones del turno que todavía no se rehabilitaron. */
    @Query("select s from SuspensionTurno s where s.turnoSemanal.idTurnoSemanal = :idTurnoSemanal and s.fechaRehabilitacion is null")
    List<SuspensionTurno> buscarSinRehabilitarDelTurno(@Param("idTurnoSemanal") Long idTurnoSemanal);

    /** Suspensiones sin rehabilitar de varios turnos a la vez (para mostrarlas en el listado). */
    @Query("select s from SuspensionTurno s where s.turnoSemanal.idTurnoSemanal in :idsTurnos and s.fechaRehabilitacion is null")
    List<SuspensionTurno> buscarSinRehabilitarDeTurnos(@Param("idsTurnos") Collection<Long> idsTurnos);

    /** Todas las suspensiones que todavía no se rehabilitaron ni terminaron (programadas, vigentes o temporales ya vencidas sin cerrar). */
    @Query("select s from SuspensionTurno s where s.fechaRehabilitacion is null order by s.idSuspensionTurno")
    List<SuspensionTurno> buscarTodasSinRehabilitar();
}
