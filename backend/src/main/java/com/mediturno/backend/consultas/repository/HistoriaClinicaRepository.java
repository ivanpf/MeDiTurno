package com.mediturno.backend.consultas.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.consultas.model.HistoriaClinica;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {
}
