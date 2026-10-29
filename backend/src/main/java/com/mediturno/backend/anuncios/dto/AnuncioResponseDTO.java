package com.mediturno.backend.anuncios.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mediturno.backend.anuncios.model.EstadoAnuncio;
import com.mediturno.backend.anuncios.model.VigenciaAnuncio;

public record AnuncioResponseDTO(
        Long idAnuncio,
        String titulo,
        String contenido,
        LocalDateTime fechaPublicacion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoAnuncio estado,
        VigenciaAnuncio vigencia,
        ImagenAnuncioResponseDTO imagen) {
}
