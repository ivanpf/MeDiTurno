package com.mediturno.backend.reservas.repository;

import com.mediturno.backend.reservas.model.Reserva;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    @Query("select r.numero from Reserva r where r.ocurrenciaTurno.idOcurrenciaTurno = :id and r.reservaEstado not in (com.mediturno.backend.reservas.model.EstadoReserva.CANCELADA, com.mediturno.backend.reservas.model.EstadoReserva.ANULADA) order by r.numero")
    List<Integer> numerosOcupados(@Param("id") Long id);
}
