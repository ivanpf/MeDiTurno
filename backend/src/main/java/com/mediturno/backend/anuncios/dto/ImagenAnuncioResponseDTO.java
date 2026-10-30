package com.mediturno.backend.anuncios.dto;

/** La imagen de un anuncio. El archivo se pide a {@code GET /api/anuncios/imagenes/{idImagen}}. */
public record ImagenAnuncioResponseDTO(Long idImagen, String nombreArchivo, String textoAlternativo) {
}
