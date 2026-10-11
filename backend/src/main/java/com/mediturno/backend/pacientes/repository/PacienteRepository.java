package com.mediturno.backend.pacientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.pacientes.model.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
