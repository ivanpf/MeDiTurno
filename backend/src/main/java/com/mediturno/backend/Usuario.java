package com.mediturno.backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un usuario que puede ingresar a la app.
 *
 * No hace falta escribir el CREATE TABLE: como application.yaml tiene
 * ddl-auto: update, Hibernate lee esta clase y crea la tabla "usuarios" con
 * una columna por cada campo.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * La contrasena guardada con BCrypt, nunca en texto plano. BCrypt es de una
     * sola via: se puede verificar si una clave coincide, pero no se puede
     * recuperar la original a partir de lo guardado.
     */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nombre;

    /** JPA exige un constructor sin argumentos para poder instanciar la entidad. */
    protected Usuario() {
    }

    public Usuario(String email, String password, String nombre) {
        this.email = email;
        this.password = password;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getNombre() {
        return nombre;
    }
}
