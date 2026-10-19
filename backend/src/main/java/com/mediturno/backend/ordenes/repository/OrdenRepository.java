package com.mediturno.backend.ordenes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.ordenes.model.Orden;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /** La orden emitida para una reserva (hay una sola por reserva), si existe. */
    Optional<Orden> findByReservaIdReserva(Long idReserva);
}
