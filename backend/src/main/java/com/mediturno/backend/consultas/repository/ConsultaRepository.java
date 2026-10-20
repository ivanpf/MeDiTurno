package com.mediturno.backend.consultas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.reservas.model.EstadoReserva;

public interface ConsultaRepository extends JpaRepository<Consulta, Long>, JpaSpecificationExecutor<Consulta> {

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
