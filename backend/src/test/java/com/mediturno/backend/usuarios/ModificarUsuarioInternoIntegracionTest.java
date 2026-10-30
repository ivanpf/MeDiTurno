package com.mediturno.backend.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.CorreoYaRegistradoEnProveedorException;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.common.seguridad.ProveedorIdentidadException;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO.RolUsuarioInterno;
import com.mediturno.backend.usuarios.dto.ModificarUsuarioDTO;
import com.mediturno.backend.usuarios.dto.UsuarioResponseDTO;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.UsuarioInterno;

/** CU-45 Modificación de usuarios internos, con una base PostgreSQL real. */
class ModificarUsuarioInternoIntegracionTest extends BaseUsuariosInternos {

    @Autowired UsuarioInternoService servicio;
    @MockitoBean ProveedorIdentidad proveedor;

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Administrador actor;
    private Administrador otroAdmin;
    private Funcionario funcionario;
    private Medico medico;
    private Especialidad mc1;
    private Especialidad mc2;
    private Especialidad mcInactiva;
    private Consultorio consultorio;

    @BeforeEach
    void datosBase() {
        mc1 = especialidad("MC1");
        mc2 = especialidad("MC2");
        mcInactiva = especialidad("MC3");
        mcInactiva.setEstado(EstadoActivacion.INACTIVO);
        especialidades.saveAndFlush(mcInactiva);
        consultorio = consultorio("MC-C1", EstadoActivacion.ACTIVO);
        actor = administrador("5.630.001-1", "Actor", "Mcactor", "mc.actor@ejemplo.com", EstadoActivacion.ACTIVO);
        otroAdmin = administrador("5.630.002-2", "Otro", "Mcotro", "mc.otro@ejemplo.com", EstadoActivacion.ACTIVO);
        funcionario = funcionarioInterno("5.630.003-3", "Fede", "Mcfuncionario", "mc.fede@ejemplo.com", EstadoActivacion.ACTIVO, LocalDateTime.now());
        medico = medicoInterno("5.630.004-4", "Mara", "Mcmedica", "mc.mara@ejemplo.com", "MC-001", EstadoActivacion.ACTIVO, LocalDateTime.now(), mc1);
    }

    private ModificarUsuarioDTO comoFuncionario(String nombre, String apellido, String correo) {
        return new ModificarUsuarioDTO(nombre, apellido, correo, RolUsuarioInterno.FUNCIONARIO, null, null);
    }

    private ModificarUsuarioDTO comoMedico(String correo, String matricula, String... especialidadIds) {
        return new ModificarUsuarioDTO("Mara", "Mcmedica", correo, RolUsuarioInterno.MEDICO, matricula, List.of(especialidadIds));
    }

    private UsuarioResponseDTO modificar(Long id, ModificarUsuarioDTO datos) {
        return servicio.modificarUsuarioInterno(actor.getIdUsuario(), id, datos);
    }

    private HttpStatus errorAl(Long id, ModificarUsuarioDTO datos) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, () -> modificar(id, datos)).getStatusCode();
    }

    private UsuarioInterno recargado(Long id) {
        usuarios.flush();
        return (UsuarioInterno) usuarios.findById(id).orElseThrow();
    }

    // ------------------------------------------------------------------ datos

    @Test
    void modificaNombreApellidoYCorreoYLaCedulaNoCambia() {
        UsuarioResponseDTO r = modificar(funcionario.getIdUsuario(), comoFuncionario("Federico", "Mcgonzalez", "MC.nuevo@ejemplo.com"));

        assertEquals("Federico", r.nombre());
        assertEquals("Mcgonzalez", r.apellido());
        assertEquals("mc.nuevo@ejemplo.com", r.correo());
        assertEquals("5.630.003-3", r.cedula());
        assertEquals(RolUsuarioInterno.FUNCIONARIO, r.rol());
        UsuarioInterno f = recargado(funcionario.getIdUsuario());
        assertEquals("Federico", f.getNombre());
        assertEquals("mc.nuevo@ejemplo.com", f.getCorreo());
        assertEquals(EstadoActivacion.ACTIVO, f.getEstado());
    }

    @Test
    void registraElAdministradorYLaFechaDelCambio() {
        modificar(funcionario.getIdUsuario(), comoFuncionario("Federico", "Mcfuncionario", "mc.fede@ejemplo.com"));

        UsuarioInterno f = recargado(funcionario.getIdUsuario());
        assertEquals(actor.getIdUsuario(), f.getAdministradorModificacion().getIdUsuario());
        assertNotNull(f.getFechaModificacion());
    }

    @Test
    void sinCambiosNoSeGuardaNadaNiSeRegistraAuditoria() {
        modificar(funcionario.getIdUsuario(), comoFuncionario("Fede", "Mcfuncionario", "MC.FEDE@ejemplo.com"));

        UsuarioInterno f = recargado(funcionario.getIdUsuario());
        assertNull(f.getAdministradorModificacion());
        assertNull(f.getFechaModificacion());
        verify(proveedor, never()).actualizarCorreo(anyString(), anyString());
    }

    @Test
    void elCorreoNuevoTambienSeCambiaEnLaCuentaDeAcceso() {
        modificar(funcionario.getIdUsuario(), comoFuncionario("Fede", "Mcfuncionario", "mc.nuevo@ejemplo.com"));

        verify(proveedor).actualizarCorreo("uid-mc.fede@ejemplo.com", "mc.nuevo@ejemplo.com");
    }

    @Test
    void siFallaElCambioDeCorreoEnLaCuentaLaExcepcionLlegaAlLlamador() {
        doThrow(new ProveedorIdentidadException("sin conexión")).when(proveedor).actualizarCorreo(anyString(), anyString());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, errorAl(funcionario.getIdUsuario(), comoFuncionario("Fede", "Mcfuncionario", "mc.nuevo@ejemplo.com")));
    }

    @Test
    void unCorreoYaTomadoEnLaCuentaDeAccesoEsUn409() {
        doThrow(new CorreoYaRegistradoEnProveedorException()).when(proveedor).actualizarCorreo(anyString(), anyString());

        assertEquals(HttpStatus.CONFLICT, errorAl(funcionario.getIdUsuario(), comoFuncionario("Fede", "Mcfuncionario", "mc.nuevo@ejemplo.com")));
    }

    @Test
    void elCorreoDeOtraPersonaEsUn409() {
        assertEquals(HttpStatus.CONFLICT, errorAl(funcionario.getIdUsuario(), comoFuncionario("Fede", "Mcfuncionario", "MC.OTRO@ejemplo.com")));
    }

    @Test
    void unCorreoSinFormatoValidoEsUn400() {
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(funcionario.getIdUsuario(), comoFuncionario("Fede", "Mcfuncionario", "no-es-un-correo")));
    }

    @Test
    void losCamposObligatoriosSeValidanEnElDto() {
        ModificarUsuarioDTO vacio = new ModificarUsuarioDTO(" ", "", null, null, null, null);

        assertEquals(4, validador.validate(vacio).size()); // nombre, apellido, correo y rol
    }

    @Test
    void unPacienteOUnIdInexistenteSonUn404() {
        var paciente = pacienteInterno("5.630.098-8", "Paula", "Mcpaciente", "mc.paciente@ejemplo.com");

        assertEquals(HttpStatus.NOT_FOUND, errorAl(paciente.getIdUsuario(), comoFuncionario("Paula", "Mcpaciente", "mc.paciente@ejemplo.com")));
        assertEquals(HttpStatus.NOT_FOUND, errorAl(-1L, comoFuncionario("X", "Y", "x@ejemplo.com")));
    }

    // ------------------------------------------------------------------ médicos

    @Test
    void corrigeLaMatriculaYLasEspecialidadesDeUnMedico() {
        UsuarioResponseDTO r = modificar(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "MC-777", "MC1", "MC2"));

        assertEquals("MC-777", r.matricula());
        assertEquals(List.of("MC1", "MC2"), r.especialidades().stream().map(UsuarioResponseDTO.EspecialidadAsignada::codigo).toList());
        Medico m = (Medico) recargado(medico.getIdUsuario());
        assertEquals("MC-777", m.getMatricula());
        assertEquals(2, m.getEspecialidades().size());
    }

    @Test
    void sePuedeCambiarLaEspecialidadSiNoTieneTurnosEnLaQueSeQuita() {
        UsuarioResponseDTO r = modificar(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "MC-001", "MC2"));

        assertEquals(List.of("MC2"), r.especialidades().stream().map(UsuarioResponseDTO.EspecialidadAsignada::codigo).toList());
    }

    @Test
    void noSeLeQuitaUnaEspecialidadEnLaQueTieneTurnosVigentes() {
        turno(medico, mc1, consultorio, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(7), null, EstadoTurnoSemanal.ACTIVO);

        assertEquals(HttpStatus.CONFLICT, errorAl(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "MC-001", "MC2")));
        // Con la especialidad de los turnos incluida, sí se puede.
        modificar(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "MC-001", "MC1", "MC2"));
    }

    @Test
    void laMatriculaDeOtroMedicoEsUn409() {
        medicoInterno("5.630.005-5", "Otro", "Mcotromedico", "mc.otromedico@ejemplo.com", "MC-002", EstadoActivacion.ACTIVO, LocalDateTime.now(), mc1);

        assertEquals(HttpStatus.CONFLICT, errorAl(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "mc-002", "MC1")));
    }

    @Test
    void laMatriculaYLasEspecialidadesSonObligatoriasParaUnMedico() {
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", " ", "MC1")));
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "MC-001")));
    }

    @Test
    void lasEspecialidadesNuevasDebenExistirYEstarActivas() {
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "MC-001", "MC1", "NOEXISTE")));
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(medico.getIdUsuario(), comoMedico("mc.mara@ejemplo.com", "MC-001", "MC1", "MC3")));
    }

    @Test
    void unaEspecialidadYaAsignadaSeConservaAunqueSeHayaInactivado() {
        medico.getEspecialidades().add(mcInactiva);
        usuarios.saveAndFlush(medico);

        UsuarioResponseDTO r = modificar(medico.getIdUsuario(), new ModificarUsuarioDTO("Mariana", "Mcmedica", "mc.mara@ejemplo.com",
            RolUsuarioInterno.MEDICO, "MC-001", List.of("MC1", "MC3")));

        assertEquals("Mariana", r.nombre());
        assertEquals(2, r.especialidades().size());
    }

    // ------------------------------------------------------------------ rol

    @Test
    void unFuncionarioPasaAMedicoConMatriculaYEspecialidades() {
        UsuarioResponseDTO r = modificar(funcionario.getIdUsuario(),
            new ModificarUsuarioDTO("Fede", "Mcfuncionario", "mc.fede@ejemplo.com", RolUsuarioInterno.MEDICO, "MC-900", List.of("MC1", "MC2")));

        assertEquals(RolUsuarioInterno.MEDICO, r.rol());
        assertEquals("MC-900", r.matricula());
        assertEquals(2, r.especialidades().size());
        Medico m = assertInstanceOf(Medico.class, recargado(funcionario.getIdUsuario()));
        assertEquals("MC-900", m.getMatricula());
        assertEquals(actor.getIdUsuario(), m.getAdministradorModificacion().getIdUsuario());
        assertEquals("5.630.003-3", m.getCedula());
        assertEquals("uid-mc.fede@ejemplo.com", m.getFirebaseUid()); // la cuenta de acceso sigue siendo la misma
    }

    @Test
    void unMedicoSinActividadPasaAFuncionarioYPierdeMatriculaYEspecialidades() {
        UsuarioResponseDTO r = modificar(medico.getIdUsuario(), comoFuncionario("Mara", "Mcmedica", "mc.mara@ejemplo.com"));

        assertEquals(RolUsuarioInterno.FUNCIONARIO, r.rol());
        assertNull(r.matricula());
        assertEquals(List.of(), r.especialidades());
        assertInstanceOf(Funcionario.class, recargado(medico.getIdUsuario()));
    }

    @Test
    void unFuncionarioPasaAAdministrador() {
        UsuarioResponseDTO r = modificar(funcionario.getIdUsuario(), new ModificarUsuarioDTO("Fede", "Mcfuncionario",
            "mc.fede@ejemplo.com", RolUsuarioInterno.ADMINISTRADOR, null, null));

        assertEquals(RolUsuarioInterno.ADMINISTRADOR, r.rol());
        assertInstanceOf(Administrador.class, recargado(funcionario.getIdUsuario()));
    }

    @Test
    void alCambiarDeRolTambienSeAplicanLosDemasCambios() {
        UsuarioResponseDTO r = modificar(funcionario.getIdUsuario(), new ModificarUsuarioDTO("Federico", "Mcgonzalez",
            "mc.fede2@ejemplo.com", RolUsuarioInterno.ADMINISTRADOR, null, null));

        assertEquals("Federico", r.nombre());
        assertEquals("mc.fede2@ejemplo.com", r.correo());
        assertEquals("mc.fede2@ejemplo.com", recargado(funcionario.getIdUsuario()).getCorreo());
    }

    @Test
    void noSePuedeRetirarElRolDeMedicoConTurnosVigentes() {
        turno(medico, mc1, consultorio, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(7), null, EstadoTurnoSemanal.ACTIVO);

        assertEquals(HttpStatus.CONFLICT, errorAl(medico.getIdUsuario(), comoFuncionario("Mara", "Mcmedica", "mc.mara@ejemplo.com")));
    }

    @Test
    void conActividadHistoricaConElRolQueSeDejaNoSePuedeCambiarDeRol() {
        // Un turno que ya terminó no es «vigente», pero es actividad registrada del médico y se conserva.
        turno(medico, mc1, consultorio, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(60), HOY.minusDays(1), EstadoTurnoSemanal.ACTIVO);

        assertEquals(HttpStatus.CONFLICT, errorAl(medico.getIdUsuario(), comoFuncionario("Mara", "Mcmedica", "mc.mara@ejemplo.com")));
    }

    @Test
    void unAdministradorConCambiosHechosAOtrasCuentasNoPuedeDejarDeSerlo() {
        // otroAdmin figura como quien modificó la cuenta del funcionario: es actividad registrada que no se pierde.
        modificar(funcionario.getIdUsuario(), comoFuncionario("Federico", "Mcfuncionario", "mc.fede@ejemplo.com"));
        servicio.modificarUsuarioInterno(otroAdmin.getIdUsuario(), medico.getIdUsuario(), comoMedico("mc.mara2@ejemplo.com", "MC-001", "MC1"));

        assertEquals(HttpStatus.CONFLICT, errorAl(otroAdmin.getIdUsuario(), comoFuncionario("Otro", "Mcotro", "mc.otro@ejemplo.com")));
    }

    @Test
    void nadieCambiaSuPropioRol() {
        assertEquals(HttpStatus.CONFLICT, errorAl(actor.getIdUsuario(), comoFuncionario("Actor", "Mcactor", "mc.actor@ejemplo.com")));
    }

    @Test
    void noSeLeRetiraElRolAlUltimoAdministradorActivo() {
        inactivarAdministradoresSalvo(otroAdmin.getIdUsuario());

        // Quien lo pide es un administrador que ya quedó inactivo; el único activo es otroAdmin.
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class, () -> servicio.modificarUsuarioInterno(
            actor.getIdUsuario(), otroAdmin.getIdUsuario(), comoFuncionario("Otro", "Mcotro", "mc.otro@ejemplo.com"))).getStatusCode());
    }

    @Test
    void sePuedeRetirarElRolDeAdministradorSiQuedaOtroActivo() {
        UsuarioResponseDTO r = modificar(otroAdmin.getIdUsuario(), comoFuncionario("Otro", "Mcotro", "mc.otro@ejemplo.com"));

        assertEquals(RolUsuarioInterno.FUNCIONARIO, r.rol());
        assertTrue(recargado(otroAdmin.getIdUsuario()) instanceof Funcionario);
    }

    @Test
    void sePuedeModificarUnaCuentaInactivaYSigueInactiva() {
        funcionario.setEstado(EstadoActivacion.INACTIVO);
        usuarios.saveAndFlush(funcionario);

        modificar(funcionario.getIdUsuario(), comoFuncionario("Federico", "Mcfuncionario", "mc.fede@ejemplo.com"));

        assertEquals(EstadoActivacion.INACTIVO, recargado(funcionario.getIdUsuario()).getEstado());
    }
}
