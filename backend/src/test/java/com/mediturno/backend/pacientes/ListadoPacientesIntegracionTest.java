package com.mediturno.backend.pacientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;

/** CU-33 Listado y búsqueda de pacientes, y su detalle, con una base PostgreSQL real. */
class ListadoPacientesIntegracionTest extends PostgresIntegracionTest {

    @Autowired PacienteService servicio;
    @Autowired PacienteRepository pacientes;

    private Paciente ana;      // sin cuenta, activa, la más antigua
    private Paciente bruno;    // con cuenta, activo, el más nuevo
    private Paciente carla;    // sin cuenta, inactiva

    private Paciente crear(String cedula, String nombre, String apellido, String correo, String uid, EstadoActivacion estado, int diasDeAlta) {
        Paciente p = new Paciente();
        p.setCedula(cedula);
        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setCorreo(correo);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 17));
        p.setTelefono("099 123 456");
        p.setDomicilio("Calle 1");
        p.setEstado(estado);
        p.setFirebaseUid(uid);
        p.setFechaCreacion(LocalDateTime.now());
        p.setFechaAlta(LocalDateTime.now().minusDays(diasDeAlta));
        return pacientes.saveAndFlush(p);
    }

    @BeforeEach
    void datosBase() {
        // Los apellidos llevan «Zq» para que las búsquedas no se mezclen con otros datos de la base.
        ana = crear("5.123.456-7", "Ana", "Zqrojas", "zq.ana@ejemplo.com", null, EstadoActivacion.ACTIVO, 3);
        bruno = crear("5.123.457-8", "Bruno", "Zqaguirre", "zq.bruno@ejemplo.com", "uid-zq-bruno", EstadoActivacion.ACTIVO, 1);
        carla = crear("5.987.654-3", "Carla", "Zqmendez", "carla@zqcorreo.com", null, EstadoActivacion.INACTIVO, 2);
    }

    private CriteriosBusquedaDTO criterios(String texto, Boolean tieneCuenta, String estado, String ordenarPor, Boolean descendente) {
        return new CriteriosBusquedaDTO(null, null, null, null, estado, ordenarPor, descendente, null, null, texto, null, null, tieneCuenta);
    }

    private List<String> nombres(CriteriosBusquedaDTO criterios) {
        return servicio.listarPacientes(criterios).stream().map(PacienteResponseDTO::nombre).toList();
    }

    private List<String> nombres(String texto) {
        return nombres(criterios(texto, null, null, null, null));
    }

    // ------------------------------------------------------------------ búsqueda

    @Test
    void sinCriteriosDevuelveTodoElPadronPorApellido() {
        List<PacienteResponseDTO> todos = servicio.listarPacientes(CriteriosBusquedaDTO.vacio());

        assertTrue(todos.size() >= 3);
        List<String> apellidos = todos.stream().map(PacienteResponseDTO::apellido).toList();
        assertEquals(apellidos.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), apellidos);
    }

    @Test
    void buscaPorApellidoSinDistinguirMayusculas() {
        assertEquals(List.of("Bruno", "Carla", "Ana"), nombres("ZQ"));
        assertEquals(List.of("Ana"), nombres("zqROJAS"));
    }

    @Test
    void buscaPorNombre() {
        assertEquals(List.of("Bruno"), nombres("bruno"));
    }

    @Test
    void buscaPorCorreo() {
        assertEquals(List.of("Carla"), nombres("carla@zqcorreo"));
    }

    @Test
    void buscaPorCedulaConPuntosYGuion() {
        assertEquals(List.of("Ana"), nombres("5.123.456-7"));
        assertEquals(List.of("Bruno", "Ana"), nombres("5.123"));
    }

    @Test
    void buscaPorCedulaSinPuntosNiGuion() {
        assertEquals(List.of("Ana"), nombres("51234567"));
        assertEquals(List.of("Ana"), nombres("5123456"));
        assertEquals(List.of("Carla"), nombres("598765"));
    }

    @Test
    void buscaPorApellidoYNombreJuntos() {
        assertEquals(List.of("Ana"), nombres("Zqrojas Ana"));
        assertEquals(List.of("Ana"), nombres("ana zqrojas"));
        assertEquals(List.of("Ana"), nombres("  Zqrojas Ana "));
    }

    @Test
    void losComodinesSeBuscanComoTexto() {
        assertEquals(List.of(), nombres("%"));
        assertEquals(List.of(), nombres("zq_"));
    }

    @Test
    void sinCoincidenciasDevuelveUnaListaVacia() {
        assertEquals(List.of(), nombres("zqinexistente"));
    }

    // ------------------------------------------------------------------ filtros

    @Test
    void filtraPorSiTieneCuenta() {
        assertEquals(List.of("Bruno"), nombres(criterios("zq", true, null, null, null)));
        assertEquals(List.of("Carla", "Ana"), nombres(criterios("zq", false, null, null, null)));
    }

    @Test
    void filtraPorEstado() {
        assertEquals(List.of("Carla"), nombres(criterios("zq", null, "INACTIVO", null, null)));
        assertEquals(List.of("Bruno", "Ana"), nombres(criterios("zq", null, "activo", null, null)));
    }

    @Test
    void combinaLosCriterios() {
        assertEquals(List.of("Ana"), nombres(criterios("zq", false, "ACTIVO", null, null)));
        assertEquals(List.of(), nombres(criterios("zq", true, "INACTIVO", null, null)));
        assertEquals(List.of("Bruno"), nombres(criterios("bruno", true, "ACTIVO", null, null)));
    }

    @Test
    void unEstadoInvalidoResponde400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.listarPacientes(criterios("zq", null, "DORMIDO", null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    // ------------------------------------------------------------------ orden

    @Test
    void ordenaPorNombre() {
        assertEquals(List.of("Ana", "Bruno", "Carla"), nombres(criterios("zq", null, null, "nombre", null)));
        assertEquals(List.of("Carla", "Bruno", "Ana"), nombres(criterios("zq", null, null, "nombre", true)));
    }

    @Test
    void ordenaPorApellidoEnAmbosSentidos() {
        assertEquals(List.of("Bruno", "Carla", "Ana"), nombres(criterios("zq", null, null, "apellido", false)));
        assertEquals(List.of("Ana", "Carla", "Bruno"), nombres(criterios("zq", null, null, "apellido", true)));
    }

    @Test
    void ordenaPorFechaDeAlta() {
        assertEquals(List.of("Ana", "Carla", "Bruno"), nombres(criterios("zq", null, null, "fechaAlta", false)));
        assertEquals(List.of("Bruno", "Carla", "Ana"), nombres(criterios("zq", null, null, "fechaalta", true)));
    }

    @Test
    void unOrdenInvalidoResponde400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.listarPacientes(criterios("zq", null, null, "correo", null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    // ------------------------------------------------------------------ datos del resultado y detalle

    @Test
    void cadaResultadoTraeLosDatosDelPadron() {
        PacienteResponseDTO r = servicio.listarPacientes(criterios("zqaguirre", null, null, null, null)).get(0);

        assertEquals(bruno.getIdUsuario(), r.idPaciente());
        assertEquals("5.123.457-8", r.cedula());
        assertEquals("Bruno", r.nombre());
        assertEquals("Zqaguirre", r.apellido());
        assertEquals("zq.bruno@ejemplo.com", r.correo());
        assertEquals("099 123 456", r.telefono());
        assertEquals(EstadoActivacion.ACTIVO, r.estado());
        assertTrue(r.tieneCuenta());
        assertFalse(servicio.listarPacientes(criterios("zqrojas", null, null, null, null)).get(0).tieneCuenta());
    }

    @Test
    void elDetalleTraeLosDatosAdministrativosDelPaciente() {
        PacienteResponseDTO r = servicio.obtenerPaciente(ana.getIdUsuario());

        assertEquals(ana.getIdUsuario(), r.idPaciente());
        assertEquals("5.123.456-7", r.cedula());
        assertEquals("Ana", r.nombre());
        assertEquals("Zqrojas", r.apellido());
        assertEquals("zq.ana@ejemplo.com", r.correo());
        assertEquals(LocalDate.of(1990, 5, 17), r.fechaNacimiento());
        assertEquals("099 123 456", r.telefono());
        assertEquals("Calle 1", r.domicilio());
        assertFalse(r.tieneCuenta());
        assertEquals(EstadoActivacion.ACTIVO, r.estado());
    }

    @Test
    void elDetalleDeUnPacienteInexistenteResponde404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> servicio.obtenerPaciente(Long.MAX_VALUE));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void elDetalleDeUnaPersonaQueNoEsPacienteResponde404() {
        Long idFuncionario = funcionario("9881").getIdUsuario();

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> servicio.obtenerPaciente(idFuncionario));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
