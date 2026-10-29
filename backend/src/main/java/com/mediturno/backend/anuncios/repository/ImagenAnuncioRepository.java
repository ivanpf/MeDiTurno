package com.mediturno.backend.anuncios.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.anuncios.model.EstadoAnuncio;
import com.mediturno.backend.anuncios.model.ImagenAnuncio;

public interface ImagenAnuncioRepository extends JpaRepository<ImagenAnuncio, Long> {

    /** La imagen, solo si su anuncio tiene ese estado distinto del indicado (para no servir las de anuncios eliminados). */
    @Query("select i from ImagenAnuncio i where i.idImagenAnuncio = :id and i.anuncio.anuncioEstado <> :excluido")
    Optional<ImagenAnuncio> buscarSalvoEstado(@Param("id") Long id, @Param("excluido") EstadoAnuncio excluido);
}
