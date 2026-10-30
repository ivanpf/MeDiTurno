package com.mediturno.backend.consultorios;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.consultorios.dto.AltaConsultorioDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioDetalleResponseDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.consultorios.dto.ModificarConsultorioDTO;
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
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;

/**
 * Endpoints de: CU-37, CU-38, CU-39, CU-47.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RolesPermitidos(Administrador.class)
@RestController
@RequestMapping("/api/consultorios")
public class ConsultorioController {

    private final ConsultorioService consultorioService;
    private final UsuarioActual usuarioActual;

    public ConsultorioController(ConsultorioService consultorioService, UsuarioActual usuarioActual) {
        this.consultorioService = consultorioService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultorioResponseDTO altaConsultorio(@RequestBody AltaConsultorioDTO datos) {
        return consultorioService.altaConsultorio(datos);
    }

    /** CU-38, paso previo: valida el archivo y muestra lo detectado, sin guardar nada. */
    @PostMapping("/csv/previsualizacion")
    public ResultadoImportacionDTO previsualizarConsultoriosCSV(@RequestParam("archivo") MultipartFile archivo) {
        return consultorioService.previsualizarConsultoriosCSV(archivo);
    }

    @PostMapping("/csv")
    public ResultadoImportacionDTO altaConsultoriosCSV(@RequestParam("archivo") MultipartFile archivo) {
        return consultorioService.altaConsultoriosCSV(archivo);
    }

    @RolesPermitidos({Administrador.class, Funcionario.class})
    @GetMapping
    public List<ConsultorioResponseDTO> listarConsultorios(CriteriosBusquedaDTO criterios) {
        return consultorioService.listarConsultorios(criterios);
    }

    /** CU-39. Detalle con los turnos vigentes que usan el consultorio. */
    @GetMapping("/{codigoConsultorio}/detalle")
    public ConsultorioDetalleResponseDTO obtenerDetalleConsultorio(@PathVariable String codigoConsultorio) {
        return consultorioService.obtenerDetalleConsultorio(codigoConsultorio);
    }

    @PutMapping("/{codigoConsultorio}")
    public ConsultorioResponseDTO modificarConsultorio(@PathVariable String codigoConsultorio, @Valid @RequestBody ModificarConsultorioDTO datos) {
        return consultorioService.modificarConsultorio(usuarioActual.obtenerIdAdministradorAutenticado(), codigoConsultorio, datos);
    }
}
