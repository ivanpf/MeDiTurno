package com.mediturno.backend.usuarios.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mediturno.backend.usuarios.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    boolean existsByCedula(String cedula);

    boolean existsByCorreoIgnoreCase(String correo);

    /** ¿Otra persona (no la indicada) usa ese correo? Para validar el cambio de correo de alguien que ya existe. */
    boolean existsByCorreoIgnoreCaseAndIdUsuarioNot(String correo, Long idUsuario);

    /** Todas las cédulas registradas, de cualquier rol: las cargas masivas comprueban así los duplicados de una sola vez. */
    @Query("select u.cedula from Usuario u")
    List<String> listarCedulas();

    /** Todos los correos registrados, de cualquier rol, en minúsculas. */
    @Query("select lower(u.correo) from Usuario u")
    List<String> listarCorreosEnMinusculas();
}
