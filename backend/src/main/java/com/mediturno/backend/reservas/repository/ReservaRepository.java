package com.mediturno.backend.reservas.repository;

import com.mediturno.backend.reservas.model.Reserva;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    /** CU-22: atención del día, propia del médico y aún habilitada. Una orden debe existir y estar emitida. */
    @Query("""
        select count(r) > 0 from Reserva r
        join r.ocurrenciaTurno o join o.turnoSemanal t
        where r.paciente.idUsuario = :idPaciente and t.medico.idUsuario = :idMedico
          and o.fecha = :fecha
          and o.ocurrenciaTurnoEstado = com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno.HABILITADA
          and t.vigenteDesde <= :fecha and (t.vigenteHasta is null or t.vigenteHasta >= :fecha)
          and (r.reservaEstado = com.mediturno.backend.reservas.model.EstadoReserva.VIGENTE
            or (r.reservaEstado = com.mediturno.backend.reservas.model.EstadoReserva.ORDEN_EMITIDA
                and exists (select orden.numeroOrden from Orden orden where orden.reserva = r
                  and orden.paciente.idUsuario = :idPaciente
                  and orden.ordenEstado = com.mediturno.backend.ordenes.model.EstadoOrden.EMITIDA)))
          and not exists (select s.idSuspensionTurno from SuspensionTurno s where s.turnoSemanal = t
            and s.fechaRehabilitacion is null and s.fechaInicio <= :fecha
            and (s.fechaFin is null or s.fechaFin >= :fecha))
        """)
    boolean existeAtencionAutorizada(@Param("idMedico") Long idMedico, @Param("idPaciente") Long idPaciente,
            @Param("fecha") java.time.LocalDate fecha);

    Optional<Reserva> findByIdReservaAndPacienteIdUsuario(Long idReserva, Long idPaciente);

    /** CU-9: consulta escalar para comprobar la propiedad antes de bloquear ocurrencia y reserva, en ese orden. */
    @Query("select r.ocurrenciaTurno.idOcurrenciaTurno from Reserva r where r.idReserva = :id and r.paciente.idUsuario = :idPaciente")
    Optional<Long> buscarOcurrenciaPropia(@Param("id") Long id, @Param("idPaciente") Long idPaciente);

    /** Listado de apoyo de CU-9: próximas reservas propias, conservando las canceladas para mostrar su registro. */
    @Query("""
        select r from Reserva r
        join fetch r.ocurrenciaTurno o
        join fetch o.turnoSemanal t
        join fetch t.medico
        join fetch t.especialidad
        join fetch t.consultorio
        join fetch r.paciente
        where r.paciente.idUsuario = :idPaciente and o.fecha >= :desde
        order by o.fecha, o.horaInicio, r.numero, r.idReserva
        """)
    List<Reserva> buscarProximasPropias(@Param("idPaciente") Long idPaciente, @Param("desde") java.time.LocalDate desde);

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
