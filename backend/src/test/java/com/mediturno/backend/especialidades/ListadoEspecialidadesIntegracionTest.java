package com.mediturno.backend.especialidades;

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
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.dto.EspecialidadDetalleResponseDTO;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.usuarios.model.Medico;

/** CU-30 Listado y búsqueda de especialidades médicas, y su detalle, con una base PostgreSQL real. */
class ListadoEspecialidadesIntegracionTest extends PostgresIntegracionTest {

    @Autowired EspecialidadService servicio;

    private Especialidad cardiologia;
    private Especialidad pediatria;
    private Especialidad neurologia;
    private Especialidad dermatologia;

    private Especialidad crear(String codigo, String nombre, String descripcion, boolean requierePase, EstadoActivacion estado) {
        Especialidad e = new Especialidad();
        e.setCodigo(codigo);
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        e.setRequierePase(requierePase);
        e.setEstado(estado);
        return especialidades.saveAndFlush(e);
    }

    @BeforeEach
    void datosBase() {
        // Los códigos y nombres llevan «LT» para que las búsquedas no se mezclen con otros datos de la base.
        cardiologia = crear("LT-CARD", "LT Cardiología", "Corazón", true, EstadoActivacion.ACTIVO);
        pediatria = crear("LT-PED", "LT Pediatría", null, false, EstadoActivacion.ACTIVO);
        neurologia = crear("LT-NEU", "LT Neurología", "Sistema nervioso", true, EstadoActivacion.INACTIVO);
        dermatologia = crear("LT-DER", "LT Dermatología", "Piel", false, EstadoActivacion.INACTIVO);
    }

    private CriteriosBusquedaDTO criterios(String texto, String estado, Boolean requierePase, String ordenarPor, Boolean descendente) {
        return new CriteriosBusquedaDTO(null, null, null, null, estado, ordenarPor, descendente, null, null, texto, requierePase);
    }

    /** Códigos de las especialidades de estas pruebas que devuelve el listado, en el orden en que salen. */
    private List<String> codigos(List<EspecialidadResponseDTO> lista) {
        return lista.stream().map(EspecialidadResponseDTO::codigo).filter(c -> c.startsWith("LT-")).toList();
    }

    private ResponseStatusException error(CriteriosBusquedaDTO criterios) {
        return assertThrows(ResponseStatusException.class, () -> servicio.listarEspecialidades(criterios));
    }

    // ------------------------------------------------------------------ listado

    @Test
    void sinCriteriosDevuelveTodasPorNombreConTodosSusDatos() {
        List<EspecialidadResponseDTO> lista = servicio.listarEspecialidades(CriteriosBusquedaDTO.vacio());

        assertEquals(List.of("LT-CARD", "LT-DER", "LT-NEU", "LT-PED"), codigos(lista));
        EspecialidadResponseDTO neuro = lista.stream().filter(e -> e.codigo().equals("LT-NEU")).findFirst().orElseThrow();
        assertEquals(new EspecialidadResponseDTO("LT-NEU", "LT Neurología", "Sistema nervioso", true, EstadoActivacion.INACTIVO), neuro);
    }

    @Test
    void buscaPorNombreSinDistinguirMayusculas() {
        assertEquals(List.of("LT-CARD"), codigos(servicio.listarEspecialidades(criterios("lt cardio", null, null, null, null))));
        assertEquals(List.of("LT-CARD"), codigos(servicio.listarEspecialidades(criterios("  LT CARDIOLOGÍA ", null, null, null, null))));
    }

    @Test
    void buscaPorCodigo() {
        assertEquals(List.of("LT-PED"), codigos(servicio.listarEspecialidades(criterios("lt-ped", null, null, null, null))));
    }

    @Test
    void laBusquedaTrataLosComodinesDeLikeComoTexto() {
        assertEquals(List.of(), codigos(servicio.listarEspecialidades(criterios("%", null, null, null, null))));
        assertEquals(List.of(), codigos(servicio.listarEspecialidades(criterios("LT_PED", null, null, null, null))));
    }

    @Test
    void sinCoincidenciasDevuelveUnaListaVacia() {
        assertEquals(List.of(), servicio.listarEspecialidades(criterios("no-existe-ninguna", null, null, null, null)));
    }

    @Test
    void filtraPorEstado() {
        assertEquals(List.of("LT-CARD", "LT-PED"), codigos(servicio.listarEspecialidades(criterios("LT", "ACTIVO", null, null, null))));
        assertEquals(List.of("LT-DER", "LT-NEU"), codigos(servicio.listarEspecialidades(criterios("LT", "inactivo", null, null, null))));
    }

    @Test
    void filtraPorRequisitoDePase() {
        assertEquals(List.of("LT-CARD", "LT-NEU"), codigos(servicio.listarEspecialidades(criterios("LT", null, true, null, null))));
        assertEquals(List.of("LT-DER", "LT-PED"), codigos(servicio.listarEspecialidades(criterios("LT", null, false, null, null))));
    }

    @Test
    void combinaLosCriterios() {
        assertEquals(List.of("LT-NEU"), codigos(servicio.listarEspecialidades(criterios("neuro", "INACTIVO", true, null, null))));
        assertEquals(List.of(), codigos(servicio.listarEspecialidades(criterios("neuro", "ACTIVO", true, null, null))));
    }

    @Test
    void ordenaPorNombreOPorCodigoEnAmbosSentidos() {
        assertEquals(List.of("LT-PED", "LT-NEU", "LT-DER", "LT-CARD"),
            codigos(servicio.listarEspecialidades(criterios("LT", null, null, "nombre", true))));
        assertEquals(List.of("LT-CARD", "LT-DER", "LT-NEU", "LT-PED"),
            codigos(servicio.listarEspecialidades(criterios("LT", null, null, "codigo", false))));
        assertEquals(List.of("LT-PED", "LT-NEU", "LT-DER", "LT-CARD"),
            codigos(servicio.listarEspecialidades(criterios("LT", null, null, "CODIGO", true))));
    }

    @Test
    void elOrdenNoDistingueMayusculasNiMinusculas() {
        crear("LT-ALFA", "lt alfa", null, false, EstadoActivacion.ACTIVO);

        assertEquals(List.of("LT-ALFA", "LT-CARD", "LT-DER", "LT-NEU", "LT-PED"),
            codigos(servicio.listarEspecialidades(criterios("LT", null, null, "nombre", false))));
    }

    @Test
    void unEstadoOUnOrdenInvalidosRespondenBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, error(criterios(null, "INVALIDO", null, null, null)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, error(criterios(null, null, null, "descripcion", null)).getStatusCode());
    }

    // ------------------------------------------------------------------ detalle

    @Test
    void elDetalleTraeLosDatosDeLaEspecialidad() {
        EspecialidadDetalleResponseDTO detalle = servicio.obtenerDetalleEspecialidad("LT-CARD");

        assertEquals("LT-CARD", detalle.codigo());
        assertEquals("LT Cardiología", detalle.nombre());
        assertEquals("Corazón", detalle.descripcion());
        assertTrue(detalle.requierePase());
        assertEquals(EstadoActivacion.ACTIVO, detalle.estado());
    }

    @Test
    void elDetalleDeUnaEspecialidadSinAsociacionesTraeListasVacias() {
        EspecialidadDetalleResponseDTO detalle = servicio.obtenerDetalleEspecialidad("LT-PED");

        assertEquals(List.of(), detalle.medicos());
        assertEquals(List.of(), detalle.turnos());
    }

    @Test
    void elDetalleMuestraSoloLosMedicosActivosQueLaAtienden() {
        Medico activo = medico("9701", EstadoActivacion.ACTIVO, cardiologia, pediatria);
        medico("9702", EstadoActivacion.INACTIVO, cardiologia);
        medico("9703", EstadoActivacion.ACTIVO, pediatria);

        List<EspecialidadDetalleResponseDTO.MedicoAsociado> medicos = servicio.obtenerDetalleEspecialidad("LT-CARD").medicos();

        assertEquals(1, medicos.size());
        assertEquals(activo.getIdUsuario(), medicos.get(0).idMedico());
        assertEquals("Apellido9701, Nombre9701", medicos.get(0).nombreCompleto());
        assertEquals("MAT-9701", medicos.get(0).matricula());
    }

    @Test
    void elDetalleMuestraLosTurnosVigentesDeEsaEspecialidadPorDiaYHorario() {
        Medico medico = medico("9704", EstadoActivacion.ACTIVO, cardiologia, pediatria);
        Consultorio c1 = consultorio("LT1", EstadoActivacion.ACTIVO);
        Consultorio c2 = consultorio("LT2", EstadoActivacion.ACTIVO);
        TurnoSemanal martes = turno(medico, cardiologia, c1, DiaSemana.MARTES, "09:00", "12:00", HOY.minusDays(10), null, EstadoTurnoSemanal.ACTIVO);
        TurnoSemanal lunesTarde = turno(medico, cardiologia, c2, DiaSemana.LUNES, "14:00", "17:00", HOY.minusDays(10), HOY.plusDays(20), EstadoTurnoSemanal.SUSPENDIDO);
        TurnoSemanal lunesManana = turno(medico, cardiologia, c1, DiaSemana.LUNES, "08:00", "11:00", HOY.minusDays(10), HOY, EstadoTurnoSemanal.ACTIVO);
        turno(medico, cardiologia, c2, DiaSemana.JUEVES, "08:00", "11:00", HOY.minusDays(40), HOY.minusDays(1), EstadoTurnoSemanal.ACTIVO); // terminó ayer
        turno(medico, pediatria, c2, DiaSemana.VIERNES, "08:00", "11:00", HOY.minusDays(10), null, EstadoTurnoSemanal.ACTIVO); // otra especialidad

        List<EspecialidadDetalleResponseDTO.TurnoAsociado> lista = servicio.obtenerDetalleEspecialidad("LT-CARD").turnos();

        assertEquals(List.of(lunesManana.getIdTurnoSemanal(), lunesTarde.getIdTurnoSemanal(), martes.getIdTurnoSemanal()),
            lista.stream().map(EspecialidadDetalleResponseDTO.TurnoAsociado::idTurnoSemanal).toList());
        EspecialidadDetalleResponseDTO.TurnoAsociado suspendido = lista.get(1);
        assertEquals("Apellido9704, Nombre9704", suspendido.nombreMedico());
        assertEquals(DiaSemana.LUNES, suspendido.dia());
        assertEquals("14:00", suspendido.horaInicio().toString());
        assertEquals("17:00", suspendido.horaFin().toString());
        assertEquals("LT2", suspendido.codigoConsultorio());
        assertEquals("Consultorio LT2", suspendido.nombreConsultorio());
        assertEquals(EstadoTurnoSemanal.SUSPENDIDO, suspendido.estado());
    }

    @Test
    void elDetalleDeUnaEspecialidadInexistenteResponde404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> servicio.obtenerDetalleEspecialidad("NO-EXISTE"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
