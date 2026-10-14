package com.mediturno.backend.turnos.repository;

import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OcurrenciaTurnoRepository extends JpaRepository<OcurrenciaTurno, Long> {
    List<OcurrenciaTurno> findByTurnoSemanalEspecialidadCodigoAndFechaBetweenOrderByFechaAscHoraInicioAsc(
            String codigo, LocalDate desde, LocalDate hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OcurrenciaTurno o where o.idOcurrenciaTurno = :id")
    Optional<OcurrenciaTurno> bloquearPorId(@Param("id") Long id);
}
