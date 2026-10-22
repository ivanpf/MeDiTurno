package com.mediturno.backend.consultas.repository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.reservas.model.EstadoReserva;

public interface ConsultaRepository extends JpaRepository<Consulta, Long>, JpaSpecificationExecutor<Consulta> {

    /** CU-22: atenciones anteriores finalizadas del paciente, incluso las realizadas por otros médicos. */
    @Query("""
        select c from Consulta c
        join fetch c.paciente
        join fetch c.medico
        join fetch c.orden
        join fetch c.reserva r
        join fetch r.ocurrenciaTurno o
        join fetch o.turnoSemanal t
        join fetch t.especialidad
        where c.paciente.idUsuario = :idPaciente
          and r.reservaEstado = com.mediturno.backend.reservas.model.EstadoReserva.ATENDIDA
          and c.fecha <= :ahora
        order by c.fecha desc, c.idConsulta desc
        """)
    List<Consulta> buscarAntecedentesDePaciente(@Param("idPaciente") Long idPaciente, @Param("ahora") LocalDateTime ahora);

    /** Si la orden ya tiene su consulta registrada (hay una sola por orden). */
    boolean existsByOrdenNumeroOrden(Long numeroOrden);

    @Query("""
        select c from Consulta c
        join c.reserva r
        where c.idConsulta = :idConsulta
          and c.paciente.idUsuario = :idPaciente
          and r.reservaEstado = :estado
        """)
    Optional<Consulta> buscarAtencionFinalizadaDePaciente(
        @Param("idPaciente") Long idPaciente,
        @Param("idConsulta") Long idConsulta,
        @Param("estado") EstadoReserva estado);
}
