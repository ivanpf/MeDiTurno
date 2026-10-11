package com.mediturno.backend.reservas.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.reservas.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
}
