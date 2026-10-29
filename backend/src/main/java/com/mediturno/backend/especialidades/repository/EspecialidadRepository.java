package com.mediturno.backend.especialidades.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.mediturno.backend.especialidades.model.Especialidad;

public interface EspecialidadRepository extends JpaRepository<Especialidad, String>, JpaSpecificationExecutor<Especialidad> {

    boolean existsByNombreIgnoreCase(String nombre);
}
