package com.mediturno.backend;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de turnos.
 *
 * JpaRepository ya trae findAll(Pageable), que es todo lo que necesita el
 * scroll infinito: devuelve una pagina por vez en lugar de la tabla entera.
 */
public interface TurnoRepository extends JpaRepository<Turno, Long> {
}
