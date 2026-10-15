package com.mediturno.backend.turnos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.turnos.model.OcurrenciaTurno;

public interface OcurrenciaTurnoRepository extends JpaRepository<OcurrenciaTurno, Long> {

    /** Las ocurrencias de los turnos de una especialidad entre dos fechas (inclusive), ordenadas por fecha y hora de inicio. */
    @Query("""
        select o from OcurrenciaTurno o
        where o.turnoSemanal.especialidad.codigo = :codigoEspecialidad
          and o.fecha between :desde and :hasta
        order by o.fecha, o.horaInicio
        """)
    List<OcurrenciaTurno> buscarPorEspecialidadEntreFechas(
            @Param("codigoEspecialidad") String codigoEspecialidad, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Fechas que ya tienen ocurrencia para un turno, entre dos fechas (inclusive). */
    @Query("select o.fecha from OcurrenciaTurno o where o.turnoSemanal.idTurnoSemanal = :idTurno and o.fecha between :desde and :hasta")
    List<LocalDate> fechasExistentes(@Param("idTurno") Long idTurno, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Busca la ocurrencia y la bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): las reservas de una misma ocurrencia se hacen de a una. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OcurrenciaTurno o where o.idOcurrenciaTurno = :id")
    Optional<OcurrenciaTurno> buscarConLock(@Param("id") Long id);
}
