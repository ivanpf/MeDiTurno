package com.mediturno.backend.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO.RolUsuarioInterno;
import com.mediturno.backend.usuarios.dto.UsuarioResponseDTO;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;

/** CU-36 Listado y búsqueda de usuarios internos, y su detalle, con una base PostgreSQL real. */
class ListadoUsuariosInternosIntegracionTest extends BaseUsuariosInternos {

    @Autowired UsuarioInternoService servicio;

    private Administrador adminZk;   // activo, el más antiguo
    private Funcionario funcionarioZk; // activo
    private Medico medicoZk;         // activo, Zk1 y Zk2
    private Medico medicoInactivoZk; // inactivo, Zk2
    private Especialidad zk1;
    private Especialidad zk2;

    @BeforeEach
    void datosBase() {
        // Los apellidos llevan «Zk» para que las búsquedas no se mezclen con otros datos de la base.
        zk1 = especialidad("ZK1");
        zk2 = especialidad("ZK2");
        LocalDateTime ahora = LocalDateTime.now();
        adminZk = administrador("5.610.001-1", "Ana", "Zkcastro", "zk.ana@ejemplo.com", EstadoActivacion.ACTIVO);
        adminZk.setFechaCreacion(ahora.minusDays(5));
        funcionarioZk = funcionarioInterno("5.610.002-2", "Bruno", "Zkbenitez", "zk.bruno@ejemplo.com", EstadoActivacion.ACTIVO, ahora.minusDays(3));
        medicoZk = medicoInterno("5.610.003-3", "Carla", "Zkaguirre", "zk.carla@ejemplo.com", "ZK-001", EstadoActivacion.ACTIVO, ahora.minusDays(1), zk1, zk2);
        medicoInactivoZk = medicoInterno("5.610.004-4", "Diego", "Zkdominguez", "diego@zkcorreo.com", "ZK-002", EstadoActivacion.INACTIVO, ahora.minusDays(2), zk2);
        usuarios.flush();
    }

    private CriteriosBusquedaDTO criterios(String texto, String rol, String estado, String codigoEspecialidad, String ordenarPor, Boolean descendente) {
        return new CriteriosBusquedaDTO(null, codigoEspecialidad, null, null, estado, ordenarPor, descendente, null, null, texto, null, null, null, rol);
    }

    private List<String> nombres(CriteriosBusquedaDTO criterios) {
        return servicio.listarUsuariosInternos(criterios).stream().map(UsuarioResponseDTO::nombre).toList();
    }

    private List<String> nombres(String texto) {
        return nombres(criterios(texto, null, null, null, null, null));
    }

    private HttpStatus errorAl(CriteriosBusquedaDTO criterios) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, () -> servicio.listarUsuariosInternos(criterios)).getStatusCode();
    }

    // ------------------------------------------------------------------ búsqueda

    @Test
    void sinCriteriosDevuelveLosTresRolesYNuncaLosPacientes() {
        Paciente paciente = pacienteInterno("5.610.099-9", "Paula", "Zkpaciente", "zk.paciente@ejemplo.com");

        List<UsuarioResponseDTO> todos = servicio.listarUsuariosInternos(CriteriosBusquedaDTO.vacio());

        assertTrue(todos.stream().noneMatch(u -> u.idUsuario().equals(paciente.getIdUsuario())));
        assertEquals(3, todos.stream().map(UsuarioResponseDTO::rol).distinct().count());
        assertTrue(todos.stream().map(UsuarioResponseDTO::idUsuario).toList().containsAll(
            List.of(adminZk.getIdUsuario(), funcionarioZk.getIdUsuario(), medicoZk.getIdUsuario(), medicoInactivoZk.getIdUsuario())));
    }

    @Test
    void buscaPorNombreApellidoCorreoYCedula() {
        assertEquals(List.of("Ana"), nombres("ana"));
        assertEquals(List.of("Bruno"), nombres("ZKBENITEZ"));
        assertEquals(List.of("Diego"), nombres("zkcorreo"));
        assertEquals(List.of("Carla"), nombres("5.610.003-3"));
        assertEquals(List.of("Carla"), nombres("5610003")); // la cédula también se encuentra sin puntos ni guion
    }

    @Test
    void buscaTambienPorApellidoYNombreJuntos() {
        assertEquals(List.of("Ana"), nombres("zkcastro ana"));
        assertEquals(List.of("Ana"), nombres("ana zkcastro"));
    }

    @Test
    void elTextoConComodinesSeBuscaComoTexto() {
        assertEquals(List.of(), nombres("zk%"));
        assertEquals(List.of(), nombres("zk_"));
    }

    @Test
    void filtraPorRol() {
        assertEquals(List.of("Ana"), nombres(criterios("zk", "ADMINISTRADOR", null, null, null, null)));
        assertEquals(List.of("Bruno"), nombres(criterios("zk", "funcionario", null, null, null, null)));
        assertEquals(List.of("Carla", "Diego"), nombres(criterios("zk", "MEDICO", null, null, "apellido", null)));
    }

    @Test
    void filtraPorEstado() {
        assertEquals(List.of("Diego"), nombres(criterios("zk", null, "INACTIVO", null, null, null)));
        assertEquals(List.of("Carla", "Bruno", "Ana"), nombres(criterios("zk", null, "ACTIVO", null, null, null)));
    }

    @Test
    void filtraALosMedicosPorEspecialidad() {
        assertEquals(List.of("Carla"), nombres(criterios(null, null, null, "ZK1", null, null)));
        assertEquals(List.of("Carla", "Diego"), nombres(criterios(null, null, null, "ZK2", "apellido", null)));
    }

    @Test
    void combinaLosFiltros() {
        assertEquals(List.of("Diego"), nombres(criterios("zk", "MEDICO", "INACTIVO", "ZK2", null, null)));
        assertEquals(List.of(), nombres(criterios("zk", "FUNCIONARIO", "INACTIVO", null, null, null)));
    }

    // ------------------------------------------------------------------ orden

    @Test
    void ordenaPorApellidoPorDefectoYEnSentidoInverso() {
        // Zkaguirre, Zkbenitez, Zkcastro, Zkdominguez
        assertEquals(List.of("Carla", "Bruno", "Ana", "Diego"), nombres(criterios("zk", null, null, null, null, null)));
        assertEquals(List.of("Diego", "Ana", "Bruno", "Carla"), nombres(criterios("zk", null, null, null, "apellido", true)));
    }

    @Test
    void ordenaPorNombreRolYFechaDeCreacion() {
        assertEquals(List.of("Ana", "Bruno", "Carla", "Diego"), nombres(criterios("zk", null, null, null, "nombre", null)));
        assertEquals(List.of("Ana", "Bruno", "Carla", "Diego"), nombres(criterios("zk", null, null, null, "rol", null))); // admin, funcionario, médicos
        assertEquals(List.of("Ana", "Bruno", "Diego", "Carla"), nombres(criterios("zk", null, null, null, "fechaCreacion", null)));
        assertEquals(List.of("Carla", "Diego", "Bruno", "Ana"), nombres(criterios("zk", null, null, null, "FECHACREACION", true)));
    }

    @Test
    void unCriterioInvalidoEsUnError400() {
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(criterios(null, "PACIENTE", null, null, null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(criterios(null, null, "BORRADO", null, null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(criterios(null, null, null, null, "cedula", null)));
    }

    // ------------------------------------------------------------------ detalle

    @Test
    void elListadoYElDetalleDelMedicoTraenMatriculaYEspecialidades() {
        UsuarioResponseDTO d = servicio.obtenerUsuarioInterno(medicoZk.getIdUsuario());

        assertEquals(RolUsuarioInterno.MEDICO, d.rol());
        assertEquals("ZK-001", d.matricula());
        assertEquals("5.610.003-3", d.cedula());
        assertEquals(List.of("ZK1", "ZK2"), d.especialidades().stream().map(UsuarioResponseDTO.EspecialidadAsignada::codigo).toList());
        assertEquals(EstadoActivacion.ACTIVO, d.especialidades().get(0).estado());
        UsuarioResponseDTO enListado = servicio.listarUsuariosInternos(criterios("zkaguirre", null, null, null, null, null)).get(0);
        assertEquals("ZK-001", enListado.matricula());
        assertEquals(2, enListado.especialidades().size());
    }

    @Test
    void elDetalleDeUnAdministradorOFuncionarioNoTrajoMatriculaNiEspecialidades() {
        UsuarioResponseDTO d = servicio.obtenerUsuarioInterno(funcionarioZk.getIdUsuario());

        assertEquals(RolUsuarioInterno.FUNCIONARIO, d.rol());
        assertNull(d.matricula());
        assertEquals(List.of(), d.especialidades());
        assertEquals(RolUsuarioInterno.ADMINISTRADOR, servicio.obtenerUsuarioInterno(adminZk.getIdUsuario()).rol());
    }

    @Test
    void elDetalleDeUnPacienteOUnIdInexistenteEsUn404() {
        Paciente paciente = pacienteInterno("5.610.098-8", "Paula", "Zkpaciente", "zk.paciente@ejemplo.com");

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> servicio.obtenerUsuarioInterno(paciente.getIdUsuario())).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> servicio.obtenerUsuarioInterno(-1L)).getStatusCode());
    }
}
