package com.mediturno.backend.anuncios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.anuncios.model.ImagenAnuncio;

public interface ImagenAnuncioRepository extends JpaRepository<ImagenAnuncio, Long> {
}
