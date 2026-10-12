package com.mediturno.backend.pacientes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mediturno.backend.common.model.EstadoActivacion;

/** Paciente del padrón. {@code tieneCuenta} indica si ya creó su cuenta (CU-7); mientras no, solo está en el padrón. */
public record PacienteResponseDTO(
        Long idPaciente,
        String cedula,
        String nombre,
        String apellido,
        String correo,
        LocalDate fechaNacimiento,
        String telefono,
        String domicilio,
        EstadoActivacion estado,
        boolean tieneCuenta,
        LocalDateTime fechaAlta) {
}
