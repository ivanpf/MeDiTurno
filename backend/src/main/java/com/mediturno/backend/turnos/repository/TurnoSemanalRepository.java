package com.mediturno.backend.turnos.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;

public interface TurnoSemanalRepository extends JpaRepository<TurnoSemanal, Long>, JpaSpecificationExecutor<TurnoSemanal> {

    /**
     * Busca el turno y lo bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): quien lo pida después espera.
     * Así dos suspensiones del mismo turno no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TurnoSemanal t where t.idTurnoSemanal = :id")
    Optional<TurnoSemanal> buscarConLock(@Param("id") Long id);

    /** Ids de los turnos en alguno de esos estados cuya vigencia coincide en algún día con el período entre las dos fechas. */
    @Query("""
        select t.idTurnoSemanal from TurnoSemanal t
        where t.estado in :estados
          and t.vigenteDesde <= :hasta
          and (t.vigenteHasta is null or t.vigenteHasta >= :desde)
        order by t.idTurnoSemanal
        """)
    List<Long> idsConVigenciaEntre(@Param("estados") Collection<EstadoTurnoSemanal> estados, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * Turnos que se superponen con la franja propuesta: del mismo médico o del mismo consultorio, el mismo día de la
     * semana, con horarios que se intersectan (inicio &lt; finExistente y fin &gt; inicioExistente) y cuya vigencia
     * todavía no terminó cuando empieza la del nuevo turno. Incluye los suspendidos, porque pueden rehabilitarse.
     */
    @Query("""
        select t from TurnoSemanal t
        where t.dia = :dia
          and (t.medico.idUsuario = :idMedico or t.consultorio.codigo = :codigoConsultorio)
          and t.horaInicio < :horaFin
          and t.horaFin > :horaInicio
          and (t.vigenteHasta is null or t.vigenteHasta >= :vigenteDesde)
        order by t.horaInicio
        """)
    List<TurnoSemanal> buscarSuperposiciones(
            @Param("dia") DiaSemana dia,
            @Param("idMedico") Long idMedico,
            @Param("codigoConsultorio") String codigoConsultorio,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("vigenteDesde") LocalDate vigenteDesde);
}
