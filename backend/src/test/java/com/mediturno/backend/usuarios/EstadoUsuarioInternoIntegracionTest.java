package com.mediturno.backend.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.UsuarioInterno;

/** CU-46 Activación e inactivación de usuarios internos, con una base PostgreSQL real. */
class EstadoUsuarioInternoIntegracionTest extends BaseUsuariosInternos {

    @Autowired UsuarioInternoService servicio;

    private Administrador actor;     // quien hace los cambios
    private Administrador otroAdmin;
    private Funcionario funcionario;
    private Medico medico;
    private Especialidad especialidad;
    private Consultorio consultorio;

    @BeforeEach
    void datosBase() {
        especialidad = especialidad("ZE1");
        consultorio = consultorio("ZE-C1", EstadoActivacion.ACTIVO);
        actor = administrador("5.620.001-1", "Actor", "Zeactor", "ze.actor@ejemplo.com", EstadoActivacion.ACTIVO);
        otroAdmin = administrador("5.620.002-2", "Otro", "Zeotro", "ze.otro@ejemplo.com", EstadoActivacion.ACTIVO);
        funcionario = funcionarioInterno("5.620.003-3", "Fede", "Zefuncionario", "ze.fede@ejemplo.com", EstadoActivacion.ACTIVO, LocalDateTime.now());
        medico = medicoInterno("5.620.004-4", "Mara", "Zemedica", "ze.mara@ejemplo.com", "ZE-001", EstadoActivacion.ACTIVO, LocalDateTime.now(), especialidad);
    }

    private HttpStatus errorAlInactivar(Long idActor, Long id) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, () -> servicio.inactivarUsuario(idActor, id)).getStatusCode();
    }

    private UsuarioInterno recargado(Long id) {
        usuarios.flush();
        return (UsuarioInterno) usuarios.findById(id).orElseThrow();
    }

    // ------------------------------------------------------------------ inactivación

    @Test
    void inactivaUnaCuentaYRegistraQuienYCuando() {
        servicio.inactivarUsuario(actor.getIdUsuario(), funcionario.getIdUsuario());

        UsuarioInterno f = recargado(funcionario.getIdUsuario());
        assertEquals(EstadoActivacion.INACTIVO, f.getEstado());
        assertEquals(actor.getIdUsuario(), f.getAdministradorModificacion().getIdUsuario());
        assertNotNull(f.getFechaModificacion());
    }

    @Test
    void activaUnaCuentaInactiva() {
        servicio.inactivarUsuario(actor.getIdUsuario(), funcionario.getIdUsuario());

        servicio.activarUsuario(otroAdmin.getIdUsuario(), funcionario.getIdUsuario());

        UsuarioInterno f = recargado(funcionario.getIdUsuario());
        assertEquals(EstadoActivacion.ACTIVO, f.getEstado());
        assertEquals(otroAdmin.getIdUsuario(), f.getAdministradorModificacion().getIdUsuario());
    }

    @Test
    void inactivarUnaCuentaInactivaOActivarUnaActivaEsUn409() {
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> servicio.activarUsuario(actor.getIdUsuario(), funcionario.getIdUsuario())).getStatusCode());
        servicio.inactivarUsuario(actor.getIdUsuario(), funcionario.getIdUsuario());
        assertEquals(HttpStatus.CONFLICT, errorAlInactivar(actor.getIdUsuario(), funcionario.getIdUsuario()));
    }

    @Test
    void nadieInactivaSuPropiaCuenta() {
        assertEquals(HttpStatus.CONFLICT, errorAlInactivar(actor.getIdUsuario(), actor.getIdUsuario()));
        assertEquals(EstadoActivacion.ACTIVO, recargado(actor.getIdUsuario()).getEstado());
    }

    @Test
    void noSePuedeInactivarAlUltimoAdministradorActivo() {
        inactivarAdministradoresSalvo(otroAdmin.getIdUsuario());

        // Quien lo pide es otra persona (por ejemplo, un administrador que ya quedó inactivo) y el último activo es otroAdmin.
        assertEquals(HttpStatus.CONFLICT, errorAlInactivar(actor.getIdUsuario(), otroAdmin.getIdUsuario()));
        assertEquals(EstadoActivacion.ACTIVO, recargado(otroAdmin.getIdUsuario()).getEstado());
    }

    @Test
    void sePuedeInactivarAUnAdministradorSiQuedaOtroActivo() {
        servicio.inactivarUsuario(actor.getIdUsuario(), otroAdmin.getIdUsuario());

        assertEquals(EstadoActivacion.INACTIVO, recargado(otroAdmin.getIdUsuario()).getEstado());
    }

    @Test
    void noSePuedeInactivarAUnMedicoConTurnosVigentes() {
        turno(medico, especialidad, consultorio, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(7), null, EstadoTurnoSemanal.ACTIVO);

        assertEquals(HttpStatus.CONFLICT, errorAlInactivar(actor.getIdUsuario(), medico.getIdUsuario()));
        assertEquals(EstadoActivacion.ACTIVO, recargado(medico.getIdUsuario()).getEstado());
    }

    @Test
    void tambienCuentanLosTurnosVigentesSuspendidos() {
        turno(medico, especialidad, consultorio, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(7), HOY.plusDays(30), EstadoTurnoSemanal.SUSPENDIDO);

        assertEquals(HttpStatus.CONFLICT, errorAlInactivar(actor.getIdUsuario(), medico.getIdUsuario()));
    }

    @Test
    void unMedicoConTurnosYaTerminadosSeInactivaYSeConservanSusTurnos() {
        turno(medico, especialidad, consultorio, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(60), HOY.minusDays(1), EstadoTurnoSemanal.ACTIVO);

        servicio.inactivarUsuario(actor.getIdUsuario(), medico.getIdUsuario());

        assertEquals(EstadoActivacion.INACTIVO, recargado(medico.getIdUsuario()).getEstado());
        assertEquals(1, turnos.findAll().stream().filter(t -> t.getMedico().getIdUsuario().equals(medico.getIdUsuario())).count());
    }

    @Test
    void unaCuentaInactivaNoTocaLaAuditoriaDeLasQueNoSeModificaron() {
        servicio.inactivarUsuario(actor.getIdUsuario(), funcionario.getIdUsuario());

        assertNull(recargado(medico.getIdUsuario()).getAdministradorModificacion());
        assertNull(recargado(medico.getIdUsuario()).getFechaModificacion());
    }

    @Test
    void unPacienteOUnIdInexistenteSonUn404() {
        Paciente paciente = pacienteInterno("5.620.098-8", "Paula", "Zepaciente", "ze.paciente@ejemplo.com");

        assertEquals(HttpStatus.NOT_FOUND, errorAlInactivar(actor.getIdUsuario(), paciente.getIdUsuario()));
        assertEquals(HttpStatus.NOT_FOUND, errorAlInactivar(actor.getIdUsuario(), -1L));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> servicio.activarUsuario(actor.getIdUsuario(), paciente.getIdUsuario())).getStatusCode());
    }
}
