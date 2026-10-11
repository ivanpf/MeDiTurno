package com.mediturno.backend.consultas.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.consultas.model.Consulta;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
}
