package com.mediturno.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Comprueba que los datos de prueba (db/datos-prueba, perfil dev) se cargan sobre una base real sin violar ninguna
 * restricción, que tienen el contenido esperado, que son coherentes con las reglas de la aplicación (estados, pases, horarios) y
 * que volver a ejecutar el script no duplica nada.
 */
@SpringBootTest
@ActiveProfiles("dev")
class DatosDePruebaTest {

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

    /** Hoy y ahora en Montevideo, como el script y el reloj de la aplicación (la base puede estar en otra zona horaria). */
    private static final String HOY = "(now() AT TIME ZONE 'America/Montevideo')::date";
    private static final String AHORA = "(now() AT TIME ZONE 'America/Montevideo')";
    private static final String RESERVAS = "reserva r JOIN ocurrencia_turno o ON o.id_ocurrencia_turno = r.id_ocurrencia_turno "
        + "JOIN turno_semanal t ON t.id_turno_semanal = o.id_turno_semanal";
    private static final String RESERVAS_CON_PERSONAS = RESERVAS + " JOIN usuario m ON m.id_usuario = t.id_medico "
        + "JOIN usuario p ON p.id_usuario = r.id_paciente";

    @Autowired JdbcTemplate jdbc;

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    private int contar(String tabla, String condicion) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE " + condicion, Integer.class);
    }

    @Test
    void cargaLosDatosBasicosDeCadaRol() {
        assertEquals(1, contar("administrador"));
        assertEquals(2, contar("funcionario"));
        assertEquals(6, contar("medico"));
        assertEquals(15, contar("paciente"));
        assertEquals(24, contar("usuario"));
        assertEquals(4, contar("usuario", "firebase_uid IS NOT NULL"), "solo los cuatro usuarios de prueba con Firebase tienen cuenta");
        assertEquals(1, contar("medico m JOIN usuario u ON u.id_usuario = m.id_usuario", "u.estado = 'INACTIVO'"));
        assertEquals(1, contar("paciente p JOIN usuario u ON u.id_usuario = p.id_usuario", "u.estado = 'INACTIVO'"));
    }

    @Test
    void cargaEspecialidadesConsultoriosYUnaDeCadaCosaInactiva() {
        assertEquals(6, contar("especialidad"));
        assertEquals(2, contar("especialidad", "requiere_pase"));
        assertEquals(1, contar("especialidad", "estado = 'INACTIVO'"));
        assertEquals(5, contar("consultorio"));
        assertEquals(1, contar("consultorio", "estado = 'INACTIVO'"));
        assertEquals(7, contar("medico_especialidad"));
    }

    @Test
    void cargaTurnosOcurrenciasYSuspensiones() {
        assertEquals(11, contar("turno_semanal"));
        assertEquals(2, contar("turno_semanal", "estado = 'SUSPENDIDO'"));
        assertEquals(1, contar("turno_semanal", "vigente_hasta IS NOT NULL"));
        assertEquals(1, contar("suspension_turno", "suspension_tipo = 'TEMPORAL'"));
        assertEquals(1, contar("suspension_turno", "suspension_tipo = 'PERMANENTE'"));
        for (String estado : new String[] {"HABILITADA", "SUSPENDIDA", "FINALIZADA"}) {
            assertTrue(contar("ocurrencia_turno", "ocurrencia_turno_estado = '" + estado + "'") >= 1, "falta una ocurrencia " + estado);
        }
        assertEquals(0, contar("ocurrencia_turno", "fecha > " + HOY + " + 56"), "no hay fechas más allá de la ventana de reserva");
        assertEquals(3, contar("ocurrencia_turno o JOIN turno_semanal t ON t.id_turno_semanal = o.id_turno_semanal",
            "o.fecha = " + HOY + " AND t.vigente_desde = " + HOY + " - 28"), "hay tres turnos hoy");
    }

    @Test
    void ningunMedicoNiConsultorioTieneDosTurnosSuperpuestos() {
        Integer superpuestos = jdbc.queryForObject("""
            SELECT count(*) FROM turno_semanal a
            JOIN turno_semanal b ON a.id_turno_semanal < b.id_turno_semanal AND a.dia = b.dia
             AND a.hora_inicio < b.hora_fin AND b.hora_inicio < a.hora_fin
             AND (a.id_medico = b.id_medico OR a.codigo_consultorio = b.codigo_consultorio)
            """, Integer.class);
        assertEquals(0, superpuestos);
    }

    @Test
    void lasReservasSonCoherentesConSuEstadoYSuFecha() {
        for (String estado : new String[] {"VIGENTE", "ORDEN_EMITIDA", "ATENDIDA", "CANCELADA", "ANULADA", "VENCIDA"}) {
            assertTrue(contar("reserva", "reserva_estado = '" + estado + "'") >= 1, "falta una reserva " + estado);
        }
        assertTrue(contar("reserva") >= 150, "hay volumen para los listados y las estadísticas");
        assertEquals(contar("reserva", "reserva_estado = 'ATENDIDA'"), contar("consulta"), "cada atención tiene su consulta");
        assertEquals(contar("consulta"), contar("orden", "orden_estado = 'UTILIZADA'"));
        assertEquals(contar("reserva", "reserva_estado = 'ORDEN_EMITIDA'"), contar("orden", "orden_estado = 'EMITIDA'"));

        assertEquals(0, contar(RESERVAS, "o.fecha < " + HOY + " AND r.reserva_estado IN ('VIGENTE', 'ORDEN_EMITIDA')"),
            "las fechas pasadas ya no tienen reservas pendientes");
        assertEquals(0, contar(RESERVAS, "o.fecha >= " + HOY + " AND r.reserva_estado IN ('ATENDIDA', 'VENCIDA')"));
        assertEquals(0, contar(RESERVAS + " JOIN especialidad e ON e.codigo = t.codigo_especialidad", "e.requiere_pase AND r.id_pase IS NULL"),
            "las especialidades con pase se reservaron con uno");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM (SELECT 1 FROM reserva GROUP BY id_ocurrencia_turno, id_paciente "
            + "HAVING count(*) > 1) AS x", Integer.class), "nadie tiene dos reservas en la misma fecha de un turno");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM (SELECT 1 FROM historia_clinica GROUP BY id_paciente "
            + "HAVING count(*) > 1) AS x", Integer.class), "una historia clínica por paciente");
    }

    @Test
    void laMedicaDePruebaTieneHoyPacientesParaAtender() {
        String deLaura = "m.correo = 'medico@mediturno.com' AND o.fecha = " + HOY + " AND r.reserva_estado = 'ORDEN_EMITIDA' AND ";
        assertEquals(1, contar(RESERVAS_CON_PERSONAS, deLaura + "p.correo = 'paciente@mediturno.com'"), "Ana, con antecedentes");
        assertEquals(1, contar(RESERVAS_CON_PERSONAS, deLaura
            + "NOT EXISTS (SELECT 1 FROM historia_clinica h WHERE h.id_paciente = r.id_paciente)"), "un paciente sin historia clínica");
        assertTrue(contar("consulta c JOIN usuario m ON m.id_usuario = c.id_medico", "m.correo = 'medico@mediturno.com'") >= 10,
            "la médica tiene historial de atenciones propias (CU-26)");
    }

    @Test
    void aCualquierHoraDeHoyHayUnaReservaALaQueSeLePuedeEmitirLaOrden() {
        // De 7 a 21: cada hora cae en la ventana de emisión (desde una hora antes del inicio hasta el fin) de una reserva vigente.
        Integer horasSinReserva = jdbc.queryForObject("""
            SELECT count(*) FROM generate_series(7, 20) AS h
            WHERE NOT EXISTS (
                SELECT 1 FROM reserva r JOIN ocurrencia_turno o ON o.id_ocurrencia_turno = r.id_ocurrencia_turno
                WHERE o.fecha = %s AND r.reserva_estado = 'VIGENTE'
                  AND make_time(h, 0, 0) >= o.hora_inicio - INTERVAL '1 hour' AND make_time(h, 59, 0) <= o.hora_fin)
            """.formatted(HOY), Integer.class);
        assertEquals(0, horasSinReserva);
    }

    @Test
    void laPacienteDePruebaTieneUnCasoParaCadaPrueba() {
        String ana = "p.correo = 'paciente@mediturno.com' AND ";
        assertTrue(contar(RESERVAS_CON_PERSONAS, ana + "r.reserva_estado = 'VIGENTE' AND o.fecha + o.hora_inicio >= "
            + AHORA + " + INTERVAL '48 hours'") >= 1, "una reserva que se puede cancelar");
        assertEquals(1, contar(RESERVAS_CON_PERSONAS, ana + "r.reserva_estado = 'VIGENTE' AND o.fecha = " + HOY + " + 1"),
            "una reserva de mañana, que no se puede cancelar");
        assertEquals(6, jdbc.queryForObject("SELECT count(DISTINCT r.reserva_estado) FROM " + RESERVAS_CON_PERSONAS
            + " WHERE p.correo = 'paciente@mediturno.com'", Integer.class), "reservas en todos los estados (CU-10)");

        String consultasDeAna = "consulta c JOIN usuario p ON p.id_usuario = c.id_paciente JOIN reserva r ON r.id_reserva = c.id_reserva "
            + "JOIN ocurrencia_turno o ON o.id_ocurrencia_turno = r.id_ocurrencia_turno "
            + "JOIN turno_semanal t ON t.id_turno_semanal = o.id_turno_semanal WHERE p.correo = 'paciente@mediturno.com'";
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM " + consultasDeAna, Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT count(DISTINCT c.id_medico) FROM " + consultasDeAna, Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT count(DISTINCT t.codigo_especialidad) FROM " + consultasDeAna, Integer.class));

        String pasesDeAna = "pase pa JOIN usuario p ON p.id_usuario = pa.id_paciente";
        String valido = "pa.pase_estado = 'VIGENTE' AND pa.fecha_emision >= " + AHORA + " - INTERVAL '60 days'";
        assertEquals(1, contar(pasesDeAna, ana + "pa.especialidad_destino = 'TRAU' AND " + valido));
        assertEquals(0, contar(pasesDeAna, ana + "pa.especialidad_destino = 'DERM' AND " + valido), "no puede reservar Dermatología");
        assertTrue(contar("ocurrencia_turno o JOIN turno_semanal t ON t.id_turno_semanal = o.id_turno_semanal",
            "t.codigo_especialidad = 'DERM' AND o.ocurrencia_turno_estado = 'HABILITADA'") >= 1, "aunque tiene fechas");

        assertTrue(contar("notificacion n JOIN usuario p ON p.id_usuario = n.id_usuario",
            ana + "n.canal = 'APLICACION' AND n.estado = 'ENVIADA'") >= 1, "avisos sin leer en la campanita");
    }

    @Test
    void hayUnaFechaSinCuposYReservasFuturasRepartidas() {
        Integer sinCupos = jdbc.queryForObject("""
            SELECT count(*) FROM ocurrencia_turno o
            WHERE o.fecha > %s AND o.cupos = (SELECT count(*) FROM reserva r WHERE r.id_ocurrencia_turno = o.id_ocurrencia_turno
                                              AND r.reserva_estado NOT IN ('CANCELADA', 'ANULADA'))
            """.formatted(HOY), Integer.class);
        assertTrue(sinCupos >= 1);
        assertTrue(contar(RESERVAS, "o.fecha >= " + HOY + " + 2 AND r.reserva_estado = 'VIGENTE'") >= 30);
    }

    @Test
    void cargaCarteleraYNotificaciones() {
        assertEquals(5, contar("anuncio"));
        assertEquals(4, contar("anuncio", "anuncio_estado = 'PUBLICADO'"));
        assertEquals(2, contar("anuncio", "anuncio_estado = 'PUBLICADO' AND fecha_inicio <= " + HOY + " AND fecha_fin >= " + HOY),
            "dos anuncios vigentes; los otros están programados, vencidos o eliminados");
        assertEquals(11, contar("notificacion"));
        for (String tipo : new String[] {"RESERVA_CONFIRMADA", "SUSPENSION_TURNO", "CONSULTA_REGISTRADA"}) {
            assertTrue(contar("notificacion", "tipo = '" + tipo + "'") >= 1, "falta un aviso " + tipo);
        }
        assertEquals(1, contar("notificacion", "estado = 'FALLIDA'"));
    }

    @Test
    void volverAEjecutarElScriptNoDuplicaNada() throws IOException {
        int usuariosAntes = contar("usuario");
        int reservasAntes = contar("reserva");
        String script = new ClassPathResource("db/datos-prueba/R__zz_datos_de_prueba.sql").getContentAsString(StandardCharsets.UTF_8);

        jdbc.execute(script);

        assertEquals(usuariosAntes, contar("usuario"));
        assertEquals(reservasAntes, contar("reserva"));
    }
}
