package com.mediturno.backend.especialidades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.dto.ModificarEspecialidadDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;

/** CU-43 Modificación de especialidades médicas, con una base PostgreSQL real. */
class ModificarEspecialidadIntegracionTest extends PostgresIntegracionTest {

    @Autowired EspecialidadService servicio;
    @Autowired AdministradorRepository administradores;

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Administrador admin;
    private Especialidad cardiologia;
    private Especialidad pediatria;

    @BeforeEach
    void datosBase() {
        Administrador a = new Administrador();
        a.setCorreo("9801@ejemplo.com");
        a.setEstado(EstadoActivacion.ACTIVO);
        a.setFechaCreacion(LocalDateTime.now());
        a.setCedula("9801");
        a.setNombre("Admin");
        a.setApellido("Prueba");
        a.setFechaNacimiento(java.time.LocalDate.of(1980, 1, 1));
        admin = administradores.save(a);
        cardiologia = crear("MD-CARD", "MD Cardiología", "Corazón", true, EstadoActivacion.ACTIVO);
        pediatria = crear("MD-PED", "MD Pediatría", null, false, EstadoActivacion.ACTIVO);
    }

    private Especialidad crear(String codigo, String nombre, String descripcion, boolean requierePase, EstadoActivacion estado) {
        Especialidad e = new Especialidad();
        e.setCodigo(codigo);
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        e.setRequierePase(requierePase);
        e.setEstado(estado);
        return especialidades.saveAndFlush(e);
    }

    private ModificarEspecialidadDTO datos(String nombre, String descripcion, boolean requierePase, EstadoActivacion estado) {
        return new ModificarEspecialidadDTO(nombre, descripcion, requierePase, estado);
    }

    private EspecialidadResponseDTO modificar(String codigo, ModificarEspecialidadDTO datos) {
        return servicio.modificarEspecialidad(admin.getIdUsuario(), codigo, datos);
    }

    private HttpStatus errorAl(String codigo, ModificarEspecialidadDTO datos) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, () -> modificar(codigo, datos)).getStatusCode();
    }

    private Especialidad guardada(String codigo) {
        especialidades.flush();
        return especialidades.findById(codigo).orElseThrow();
    }

    private void turnoVigente(Especialidad especialidad, EstadoTurnoSemanal estado, java.time.LocalDate hasta) {
        Medico medico = medico("98" + Math.abs(especialidad.getCodigo().hashCode() % 90 + 10) + estado.ordinal(), EstadoActivacion.ACTIVO, especialidad);
        Consultorio consultorio = consultorio("MD" + estado.ordinal() + (hasta == null ? "N" : "H"), EstadoActivacion.ACTIVO);
        turno(medico, especialidad, consultorio, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(10), hasta, estado);
    }

    @Test
    void modificaLosCuatroCamposYDevuelveLaEspecialidadActualizada() {
        EspecialidadResponseDTO respuesta = modificar("MD-CARD", datos("MD Cardiología clínica", "Atención del corazón", false, EstadoActivacion.INACTIVO));

        assertEquals(new EspecialidadResponseDTO("MD-CARD", "MD Cardiología clínica", "Atención del corazón", false, EstadoActivacion.INACTIVO), respuesta);
        Especialidad e = guardada("MD-CARD");
        assertEquals("MD Cardiología clínica", e.getNombre());
        assertEquals("Atención del corazón", e.getDescripcion());
        assertFalse(e.isRequierePase());
        assertEquals(EstadoActivacion.INACTIVO, e.getEstado());
    }

    @Test
    void registraElAdministradorYLaFechaDelCambio() {
        assertNull(cardiologia.getAdministradorModificacion());
        assertNull(cardiologia.getFechaModificacion());
        LocalDateTime antes = LocalDateTime.now().minusMinutes(1);

        modificar("MD-CARD", datos("MD Cardiología", "Otra descripción", true, EstadoActivacion.ACTIVO));

        Especialidad e = guardada("MD-CARD");
        assertEquals(admin.getIdUsuario(), e.getAdministradorModificacion().getIdUsuario());
        assertTrue(e.getFechaModificacion().isAfter(antes));
    }

    @Test
    void sinCambiosNoRegistraNada() {
        modificar("MD-CARD", datos("MD Cardiología", "Corazón", true, EstadoActivacion.ACTIVO));

        Especialidad e = guardada("MD-CARD");
        assertNull(e.getAdministradorModificacion());
        assertNull(e.getFechaModificacion());
    }

    @Test
    void recortaLosEspaciosYUnaDescripcionVaciaLaBorra() {
        modificar("MD-CARD", datos("  MD Cardio  ", "   ", true, EstadoActivacion.ACTIVO));

        Especialidad e = guardada("MD-CARD");
        assertEquals("MD Cardio", e.getNombre());
        assertNull(e.getDescripcion());
    }

    @Test
    void elNombreDeOtraEspecialidadResponde409SinModificarNada() {
        assertEquals(HttpStatus.CONFLICT, errorAl("MD-CARD", datos("md pediatría", "Nueva", false, EstadoActivacion.INACTIVO)));

        Especialidad e = guardada("MD-CARD");
        assertEquals("MD Cardiología", e.getNombre());
        assertEquals("Corazón", e.getDescripcion());
        assertEquals(EstadoActivacion.ACTIVO, e.getEstado());
    }

    @Test
    void puedeConservarSuPropioNombreAunqueCambieLasMayusculas() {
        modificar("MD-CARD", datos("MD CARDIOLOGÍA", "Corazón", true, EstadoActivacion.ACTIVO));

        assertEquals("MD CARDIOLOGÍA", guardada("MD-CARD").getNombre());
    }

    @Test
    void noSePuedeInactivarConTurnosVigentes() {
        turnoVigente(cardiologia, EstadoTurnoSemanal.ACTIVO, null);

        assertEquals(HttpStatus.CONFLICT, errorAl("MD-CARD", datos("MD Cardiología", "Corazón", true, EstadoActivacion.INACTIVO)));
        assertEquals(EstadoActivacion.ACTIVO, guardada("MD-CARD").getEstado());
    }

    @Test
    void unTurnoSuspendidoSinFinTambienImpideInactivar() {
        turnoVigente(cardiologia, EstadoTurnoSemanal.SUSPENDIDO, null);

        assertEquals(HttpStatus.CONFLICT, errorAl("MD-CARD", datos("MD Cardiología", "Corazón", true, EstadoActivacion.INACTIVO)));
    }

    @Test
    void sePuedeInactivarSiSusTurnosYaTerminaron() {
        turnoVigente(cardiologia, EstadoTurnoSemanal.ACTIVO, HOY.minusDays(1));

        modificar("MD-CARD", datos("MD Cardiología", "Corazón", true, EstadoActivacion.INACTIVO));

        assertEquals(EstadoActivacion.INACTIVO, guardada("MD-CARD").getEstado());
    }

    @Test
    void unaEspecialidadInactivaConTurnosPuedeEditarseSinReactivarse() {
        turnoVigente(cardiologia, EstadoTurnoSemanal.ACTIVO, null);
        cardiologia.setEstado(EstadoActivacion.INACTIVO);
        especialidades.saveAndFlush(cardiologia);

        modificar("MD-CARD", datos("MD Cardiología", "Descripción nueva", true, EstadoActivacion.INACTIVO));

        assertEquals("Descripción nueva", guardada("MD-CARD").getDescripcion());
    }

    @Test
    void sePuedeReactivarUnaEspecialidadInactiva() {
        pediatria.setEstado(EstadoActivacion.INACTIVO);
        especialidades.saveAndFlush(pediatria);

        modificar("MD-PED", datos("MD Pediatría", null, false, EstadoActivacion.ACTIVO));

        assertEquals(EstadoActivacion.ACTIVO, guardada("MD-PED").getEstado());
    }

    @Test
    void cambiarElRequisitoDePaseNoTocaOtrosDatos() {
        modificar("MD-PED", datos("MD Pediatría", null, true, EstadoActivacion.ACTIVO));

        Especialidad e = guardada("MD-PED");
        assertTrue(e.isRequierePase());
        assertEquals("MD-PED", e.getCodigo());
        assertEquals(EstadoActivacion.ACTIVO, e.getEstado());
    }

    @Test
    void unaEspecialidadInexistenteResponde404() {
        assertEquals(HttpStatus.NOT_FOUND, errorAl("NO-EXISTE", datos("Nombre", null, false, EstadoActivacion.ACTIVO)));
    }

    @Test
    void losCamposObligatoriosYLosLargosSeValidanEnElDTO() {
        Set<ConstraintViolation<ModificarEspecialidadDTO>> faltan = validador.validate(new ModificarEspecialidadDTO(" ", null, null, null));
        assertEquals(3, faltan.size());

        Set<ConstraintViolation<ModificarEspecialidadDTO>> largos = validador
            .validate(new ModificarEspecialidadDTO("n".repeat(101), "d".repeat(501), true, EstadoActivacion.ACTIVO));
        assertEquals(2, largos.size());

        assertTrue(validador.validate(datos("Nombre", null, false, EstadoActivacion.ACTIVO)).isEmpty());
    }
}
