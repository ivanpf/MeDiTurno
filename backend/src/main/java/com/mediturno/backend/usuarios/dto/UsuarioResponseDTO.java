package com.mediturno.backend.usuarios.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.mediturno.backend.common.model.EstadoActivacion;

/** Un usuario interno (administrador, funcionario o médico). {@code matricula} y {@code especialidades} son solo de los médicos. */
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
        List<EspecialidadAsignada> especialidades,
        LocalDateTime fechaCreacion) {

    /** Una especialidad que atiende el médico, con su estado: puede estar inactiva si se la inactivó después de asignarla. */
    public record EspecialidadAsignada(String codigo, String nombre, EstadoActivacion estado) {
    }
}
