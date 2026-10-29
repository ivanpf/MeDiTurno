package com.mediturno.backend.consultorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.dto.ConsultorioDetalleResponseDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.usuarios.model.Medico;

/** CU-39 Listado y búsqueda de consultorios, y su detalle, con una base PostgreSQL real. */
class ListadoConsultoriosIntegracionTest extends PostgresIntegracionTest {

    @Autowired ConsultorioService servicio;

    private Consultorio norte;
    private Consultorio sur;
    private Consultorio pediatria;
    private Consultorio cerrado;

    private Consultorio crear(String codigo, String nombre, String ubicacion, EstadoActivacion estado) {
        Consultorio c = new Consultorio();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setUbicacion(ubicacion);
        c.setEstado(estado);
        return consultorios.saveAndFlush(c);
    }

    @BeforeEach
    void datosBase() {
        // Los códigos y nombres llevan «LC» para que las búsquedas no se mezclen con otros datos de la base.
        norte = crear("LC-N", "LC Sala norte", "LC Planta baja", EstadoActivacion.ACTIVO);
        sur = crear("LC-S", "LC Sala sur", "LC Piso 2", EstadoActivacion.ACTIVO);
        pediatria = crear("LC-P", "LC Pediatría", "LC piso 2", EstadoActivacion.ACTIVO);
        cerrado = crear("LC-C", "LC Cerrado", "LC Planta baja", EstadoActivacion.INACTIVO);
    }

    private CriteriosBusquedaDTO criterios(String texto, String ubicacion, String estado, String ordenarPor, Boolean descendente) {
        return new CriteriosBusquedaDTO(null, null, null, null, estado, ordenarPor, descendente, null, null, texto, null, ubicacion);
    }

    /** Códigos de los consultorios de estas pruebas que devuelve el listado, en el orden en que salen. */
    private List<String> codigos(List<ConsultorioResponseDTO> lista) {
        return lista.stream().map(ConsultorioResponseDTO::codigo).filter(c -> c.startsWith("LC-")).toList();
    }

    private ResponseStatusException error(CriteriosBusquedaDTO criterios) {
        return assertThrows(ResponseStatusException.class, () -> servicio.listarConsultorios(criterios));
    }

    private Medico medicoDe(String id, Especialidad especialidad) {
        return medico(id, EstadoActivacion.ACTIVO, especialidad);
    }

    // ------------------------------------------------------------------ listado

    @Test
    void sinCriteriosDevuelveTodosPorNombre() {
        List<ConsultorioResponseDTO> lista = servicio.listarConsultorios(CriteriosBusquedaDTO.vacio());

        assertEquals(List.of("LC-C", "LC-P", "LC-N", "LC-S"), codigos(lista));
    }

    @Test
    void buscaPorCodigoONombreSinDistinguirMayusculas() {
        assertEquals(List.of("LC-N"), codigos(servicio.listarConsultorios(criterios("lc-n", null, null, null, null))));
        assertEquals(List.of("LC-N", "LC-S"), codigos(servicio.listarConsultorios(criterios("SALA", null, null, null, null))));
        assertEquals(List.of(), codigos(servicio.listarConsultorios(criterios("no existe", null, null, null, null))));
    }

    @Test
    void losComodinesDeLikeSeBuscanComoTexto() {
        assertEquals(List.of(), codigos(servicio.listarConsultorios(criterios("%", null, null, null, null))));
        assertEquals(List.of(), codigos(servicio.listarConsultorios(criterios("LC_N", null, null, null, null))));
    }

    @Test
    void filtraPorUbicacionExactaSinDistinguirMayusculas() {
        assertEquals(List.of("LC-C", "LC-N"), codigos(servicio.listarConsultorios(criterios(null, "lc planta baja", null, null, null))));
        assertEquals(List.of("LC-P", "LC-S"), codigos(servicio.listarConsultorios(criterios(null, "LC PISO 2", null, null, null))));
        assertEquals(List.of(), codigos(servicio.listarConsultorios(criterios(null, "LC Planta", null, null, null))));
    }

    @Test
    void filtraPorEstado() {
        assertEquals(List.of("LC-C"), codigos(servicio.listarConsultorios(criterios("LC", null, "INACTIVO", null, null))));
        assertEquals(List.of("LC-P", "LC-N", "LC-S"), codigos(servicio.listarConsultorios(criterios("LC", null, "activo", null, null))));
    }

    @Test
    void combinaLosCriterios() {
        assertEquals(List.of("LC-N"), codigos(servicio.listarConsultorios(criterios("sala", "LC Planta baja", "ACTIVO", null, null))));
    }

    @Test
    void ordenaPorCodigoNombreOUbicacionEnAmbosSentidos() {
        assertEquals(List.of("LC-C", "LC-N", "LC-P", "LC-S"), codigos(servicio.listarConsultorios(criterios("LC", null, null, "codigo", null))));
        assertEquals(List.of("LC-S", "LC-P", "LC-N", "LC-C"), codigos(servicio.listarConsultorios(criterios("LC", null, null, "codigo", true))));
        assertEquals(List.of("LC-S", "LC-N", "LC-P", "LC-C"), codigos(servicio.listarConsultorios(criterios("LC", null, null, "nombre", true))));
        List<String> porUbicacion = codigos(servicio.listarConsultorios(criterios("LC", null, null, "ubicacion", false)));
        // «Piso 2» va antes que «Planta baja»; dentro de cada una el orden no está definido.
        assertTrue(porUbicacion.indexOf("LC-S") < porUbicacion.indexOf("LC-C") && porUbicacion.indexOf("LC-P") < porUbicacion.indexOf("LC-N"));
    }

    @Test
    void unEstadoOUnOrdenInvalidosResponden400() {
        assertEquals(HttpStatus.BAD_REQUEST, error(criterios(null, null, "otro", null, null)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, error(criterios(null, null, null, "color", null)).getStatusCode());
    }

    @Test
    void cadaConsultorioTraeLaCantidadDeTurnosVigentes() {
        Especialidad e = especialidad("LC-E");
        Medico m = medicoDe("9831", e);
        Medico otro = medicoDe("9832", e);
        turno(m, e, norte, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(10), null, EstadoTurnoSemanal.ACTIVO);
        turno(m, e, norte, DiaSemana.MARTES, "09:00", "12:00", HOY.minusDays(10), null, EstadoTurnoSemanal.SUSPENDIDO);
        turno(otro, e, norte, DiaSemana.MIERCOLES, "09:00", "12:00", HOY.minusDays(10), HOY, EstadoTurnoSemanal.ACTIVO);
        // Ya terminó: no cuenta.
        turno(m, e, norte, DiaSemana.JUEVES, "09:00", "12:00", HOY.minusDays(30), HOY.minusDays(1), EstadoTurnoSemanal.ACTIVO);
        turno(m, e, sur, DiaSemana.LUNES, "13:00", "16:00", HOY.minusDays(10), null, EstadoTurnoSemanal.ACTIVO);

        List<ConsultorioResponseDTO> lista = servicio.listarConsultorios(criterios("LC", null, null, "codigo", null));

        assertEquals(List.of(0, 3, 0, 1), lista.stream().filter(c -> c.codigo().startsWith("LC-")).map(ConsultorioResponseDTO::cantidadTurnos).toList());
    }

    // ------------------------------------------------------------------ detalle

    @Test
    void elDetalleTraeLosDatosYLosTurnosVigentesOrdenadosPorDiaYHora() {
        Especialidad e = especialidad("LC-E");
        Medico m = medicoDe("9831", e);
        turno(m, e, norte, DiaSemana.MARTES, "09:00", "12:00", HOY.minusDays(10), null, EstadoTurnoSemanal.SUSPENDIDO);
        turno(m, e, norte, DiaSemana.LUNES, "14:00", "17:00", HOY.minusDays(10), null, EstadoTurnoSemanal.ACTIVO);
        turno(m, e, norte, DiaSemana.LUNES, "08:00", "11:00", HOY.minusDays(10), HOY, EstadoTurnoSemanal.ACTIVO);
        turno(m, e, norte, DiaSemana.JUEVES, "09:00", "12:00", HOY.minusDays(30), HOY.minusDays(1), EstadoTurnoSemanal.ACTIVO);
        turno(m, e, sur, DiaSemana.VIERNES, "09:00", "12:00", HOY.minusDays(10), null, EstadoTurnoSemanal.ACTIVO);

        ConsultorioDetalleResponseDTO detalle = servicio.obtenerDetalleConsultorio("LC-N");

        assertEquals("LC-N", detalle.codigo());
        assertEquals("LC Sala norte", detalle.nombre());
        assertEquals("LC Planta baja", detalle.ubicacion());
        assertEquals(EstadoActivacion.ACTIVO, detalle.estado());
        assertEquals(3, detalle.turnos().size());
        assertEquals(List.of(DiaSemana.LUNES, DiaSemana.LUNES, DiaSemana.MARTES), detalle.turnos().stream().map(ConsultorioDetalleResponseDTO.TurnoAsociado::dia).toList());
        assertEquals("08:00", detalle.turnos().get(0).horaInicio().toString());
        assertEquals(EstadoTurnoSemanal.SUSPENDIDO, detalle.turnos().get(2).estado());
        assertEquals("Especialidad LC-E", detalle.turnos().get(0).nombreEspecialidad());
        assertEquals(m.nombreCompleto(), detalle.turnos().get(0).nombreMedico());
    }

    @Test
    void unConsultorioSinTurnosTieneDetalleVacio() {
        assertEquals(List.of(), servicio.obtenerDetalleConsultorio("LC-C").turnos());
    }

    @Test
    void unConsultorioInexistenteResponde404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> servicio.obtenerDetalleConsultorio("NO-EXISTE"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
