package com.mediturno.backend.turnos.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;

public interface TurnoSemanalRepository extends JpaRepository<TurnoSemanal, Long> {

    /** Ids de los turnos en ese estado cuya vigencia coincide en algún día con el período entre las dos fechas. */
    @Query("""
        select t.idTurnoSemanal from TurnoSemanal t
        where t.estado = :estado
          and t.vigenteDesde <= :hasta
          and (t.vigenteHasta is null or t.vigenteHasta >= :desde)
        order by t.idTurnoSemanal
        """)
    List<Long> idsConVigenciaEntre(@Param("estado") EstadoTurnoSemanal estado, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

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
