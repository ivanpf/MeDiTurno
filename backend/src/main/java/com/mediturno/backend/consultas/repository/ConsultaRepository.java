package com.mediturno.backend.consultas.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.consultas.model.Consulta;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    /** Si la orden ya tiene su consulta registrada (hay una sola por orden). */
    boolean existsByOrdenNumeroOrden(Long numeroOrden);
}
