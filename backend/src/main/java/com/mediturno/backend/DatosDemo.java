package com.mediturno.backend;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga datos de prueba al arrancar, si la base esta vacia.
 *
 * Esta clase es la que hace que el proyecto se pueda clonar y funcione. Si los
 * usuarios se cargaran a mano en PostgreSQL, andaria en la maquina de quien los
 * cargo y fallaria en la de los demas. Asi, cualquiera que corra
 * "podman compose up" obtiene la misma base con los mismos datos.
 *
 * CommandLineRunner es una interfaz de Spring: el metodo run() se ejecuta una
 * vez, apenas termina de levantar la aplicacion.
 */
@Component
public class DatosDemo implements CommandLineRunner {

    private static final String[] ESPECIALIDADES = {
            "Clinica Medica", "Cardiologia", "Dermatologia", "Pediatria",
            "Traumatologia", "Oftalmologia", "Odontologia", "Neurologia"
    };

    private static final String[] PROFESIONALES = {
            "Dra. Gomez", "Dr. Fernandez", "Dra. Lopez", "Dr. Martinez",
            "Dra. Ramirez", "Dr. Suarez"
    };

    private static final String[] PACIENTES = {
            "Ana Torres", "Bruno Diaz", "Carla Nunez", "Diego Rossi",
            "Elena Vega", "Franco Pena", "Gisela Mora", "Hernan Ruiz"
    };

    private static final String[] ESTADOS = { "Confirmado", "Pendiente", "Reprogramado" };

    /** Cuantos turnos crear. Con 60 y paginas de 20 quedan 3 paginas. */
    private static final int CANTIDAD_TURNOS = 60;

    private final UsuarioRepository usuarios;
    private final TurnoRepository turnos;
    private final PasswordEncoder encoder;

    public DatosDemo(UsuarioRepository usuarios, TurnoRepository turnos, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.turnos = turnos;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // Solo se cargan si no hay nada: sin este control, cada reinicio del
        // contenedor duplicaria todos los turnos.
        if (usuarios.count() == 0) {
            usuarios.save(new Usuario(
                    "demo@mediturno.com",
                    encoder.encode("1234"),
                    "Usuario Demo"));
        }

        if (turnos.count() == 0) {
            turnos.saveAll(construirTurnos());
        }
    }

    private List<Turno> construirTurnos() {
        List<Turno> lista = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);

        for (int i = 0; i < CANTIDAD_TURNOS; i++) {
            lista.add(new Turno(
                    PACIENTES[i % PACIENTES.length],
                    ESPECIALIDADES[i % ESPECIALIDADES.length],
                    PROFESIONALES[i % PROFESIONALES.length],
                    // Cada turno media hora despues del anterior.
                    inicio.plusMinutes(30L * i),
                    ESTADOS[i % ESTADOS.length]));
        }

        return lista;
    }
}
