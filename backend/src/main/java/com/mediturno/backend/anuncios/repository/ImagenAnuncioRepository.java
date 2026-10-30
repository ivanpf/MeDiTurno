package com.mediturno.backend.anuncios.repository;

import java.util.Collection;
import java.util.List;
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

    /** Los datos de las imágenes de esos anuncios, sin el archivo (para no cargar los bytes al armar un listado). */
    @Query("""
        select i.anuncio.idAnuncio as idAnuncio, i.idImagenAnuncio as idImagen,
               i.nombreArchivo as nombreArchivo, i.textoAlternativo as textoAlternativo
        from ImagenAnuncio i where i.anuncio.idAnuncio in :ids
        """)
    List<ResumenImagen> resumenesDe(@Param("ids") Collection<Long> ids);

    interface ResumenImagen {
        Long getIdAnuncio();

        Long getIdImagen();

        String getNombreArchivo();

        String getTextoAlternativo();
    }
}
