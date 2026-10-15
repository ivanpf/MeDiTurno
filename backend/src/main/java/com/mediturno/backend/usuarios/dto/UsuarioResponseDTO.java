package com.mediturno.backend.usuarios.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.mediturno.backend.common.model.EstadoActivacion;

public record UsuarioResponseDTO(
        Long idUsuario,
        String cedula,
        String nombre,
        String apellido,
        String correo,
        LocalDate fechaNacimiento,
        EstadoActivacion estado,
        AltaUsuarioInternoDTO.RolUsuarioInterno rol,
        String matricula,
        List<String> especialidadIds,
        LocalDateTime fechaCreacion) {
}
