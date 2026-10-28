package com.mediturno.backend.ordenes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.ordenes.model.Orden;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /** La orden emitida para una reserva (hay una sola por reserva), si existe. */
    Optional<Orden> findByReservaIdReserva(Long idReserva);

    @Query("select o from Orden o join fetch o.reserva r where r.ocurrenciaTurno.idOcurrenciaTurno = :idOcurrencia")
    List<Orden> buscarPorOcurrencia(@Param("idOcurrencia") Long idOcurrencia);

    @Query("select o from Orden o where o.reserva.idReserva in :idsReserva")
    List<Orden> buscarPorReservas(@Param("idsReserva") List<Long> idsReserva);
}
