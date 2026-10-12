package com.mediturno.backend.consultas;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.consultas.dto.ConsultaResponseDTO;
import com.mediturno.backend.consultas.dto.RegistrarConsultaDTO;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.pacientes.model.Paciente;

/**
 * Endpoints de: CU-11, CU-21, CU-22, CU-23, CU-26.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Medico.class)
@RestController
@RequestMapping("/api")
public class ConsultaController {

    private final ConsultaService consultaService;
    private final UsuarioActual usuarioActual;

    public ConsultaController(ConsultaService consultaService, UsuarioActual usuarioActual) {
        this.consultaService = consultaService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/consultas/{numeroOrden}")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultaResponseDTO registrarConsulta(@PathVariable Long numeroOrden, @RequestBody RegistrarConsultaDTO datos) {
        return consultaService.registrarConsulta(usuarioActual.obtenerIdMedicoAutenticado(), numeroOrden, datos);
    }

    @RolesPermitidos(Paciente.class)
    @GetMapping("/pacientes/me/historia-clinica")
    public List<ConsultaResponseDTO> consultarHistoriaClinicaPropia(CriteriosBusquedaDTO criterios) {
        return consultaService.consultarHistoriaClinicaPropia(usuarioActual.obtenerIdPacienteAutenticado(), criterios);
    }

    @GetMapping("/consultas/historia-clinica")
    public List<ConsultaResponseDTO> consultarHistoriaClinicaDuranteAtencion(@RequestParam Long idPaciente) {
        return consultaService.consultarHistoriaClinicaDuranteAtencion(usuarioActual.obtenerIdMedicoAutenticado(), idPaciente);
    }

    @GetMapping("/turnos/{idOcurrenciaTurno}/pacientes")
    public List<ReservaResponseDTO> consultarPacientesPorTurno(@PathVariable Long idOcurrenciaTurno) {
        return consultaService.consultarPacientesPorTurno(usuarioActual.obtenerIdMedicoAutenticado(), idOcurrenciaTurno);
    }

    @GetMapping("/consultas/mias")
    public List<ConsultaResponseDTO> consultarHistorialAtencionesPropias(CriteriosBusquedaDTO criterios) {
        return consultaService.consultarHistorialAtencionesPropias(usuarioActual.obtenerIdMedicoAutenticado(), criterios);
    }
}
