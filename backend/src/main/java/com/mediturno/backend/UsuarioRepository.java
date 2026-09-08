package com.mediturno.backend;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de usuarios.
 *
 * No se implementa: Spring Data crea la clase en tiempo de ejecucion. El metodo
 * findByEmail funciona solo porque el nombre sigue la convencion
 * findBy + NombreDelCampo; Spring lo traduce a "WHERE email = ?".
 *
 * Devuelve Optional en vez de null para obligar a contemplar el caso de que el
 * usuario no exista.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}
