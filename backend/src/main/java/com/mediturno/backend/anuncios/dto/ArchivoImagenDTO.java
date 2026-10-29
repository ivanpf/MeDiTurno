package com.mediturno.backend.anuncios.dto;

/** El archivo de una imagen de anuncio, listo para devolverlo. */
public record ArchivoImagenDTO(byte[] datos, String tipoContenido) {
}
