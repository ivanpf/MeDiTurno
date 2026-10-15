package com.mediturno.backend.consultorios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.consultorios.model.Consultorio;

public interface ConsultorioRepository extends JpaRepository<Consultorio, String> {
}



