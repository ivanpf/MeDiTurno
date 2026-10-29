package com.mediturno.backend.consultorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.consultorios.dto.ModificarConsultorioDTO;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;

/** CU-47 Modificación de consultorios, con una base PostgreSQL real. */
class ModificarConsultorioIntegracionTest extends PostgresIntegracionTest {

    @Autowired ConsultorioService servicio;
    @Autowired AdministradorRepository administradores;
    @Autowired JdbcTemplate jdbc;

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Administrador admin;
    private Consultorio sala;
    private Consultorio sinTurnos;
    private int pacientes;

    @BeforeEach
    void datosBase() {
        Administrador a = new Administrador();
        a.setCorreo("9851@ejemplo.com");
        a.setEstado(EstadoActivacion.ACTIVO);
        a.setFechaCreacion(LocalDateTime.now());
        a.setCedula("9851");
        a.setNombre("Admin");
        a.setApellido("Prueba");
        a.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        admin = administradores.save(a);
        sala = crear("MC-SALA", "MC Sala", "MC Planta baja", "Con ventana", EstadoActivacion.ACTIVO);
        sinTurnos = crear("MC-LIBRE", "MC Libre", "MC Piso 2", null, EstadoActivacion.ACTIVO);
    }

    private Consultorio crear(String codigo, String nombre, String ubicacion, String descripcion, EstadoActivacion estado) {
        Consultorio c = new Consultorio();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setUbicacion(ubicacion);
        c.setDescripcion(descripcion);
        c.setEstado(estado);
        return consultorios.saveAndFlush(c);
    }

    private ModificarConsultorioDTO datos(String nombre, String ubicacion, String descripcion, EstadoActivacion estado) {
        return new ModificarConsultorioDTO(nombre, ubicacion, descripcion, estado);
    }

    private ConsultorioResponseDTO modificar(String codigo, ModificarConsultorioDTO datos) {
        return servicio.modificarConsultorio(admin.getIdUsuario(), codigo, datos);
    }

    private HttpStatus errorAl(String codigo, ModificarConsultorioDTO datos) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, () -> modificar(codigo, datos)).getStatusCode();
    }

    private Consultorio guardado(String codigo) {
        consultorios.flush();
        return consultorios.findById(codigo).orElseThrow();
    }

    /** Un turno de la sala; devuelve su id. */
    private TurnoSemanal turnoEnLaSala(EstadoTurnoSemanal estado, LocalDate hasta) {
        Especialidad e = especialidad("MC-E");
        Medico m = medico("9852", EstadoActivacion.ACTIVO, e);
        return turno(m, e, sala, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(10), hasta, estado);
    }

    /** Una reserva del turno, en una ocurrencia de esa fecha y horario, con el estado indicado. */
    private void reserva(TurnoSemanal turno, LocalDate fecha, String horaInicio, String horaFin, String estadoReserva) {
        String cedula = "98" + (++pacientes) + "p";
        Long usuario = jdbc.queryForObject("""
            insert into usuario(correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento)
            values (?, 'ACTIVO', CURRENT_TIMESTAMP, ?, 'Ana', 'López', '1990-01-01') returning id_usuario
            """, Long.class, cedula + "@ejemplo.com", cedula);
        jdbc.update("insert into paciente(id_usuario, fecha_alta) values (?, CURRENT_TIMESTAMP)", usuario);
        Long ocurrencia = jdbc.queryForObject("""
            insert into ocurrencia_turno(id_turno_semanal, fecha, cupos, hora_inicio, hora_fin, ocurrencia_turno_estado)
            values (?, ?, 6, ?, ?, 'HABILITADA') returning id_ocurrencia_turno
            """, Long.class, turno.getIdTurnoSemanal(), fecha, LocalTime.parse(horaInicio), LocalTime.parse(horaFin));
        jdbc.update("""
            insert into reserva(id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
            values (?, ?, 1, ?, CURRENT_TIMESTAMP)
            """, usuario, ocurrencia, estadoReserva);
    }

    @Test
    void modificaLosCuatroCamposYDevuelveElConsultorioActualizado() {
        ConsultorioResponseDTO respuesta = modificar("MC-SALA", datos("MC Sala grande", "MC Piso 3", "Sin ventana", EstadoActivacion.INACTIVO));

        assertEquals(new ConsultorioResponseDTO("MC-SALA", "MC Sala grande", "MC Piso 3", "Sin ventana", EstadoActivacion.INACTIVO), respuesta);
        Consultorio c = guardado("MC-SALA");
        assertEquals("MC Sala grande", c.getNombre());
        assertEquals("MC Piso 3", c.getUbicacion());
        assertEquals("Sin ventana", c.getDescripcion());
        assertEquals(EstadoActivacion.INACTIVO, c.getEstado());
    }

    @Test
    void registraElAdministradorYLaFechaDelCambio() {
        assertNull(sala.getAdministradorModificacion());
        assertNull(sala.getFechaModificacion());
        LocalDateTime antes = LocalDateTime.now().minusMinutes(1);

        modificar("MC-SALA", datos("MC Sala", "MC Planta baja", "Otra descripción", EstadoActivacion.ACTIVO));

        Consultorio c = guardado("MC-SALA");
        assertEquals(admin.getIdUsuario(), c.getAdministradorModificacion().getIdUsuario());
        assertTrue(c.getFechaModificacion().isAfter(antes));
    }

    @Test
    void sinCambiosNoRegistraNada() {
        modificar("MC-SALA", datos("MC Sala", "MC Planta baja", "Con ventana", EstadoActivacion.ACTIVO));

        Consultorio c = guardado("MC-SALA");
        assertNull(c.getAdministradorModificacion());
        assertNull(c.getFechaModificacion());
    }

    @Test
    void recortaLosEspaciosYUnaDescripcionVaciaLaBorra() {
        modificar("MC-SALA", datos("  MC Sala  ", "  MC Planta baja ", "   ", EstadoActivacion.ACTIVO));

        Consultorio c = guardado("MC-SALA");
        assertEquals("MC Sala", c.getNombre());
        assertEquals("MC Planta baja", c.getUbicacion());
        assertNull(c.getDescripcion());
    }

    @Test
    void dosConsultoriosPuedenCompartirNombre() {
        modificar("MC-SALA", datos("MC Libre", "MC Planta baja", "Con ventana", EstadoActivacion.ACTIVO));

        assertEquals("MC Libre", guardado("MC-SALA").getNombre());
    }

    // ------------------------------------------------------------------ inactivar

    @Test
    void noSePuedeInactivarConTurnosVigentes() {
        turnoEnLaSala(EstadoTurnoSemanal.ACTIVO, null);

        assertEquals(HttpStatus.CONFLICT, errorAl("MC-SALA", datos("MC Sala", "MC Planta baja", "Con ventana", EstadoActivacion.INACTIVO)));
        assertEquals(EstadoActivacion.ACTIVO, guardado("MC-SALA").getEstado());
    }

    @Test
    void unTurnoSuspendidoSinFinTambienImpideInactivar() {
        turnoEnLaSala(EstadoTurnoSemanal.SUSPENDIDO, null);

        assertEquals(HttpStatus.CONFLICT, errorAl("MC-SALA", datos("MC Sala", "MC Planta baja", "Con ventana", EstadoActivacion.INACTIVO)));
    }

    @Test
    void sePuedeInactivarSiSusTurnosYaTerminaron() {
        turnoEnLaSala(EstadoTurnoSemanal.ACTIVO, HOY.minusDays(1));

        modificar("MC-SALA", datos("MC Sala", "MC Planta baja", "Con ventana", EstadoActivacion.INACTIVO));

        assertEquals(EstadoActivacion.INACTIVO, guardado("MC-SALA").getEstado());
    }

    @Test
    void unConsultorioInactivoConTurnosPuedeEditarseSinReactivarse() {
        turnoEnLaSala(EstadoTurnoSemanal.ACTIVO, null);
        sala.setEstado(EstadoActivacion.INACTIVO);
        consultorios.saveAndFlush(sala);

        modificar("MC-SALA", datos("MC Sala renombrada", "MC Planta baja", "Con ventana", EstadoActivacion.INACTIVO));

        assertEquals("MC Sala renombrada", guardado("MC-SALA").getNombre());
    }

    @Test
    void sePuedeReactivarUnConsultorioInactivo() {
        sinTurnos.setEstado(EstadoActivacion.INACTIVO);
        consultorios.saveAndFlush(sinTurnos);

        modificar("MC-LIBRE", datos("MC Libre", "MC Piso 2", null, EstadoActivacion.ACTIVO));

        assertEquals(EstadoActivacion.ACTIVO, guardado("MC-LIBRE").getEstado());
    }

    // ------------------------------------------------------------------ ubicación

    @Test
    void noSePuedeCambiarLaUbicacionConReservasFuturas() {
        TurnoSemanal turno = turnoEnLaSala(EstadoTurnoSemanal.ACTIVO, null);
        reserva(turno, HOY.plusDays(3), "09:00", "12:00", "VIGENTE");

        assertEquals(HttpStatus.CONFLICT, errorAl("MC-SALA", datos("MC Sala", "MC Piso 3", "Con ventana", EstadoActivacion.ACTIVO)));
        assertEquals("MC Planta baja", guardado("MC-SALA").getUbicacion());
    }

    @Test
    void unaReservaConOrdenEmitidaTambienImpideCambiarLaUbicacion() {
        TurnoSemanal turno = turnoEnLaSala(EstadoTurnoSemanal.ACTIVO, null);
        reserva(turno, HOY.plusDays(1), "09:00", "12:00", "ORDEN_EMITIDA");

        assertEquals(HttpStatus.CONFLICT, errorAl("MC-SALA", datos("MC Sala", "MC Piso 3", "Con ventana", EstadoActivacion.ACTIVO)));
    }

    @Test
    void conReservasFuturasSiPuedeCambiarElRestoDeLosDatos() {
        TurnoSemanal turno = turnoEnLaSala(EstadoTurnoSemanal.ACTIVO, null);
        reserva(turno, HOY.plusDays(3), "09:00", "12:00", "VIGENTE");

        modificar("MC-SALA", datos("MC Sala nueva", "MC Planta baja", "Otra", EstadoActivacion.ACTIVO));

        assertEquals("MC Sala nueva", guardado("MC-SALA").getNombre());
    }

    @Test
    void lasReservasCanceladasAnuladasAtendidasOPasadasNoImpidenCambiarLaUbicacion() {
        TurnoSemanal turno = turnoEnLaSala(EstadoTurnoSemanal.ACTIVO, null);
        reserva(turno, HOY.plusDays(3), "09:00", "12:00", "CANCELADA");
        reserva(turno, HOY.plusDays(4), "09:00", "12:00", "ANULADA");
        reserva(turno, HOY.minusDays(3), "09:00", "12:00", "ATENDIDA");
        reserva(turno, HOY.minusDays(7), "09:00", "12:00", "VIGENTE");
        // Hoy, pero el turno ya terminó (de 0:00 a 0:01).
        reserva(turno, HOY, "00:00", "00:01", "VIGENTE");

        modificar("MC-SALA", datos("MC Sala", "MC Piso 3", "Con ventana", EstadoActivacion.ACTIVO));

        assertEquals("MC Piso 3", guardado("MC-SALA").getUbicacion());
    }

    @Test
    void sinTurnosNiReservasSePuedeCambiarLaUbicacion() {
        modificar("MC-LIBRE", datos("MC Libre", "MC Piso 5", null, EstadoActivacion.ACTIVO));

        assertEquals("MC Piso 5", guardado("MC-LIBRE").getUbicacion());
    }

    // ------------------------------------------------------------------ errores

    @Test
    void unConsultorioInexistenteResponde404() {
        assertEquals(HttpStatus.NOT_FOUND, errorAl("NO-EXISTE", datos("Nombre", "Ubicación", null, EstadoActivacion.ACTIVO)));
    }

    @Test
    void losCamposObligatoriosYLosLargosSeValidanEnElDTO() {
        Set<ConstraintViolation<ModificarConsultorioDTO>> faltan = validador.validate(new ModificarConsultorioDTO(" ", " ", null, null));
        assertEquals(3, faltan.size());

        Set<ConstraintViolation<ModificarConsultorioDTO>> largos = validador
            .validate(new ModificarConsultorioDTO("n".repeat(101), "u".repeat(201), "d".repeat(501), EstadoActivacion.ACTIVO));
        assertEquals(3, largos.size());

        assertTrue(validador.validate(datos("Nombre", "Ubicación", null, EstadoActivacion.ACTIVO)).isEmpty());
    }
}
