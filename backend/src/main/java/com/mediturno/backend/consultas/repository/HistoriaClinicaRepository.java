package com.mediturno.backend.consultas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.consultas.model.HistoriaClinica;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    /** La historia clínica del paciente, si ya tiene una. */
    Optional<HistoriaClinica> findFirstByPacienteIdUsuarioOrderByIdHistoriaClinicaAsc(Long idPaciente);
}
