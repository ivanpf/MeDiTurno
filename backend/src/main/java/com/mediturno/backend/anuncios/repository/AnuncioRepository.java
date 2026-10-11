package com.mediturno.backend.anuncios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.anuncios.model.Anuncio;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
}
