package com.mediturno.backend.turnos;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.turnos.dto.AltaTurnoSemanalDTO;
import com.mediturno.backend.turnos.dto.OcurrenciaTurnoDTO;
import com.mediturno.backend.turnos.dto.RehabilitacionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoResponseDTO;
import com.mediturno.backend.turnos.dto.TurnoSemanalResponseDTO;
import com.mediturno.backend.turnos.dto.VistaPreviaSuspensionDTO;
import jakarta.validation.Valid;
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
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.pacientes.model.Paciente;

/**
 * Endpoints de: CU-8, CU-12, CU-13, CU-14, CU-20.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Funcionario.class)
@RestController
@RequestMapping("/api/turnos-semanales")
public class TurnoSemanalController {

    private final TurnoSemanalService turnoSemanalService;
    private final UsuarioActual usuarioActual;

    public TurnoSemanalController(TurnoSemanalService turnoSemanalService, UsuarioActual usuarioActual) {
        this.turnoSemanalService = turnoSemanalService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<TurnoSemanalResponseDTO> altaTurnoSemanal(@Valid @RequestBody AltaTurnoSemanalDTO datos) {
        return turnoSemanalService.altaTurnoSemanal(datos);
    }

    @PostMapping("/{idTurnoSemanal}/suspension")
    @ResponseStatus(HttpStatus.CREATED)
    public SuspensionTurnoResponseDTO suspenderTurno(@PathVariable Long idTurnoSemanal, @Valid @RequestBody SuspensionTurnoDTO datos) {
        return turnoSemanalService.suspenderTurno(idTurnoSemanal, datos, usuarioActual.obtenerIdFuncionarioAutenticado());
    }

    /** Qué pasaría si se suspende el turno con esos datos (cantidad de fechas y reservas afectadas). No modifica nada. */
    @GetMapping("/{idTurnoSemanal}/suspension/vista-previa")
    public VistaPreviaSuspensionDTO vistaPreviaSuspension(@PathVariable Long idTurnoSemanal, @Valid SuspensionTurnoDTO datos) {
        return turnoSemanalService.vistaPreviaSuspension(idTurnoSemanal, datos);
    }

    @PostMapping("/{idTurnoSemanal}/rehabilitacion")
    public TurnoSemanalResponseDTO rehabilitarTurno(@PathVariable Long idTurnoSemanal, @Valid @RequestBody RehabilitacionTurnoDTO datos) {
        return turnoSemanalService.rehabilitarTurno(idTurnoSemanal, datos);
    }

    @GetMapping
    public List<TurnoSemanalResponseDTO> buscarTurnosSemanales(CriteriosBusquedaDTO criterios) {
        return turnoSemanalService.buscarTurnosSemanales(criterios);
    }

    @RolesPermitidos(Paciente.class)
    @GetMapping("/ocurrencias-disponibles")
    public List<OcurrenciaTurnoDTO> listarOcurrenciasDisponibles(@RequestParam String codigoEspecialidad) {
        return turnoSemanalService.listarOcurrenciasDisponibles(codigoEspecialidad, usuarioActual.obtenerIdPacienteAutenticado());
    }
}
