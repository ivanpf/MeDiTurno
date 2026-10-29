package com.mediturno.backend.pacientes;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.pacientes.dto.AltaPacienteDTO;
import com.mediturno.backend.pacientes.dto.ModificarPacienteDTO;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.dto.RegistroPacienteDTO;
import com.mediturno.backend.pacientes.dto.VerificacionRegistroDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.mediturno.backend.common.seguridad.RolesPermitidos;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.pacientes.model.Paciente;

/**
 * Endpoints de: CU-5, CU-7, CU-31, CU-32, CU-33, CU-44.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Administrador.class)
@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final UsuarioActual usuarioActual;

    public PacienteController(PacienteService pacienteService, UsuarioActual usuarioActual) {
        this.pacienteService = pacienteService;
        this.usuarioActual = usuarioActual;
    }

    @RolesPermitidos
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponseDTO registrarCuentaPaciente(@Valid @RequestBody RegistroPacienteDTO datos) {
        return pacienteService.registrarCuentaPaciente(datos);
    }

    /** Paso previo al registro con Google: comprueba la cédula y el correo contra el padrón. */
    @RolesPermitidos
    @PostMapping("/registro/verificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificarPacienteEnPadron(@Valid @RequestBody VerificacionRegistroDTO datos) {
        pacienteService.verificarPacienteEnPadron(datos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponseDTO altaPaciente(@Valid @RequestBody AltaPacienteDTO datos) {
        return pacienteService.altaPaciente(datos);
    }

    /** CU-32, paso previo: valida el archivo y muestra lo detectado, sin guardar nada. */
    @PostMapping("/csv/previsualizacion")
    public ResultadoImportacionDTO previsualizarPacientesCSV(@RequestParam("archivo") MultipartFile archivo) {
        return pacienteService.previsualizarPacientesCSV(archivo);
    }

    @PostMapping("/csv")
    public ResultadoImportacionDTO altaPacientesCSV(@RequestParam("archivo") MultipartFile archivo) {
        return pacienteService.altaPacientesCSV(archivo);
    }

    @GetMapping
    public List<PacienteResponseDTO> listarPacientes(CriteriosBusquedaDTO criterios) {
        return pacienteService.listarPacientes(criterios);
    }

    @GetMapping("/{idPaciente}")
    public PacienteResponseDTO obtenerPaciente(@PathVariable Long idPaciente) {
        return pacienteService.obtenerPaciente(idPaciente);
    }

    @PutMapping("/{idPaciente}")
    public PacienteResponseDTO modificarPaciente(@PathVariable Long idPaciente, @Valid @RequestBody ModificarPacienteDTO datos) {
        return pacienteService.modificarPaciente(usuarioActual.obtenerIdAdministradorAutenticado(), idPaciente, datos);
    }

    @RolesPermitidos(Paciente.class)
    @PutMapping("/me/perfil")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarPerfil(@RequestBody PerfilDTO datos) {
        pacienteService.actualizarPerfil(usuarioActual.obtenerIdPacienteAutenticado(), datos);
    }
}
