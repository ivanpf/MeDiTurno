package com.mediturno.backend.usuarios.dto;

import java.util.List;

import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;

/** Médico con las especialidades que atiende. Sirve para elegirlo en las pantallas (por ejemplo, al dar de alta un turno). */
public record MedicoResponseDTO(
        Long idMedico,
        String nombre,
        String apellido,
        List<EspecialidadResponseDTO> especialidades) {
}
