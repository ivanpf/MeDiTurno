package com.mediturno.backend.reservas.repository;

import com.mediturno.backend.reservas.model.Reserva;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Busca la reserva y la bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): dos emisiones de orden no se pisan. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.idReserva = :id")
    Optional<Reserva> buscarConLock(@Param("id") Long id);

    /** CU-21: excluye reservas canceladas, anuladas, vencidas y atendidas. */
    @Query("""
        select r from Reserva r join fetch r.paciente
        where r.ocurrenciaTurno.idOcurrenciaTurno = :idOcurrencia
          and r.reservaEstado in (com.mediturno.backend.reservas.model.EstadoReserva.VIGENTE,
                                  com.mediturno.backend.reservas.model.EstadoReserva.ORDEN_EMITIDA)
        order by r.numero, r.idReserva
        """)
    List<Reserva> buscarPacientesPorOcurrencia(@Param("idOcurrencia") Long idOcurrencia);

    /**
     * Reservas del turno que todavía no se atendieron (vigentes o con orden emitida) cuya fecha de atención está entre dos
     * fechas (inclusive), por fecha y número. Son las que alcanza una suspensión.
     */
    @Query("""
        select r from Reserva r
        where r.ocurrenciaTurno.turnoSemanal.idTurnoSemanal = :idTurno
          and r.reservaEstado in (com.mediturno.backend.reservas.model.EstadoReserva.VIGENTE,
                                  com.mediturno.backend.reservas.model.EstadoReserva.ORDEN_EMITIDA)
          and r.ocurrenciaTurno.fecha between :desde and :hasta
        order by r.ocurrenciaTurno.fecha, r.numero
        """)
    List<Reserva> buscarPendientesDelTurnoEntreFechas(
            @Param("idTurno") Long idTurno, @Param("desde") java.time.LocalDate desde, @Param("hasta") java.time.LocalDate hasta);

    /** Números de la ocurrencia que tienen una reserva que los conserva (las canceladas y anuladas los liberan), en orden. */
    @Query("""
        select r.numero from Reserva r
        where r.ocurrenciaTurno.idOcurrenciaTurno = :idOcurrenciaTurno
          and r.reservaEstado not in (com.mediturno.backend.reservas.model.EstadoReserva.CANCELADA,
                                      com.mediturno.backend.reservas.model.EstadoReserva.ANULADA)
        order by r.numero
        """)
    List<Integer> buscarNumerosOcupados(@Param("idOcurrenciaTurno") Long idOcurrenciaTurno);
}
