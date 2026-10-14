package com.mediturno.backend.turnos.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.turnos.model.OcurrenciaTurno;

public interface OcurrenciaTurnoRepository extends JpaRepository<OcurrenciaTurno, Long> {

    /** Fechas que ya tienen ocurrencia para un turno, entre dos fechas (inclusive). */
    @Query("select o.fecha from OcurrenciaTurno o where o.turnoSemanal.idTurnoSemanal = :idTurno and o.fecha between :desde and :hasta")
    List<LocalDate> fechasExistentes(@Param("idTurno") Long idTurno, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
