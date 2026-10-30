package com.mediturno.backend.anuncios.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CU-17. Los datos de un anuncio nuevo; la imagen (opcional) viaja aparte, como archivo.
 *
 * @param fechaInicio        primer día en que se ve en la cartelera; si falta, hoy. Una fecha futura lo deja programado.
 * @param fechaFin           último día en que se ve (inclusive); si falta, no vence.
 * @param textoAlternativo   descripción de la imagen, para quien no puede verla (opcional).
 */
public record AltaAnuncioDTO(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 5000) String contenido,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        @Size(max = 255) String textoAlternativo) {
}
