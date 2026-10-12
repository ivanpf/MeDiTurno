package com.mediturno.backend.auth.dto;

/**
 * Respuesta del inicio de sesión. El rol es el que usa el front para elegir el menú:
 * paciente, funcionario, medico o admin. El token no viaja acá: lo emite Firebase y el front lo envía en cada llamada.
 */
public record LoginResponseDTO(Long idUsuario, String nombre, String rol) {
}
