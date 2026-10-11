package com.mediturno.backend.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.usuarios.model.Funcionario;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
}
