package com.mediturno.backend.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.usuarios.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
