package com.mediturno.backend.anuncios.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mediturno.backend.anuncios.model.EstadoAnuncio;

public record AnuncioResponseDTO(
        Long idAnuncio,
        String titulo,
        String contenido,
        LocalDateTime fechaPublicacion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoAnuncio estado,
        ImagenAnuncioResponseDTO imagen) {
}
