package com.mediturno.backend.usuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "funcionario")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Funcionario extends Usuario {
}
