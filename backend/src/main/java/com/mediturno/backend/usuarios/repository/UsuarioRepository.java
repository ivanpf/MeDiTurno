package com.mediturno.backend.usuarios.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.usuarios.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    boolean existsByCedula(String cedula);

    boolean existsByCorreoIgnoreCase(String correo);
}
