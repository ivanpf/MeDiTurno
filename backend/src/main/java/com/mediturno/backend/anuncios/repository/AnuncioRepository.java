package com.mediturno.backend.anuncios.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.anuncios.model.Anuncio;
import com.mediturno.backend.anuncios.model.EstadoAnuncio;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {

    /** Los anuncios con ese estado que se ven en {@code hoy} (desde el día de inicio hasta el de fin, inclusive), el más nuevo primero. */
    @Query("""
        select a from Anuncio a
        where a.anuncioEstado = :estado
          and (a.fechaInicio is null or a.fechaInicio <= :hoy)
          and (a.fechaFin is null or a.fechaFin >= :hoy)
        order by a.fechaPublicacion desc, a.idAnuncio desc
        """)
    List<Anuncio> buscarVigentes(@Param("estado") EstadoAnuncio estado, @Param("hoy") LocalDate hoy);

    /** Todos los anuncios que hubo, en cualquier estado, el más nuevo primero. */
    List<Anuncio> findAllByOrderByFechaPublicacionDescIdAnuncioDesc();

    /** Busca el anuncio y lo bloquea hasta el fin de la transacción. Llamarlo dentro de una transacción. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Anuncio a where a.idAnuncio = :id")
    Optional<Anuncio> buscarConLock(@Param("id") Long id);
}
