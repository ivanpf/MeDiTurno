package com.mediturno.backend.usuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "administrador")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Administrador extends Usuario {
}
