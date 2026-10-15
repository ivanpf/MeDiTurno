package com.mediturno.backend.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.usuarios.model.Medico;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    boolean existsByMatriculaIgnoreCase(String matricula);
}
