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

    /** CU-21: solo los turnos del médico en la fecha solicitada; no se aplica la ventana de reserva de CU-8. */
    @Query("""
        select o from OcurrenciaTurno o
        join fetch o.turnoSemanal t
        join fetch t.especialidad
        join fetch t.consultorio
        where t.medico.idUsuario = :idMedico and o.fecha = :fecha
        order by o.horaInicio, o.idOcurrenciaTurno
        """)
    List<OcurrenciaTurno> buscarDelMedicoEnFecha(@Param("idMedico") Long idMedico, @Param("fecha") LocalDate fecha);

    @Query("""
        select o from OcurrenciaTurno o
        where o.idOcurrenciaTurno = :id and o.turnoSemanal.medico.idUsuario = :idMedico
        """)
    Optional<OcurrenciaTurno> buscarDelMedico(@Param("id") Long id, @Param("idMedico") Long idMedico);

    /** Las ocurrencias de los turnos de una especialidad entre dos fechas (inclusive), ordenadas por fecha y hora de inicio. */
    @Query("""
        select o from OcurrenciaTurno o
        where o.turnoSemanal.especialidad.codigo = :codigoEspecialidad
          and o.fecha between :desde and :hasta
        order by o.fecha, o.horaInicio
        """)
    List<OcurrenciaTurno> buscarPorEspecialidadEntreFechas(
            @Param("codigoEspecialidad") String codigoEspecialidad, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * CU-41 y CU-42: por turno semanal, la suma de los cupos de sus fechas entre las dos fechas (inclusive), sin las fechas
     * suspendidas (no ofrecen cupos). Cada fila es el id del turno y los cupos. Un filtro nulo no filtra.
     */
    @Query("""
        select t.idTurnoSemanal, sum(o.cupos)
        from OcurrenciaTurno o join o.turnoSemanal t
        where o.fecha between :desde and :hasta
          and o.ocurrenciaTurnoEstado <> com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno.SUSPENDIDA
          and (:codigoEspecialidad is null or t.especialidad.codigo = :codigoEspecialidad)
          and (:idMedico is null or t.medico.idUsuario = :idMedico)
          and (:codigoConsultorio is null or t.consultorio.codigo = :codigoConsultorio)
        group by t.idTurnoSemanal
        """)
    List<Object[]> sumarCuposPorTurno(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("codigoEspecialidad") String codigoEspecialidad, @Param("idMedico") Long idMedico,
            @Param("codigoConsultorio") String codigoConsultorio);

    /** Fechas que ya tienen ocurrencia para un turno, entre dos fechas (inclusive). */
    @Query("select o.fecha from OcurrenciaTurno o where o.turnoSemanal.idTurnoSemanal = :idTurno and o.fecha between :desde and :hasta")
    List<LocalDate> fechasExistentes(@Param("idTurno") Long idTurno, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * Las ocurrencias del turno entre dos fechas (inclusive), bloqueadas hasta el fin de la transacción (SELECT ... FOR UPDATE),
     * en orden de id: así no se mezclan con quien está reservando una de ellas. Se usa al suspender el turno.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o from OcurrenciaTurno o
        where o.turnoSemanal.idTurnoSemanal = :idTurno and o.fecha between :desde and :hasta
        order by o.idOcurrenciaTurno
        """)
    List<OcurrenciaTurno> buscarDelTurnoEntreFechasConLock(
            @Param("idTurno") Long idTurno, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Busca la ocurrencia y la bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): las reservas de una misma ocurrencia se hacen de a una. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OcurrenciaTurno o where o.idOcurrenciaTurno = :id")
    Optional<OcurrenciaTurno> buscarConLock(@Param("id") Long id);
}
