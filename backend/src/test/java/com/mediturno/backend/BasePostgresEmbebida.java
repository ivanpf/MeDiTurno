package com.mediturno.backend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.test.context.DynamicPropertyRegistry;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Un único PostgreSQL embebido (sin Podman) para todas las pruebas que necesitan una base real. Se levanta una sola vez, lo
 * que más tarda, y cada clase de prueba usa su propia base dentro de ese servidor: así los datos de una no afectan a las otras.
 * Cada clase lo usa con su propio {@code @DynamicPropertySource}:
 * <pre>
 * &#64;DynamicPropertySource
 * static void base(DynamicPropertyRegistry registro) { registrarBaseDeDatos(registro, "mi_clase"); }
 * </pre>
 */
public abstract class BasePostgresEmbebida {

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException e) {
                    // al apagar la JVM no hay nada más que hacer
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Crea (si no existe) una base propia dentro del servidor compartido y la registra como la base de la aplicación. */
    protected static void registrarBaseDeDatos(DynamicPropertyRegistry registro, String nombreDeLaBase) {
        crearBase(nombreDeLaBase);
        registro.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", nombreDeLaBase));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }

    /** Los nombres son constantes de las propias pruebas, no datos de afuera. */
    private static synchronized void crearBase(String nombre) {
        try (Connection conexion = POSTGRES.getPostgresDatabase().getConnection(); Statement sentencia = conexion.createStatement()) {
            try (ResultSet existe = sentencia.executeQuery("select 1 from pg_database where datname = '" + nombre + "'")) {
                if (existe.next()) {
                    return;
                }
            }
            sentencia.execute("create database " + nombre);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base de pruebas " + nombre, e);
        }
    }
}
