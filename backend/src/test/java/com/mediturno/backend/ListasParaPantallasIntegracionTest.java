package com.mediturno.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.ConsultorioService;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.especialidades.EspecialidadService;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.usuarios.MedicoService;
import com.mediturno.backend.usuarios.dto.MedicoResponseDTO;

/** Los tres listados que usan las pantallas para llenar sus listas desplegables, con una base PostgreSQL real. */
class ListasParaPantallasIntegracionTest extends PostgresIntegracionTest {

    @Autowired MedicoService medicoService;
    @Autowired EspecialidadService especialidadService;
    @Autowired ConsultorioService consultorioService;

    private Especialidad especialidadInactiva(String codigo) {
        Especialidad e = especialidad(codigo);
        e.setEstado(EstadoActivacion.INACTIVO);
        return especialidades.saveAndFlush(e);
    }

    @Test
    void losMedicosSonSoloLosActivosConSusEspecialidadesActivasOrdenadasPorNombre() {
        Especialidad cardiologia = especialidad("CARD");
        Especialidad pediatria = especialidad("PED");
        Especialidad antigua = especialidadInactiva("ANT");
        medico("9201", EstadoActivacion.ACTIVO, pediatria, cardiologia, antigua);
        medico("9202", EstadoActivacion.INACTIVO, cardiologia);

        List<MedicoResponseDTO> lista = medicoService.listarMedicosActivos();

        MedicoResponseDTO activo = lista.stream().filter(m -> m.apellido().equals("Apellido9201")).findFirst().orElseThrow();
        assertEquals(List.of("CARD", "PED"), activo.especialidades().stream().map(EspecialidadResponseDTO::codigo).toList());
        assertTrue(lista.stream().noneMatch(m -> m.apellido().equals("Apellido9202")));
        assertTrue(lista.stream().allMatch(m -> m.idMedico() != null));
    }

    @Test
    void losMedicosSalenOrdenadosPorApellidoYNombre() {
        medico("9203", EstadoActivacion.ACTIVO);
        medico("9204", EstadoActivacion.ACTIVO);

        List<String> apellidos = medicoService.listarMedicosActivos().stream().map(MedicoResponseDTO::apellido).toList();

        assertTrue(apellidos.indexOf("Apellido9203") < apellidos.indexOf("Apellido9204"));
    }

    @Test
    void lasEspecialidadesSalenTodasOrdenadasPorNombre() {
        especialidad("ZZZ");
        especialidadInactiva("AAA");

        List<EspecialidadResponseDTO> lista = especialidadService.listarEspecialidades(CriteriosBusquedaDTO.vacio());

        List<String> codigos = lista.stream().map(EspecialidadResponseDTO::codigo).toList();
        assertTrue(codigos.contains("AAA") && codigos.contains("ZZZ"));
        assertTrue(codigos.indexOf("AAA") < codigos.indexOf("ZZZ"));
        assertEquals(EstadoActivacion.INACTIVO, lista.stream().filter(e -> e.codigo().equals("AAA")).findFirst().orElseThrow().estado());
    }

    @Test
    void losConsultoriosSalenTodosOrdenadosPorNombre() {
        consultorio("ZC", EstadoActivacion.ACTIVO);
        consultorio("AC", EstadoActivacion.INACTIVO);

        List<ConsultorioResponseDTO> lista = consultorioService.listarConsultorios(CriteriosBusquedaDTO.vacio());

        List<String> codigos = lista.stream().map(ConsultorioResponseDTO::codigo).toList();
        assertTrue(codigos.indexOf("AC") < codigos.indexOf("ZC"));
        ConsultorioResponseDTO inactivo = lista.stream().filter(c -> c.codigo().equals("AC")).findFirst().orElseThrow();
        assertEquals(EstadoActivacion.INACTIVO, inactivo.estado());
        assertEquals("Piso 1", inactivo.ubicacion());
    }
}
