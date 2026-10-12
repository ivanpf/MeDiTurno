package com.mediturno.backend.especialidades.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.especialidades.model.Especialidad;

public interface EspecialidadRepository extends JpaRepository<Especialidad, String> {

    boolean existsByNombreIgnoreCase(String nombre);
}
