package com.mediturno.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.repository.MedicoRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Prueba la base de datos con un PostgreSQL real que se levanta solo para el test (no hace falta Podman):
 * Flyway ejecuta las migraciones, Hibernate valida que cada entidad coincida con su tabla y se comprueban
 * las restricciones más importantes del modelo.
 */
@SpringBootTest
class EsquemaBaseDeDatosTest {

    static final EmbeddedPostgres POSTGRES = iniciarPostgres();

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void detenerPostgres() throws IOException {
        POSTGRES.close();
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EspecialidadRepository especialidades;
    @Autowired ConsultorioRepository consultorios;
    @Autowired MedicoRepository medicos;
    @Autowired PacienteRepository pacientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired TurnoSemanalRepository turnos;
    @Autowired OcurrenciaTurnoRepository ocurrencias;
    @Autowired ReservaRepository reservas;

    // ------------------------------------------------------------------ datos de prueba

    private Especialidad especialidad(String codigo) {
        Especialidad e = new Especialidad();
        e.setCodigo(codigo);
        e.setNombre("Especialidad " + codigo);
        e.setRequierePase(false);
        e.setEstado(EstadoActivacion.ACTIVO);
        return especialidades.save(e);
    }

    private Consultorio consultorio(String codigo) {
        Consultorio c = new Consultorio();
        c.setCodigo(codigo);
        c.setNombre("Consultorio " + codigo);
        c.setUbicacion("Piso 1");
        c.setEstado(EstadoActivacion.ACTIVO);
        return consultorios.save(c);
    }

    private void datosDePersona(Usuario u, String id) {
        u.setCorreo(id + "@ejemplo.com");
        u.setEstado(EstadoActivacion.ACTIVO);
        u.setFechaCreacion(LocalDateTime.now());
        u.setCedula(id);
        u.setNombre("Nombre " + id);
        u.setApellido("Apellido " + id);
        u.setFechaNacimiento(LocalDate.of(1990, 1, 1));
    }

    private Medico medico(String id, Especialidad... especialidadesDelMedico) {
        Medico m = new Medico();
        datosDePersona(m, id);
        m.setMatricula("MAT-" + id);
        for (Especialidad e : especialidadesDelMedico) {
            m.getEspecialidades().add(e);
        }
        return medicos.save(m);
    }

    private Paciente paciente(String id) {
        Paciente p = new Paciente();
        datosDePersona(p, id);
        p.setFechaAlta(LocalDateTime.now());
        return pacientes.save(p);
    }

    private TurnoSemanal turno(String id, LocalTime inicio, LocalTime fin, int cupoMaximo) {
        Especialidad esp = especialidad("E" + id);
        Consultorio cons = consultorio("C" + id);
        Medico med = medico("M" + id, esp);
        TurnoSemanal t = new TurnoSemanal();
        t.setMedico(med);
        t.setEspecialidad(esp);
        t.setConsultorio(cons);
        t.setDia(DiaSemana.LUNES);
        t.setHoraInicio(inicio);
        t.setHoraFin(fin);
        t.setCupoMaximo(cupoMaximo);
        t.setVigenteDesde(LocalDate.of(2026, 1, 1));
        t.setEstado(EstadoTurnoSemanal.ACTIVO);
        return turnos.saveAndFlush(t);
    }

    private OcurrenciaTurno ocurrencia(TurnoSemanal turno, LocalDate fecha) {
        OcurrenciaTurno o = new OcurrenciaTurno();
        o.setTurnoSemanal(turno);
        o.setFecha(fecha);
        o.setCupos(turno.getCupoMaximo());
        o.setHoraInicio(turno.getHoraInicio());
        o.setHoraFin(turno.getHoraFin());
        o.setOcurrenciaTurnoEstado(EstadoOcurrenciaTurno.HABILITADA);
        return ocurrencias.saveAndFlush(o);
    }

    private Reserva reserva(Paciente paciente, OcurrenciaTurno ocurrencia, int numero, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setPaciente(paciente);
        r.setOcurrenciaTurno(ocurrencia);
        r.setNumero(numero);
        r.setReservaEstado(estado);
        r.setFechaReserva(LocalDateTime.now());
        return reservas.saveAndFlush(r);
    }

    // ------------------------------------------------------------------ pruebas

    @Test
    void flywayAplicoLaMigracionInicialYHibernateValidoLasEntidades() {
        // Si el contexto de Spring arrancó, Hibernate (ddl-auto: validate) encontró todas las tablas y columnas.
        Integer aplicadas = jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertEquals(1, aplicadas);
    }

    @Test
    @Transactional
    void cadaUsuarioSeCargaComoSuTipoYElMedicoGuardaSusEspecialidades() {
        Especialidad cardiologia = especialidad("CARDIO-H");
        Medico guardado = medico("HERENCIA1", cardiologia);
        Paciente paciente = paciente("HERENCIA2");

        Usuario comoMedico = usuarios.findById(guardado.getIdUsuario()).orElseThrow();
        Usuario comoPaciente = usuarios.findById(paciente.getIdUsuario()).orElseThrow();

        assertInstanceOf(Medico.class, comoMedico);
        assertInstanceOf(Paciente.class, comoPaciente);
        assertEquals(1, ((Medico) comoMedico).getEspecialidades().size());
    }

    @Test
    void noPermiteDosReservasQueConservenElMismoNumeroEnUnaOcurrencia() {
        TurnoSemanal turno = turno("R1", LocalTime.of(9, 0), LocalTime.of(12, 0), 9);
        OcurrenciaTurno ocurrencia = ocurrencia(turno, LocalDate.of(2026, 11, 2));
        reserva(paciente("RES-A"), ocurrencia, 5, EstadoReserva.VIGENTE);

        Paciente otro = paciente("RES-B");
        assertThrows(DataIntegrityViolationException.class,
            () -> reserva(otro, ocurrencia, 5, EstadoReserva.VIGENTE));
    }

    @Test
    void cancelarUnaReservaLiberaSuNumero() {
        TurnoSemanal turno = turno("R2", LocalTime.of(9, 0), LocalTime.of(12, 0), 9);
        OcurrenciaTurno ocurrencia = ocurrencia(turno, LocalDate.of(2026, 11, 9));
        reserva(paciente("CAN-A"), ocurrencia, 3, EstadoReserva.CANCELADA);

        Reserva nueva = reserva(paciente("CAN-B"), ocurrencia, 3, EstadoReserva.VIGENTE);

        assertTrue(nueva.getIdReserva() > 0);
    }

    @Test
    void elHorarioDeUnTurnoDebeTenerFinPosteriorAlInicio() {
        assertThrows(DataIntegrityViolationException.class,
            () -> turno("H1", LocalTime.of(12, 0), LocalTime.of(9, 0), 5));
    }

    @Test
    void noPermiteDosEspecialidadesConElMismoNombre() {
        especialidad("DUP-1");
        Especialidad repetida = new Especialidad();
        repetida.setCodigo("DUP-2");
        repetida.setNombre("Especialidad DUP-1");
        repetida.setEstado(EstadoActivacion.ACTIVO);

        assertThrows(DataIntegrityViolationException.class, () -> especialidades.saveAndFlush(repetida));
    }
}
