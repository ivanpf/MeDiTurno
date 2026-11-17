package com.mediturno.backend.common.dto;

import java.time.LocalDate;

public record PerfilResponseDTO(Long idUsuario, String nombre, String cedula, LocalDate fechaNacimiento,
        String rol, String correo, String telefono, String domicilio) {
}
