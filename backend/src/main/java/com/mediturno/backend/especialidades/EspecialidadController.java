package com.mediturno.backend.especialidades;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.especialidades.dto.AltaEspecialidadDTO;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.dto.ModificarEspecialidadDTO;
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

/**
 * Endpoints de: CU-28, CU-29, CU-30, CU-43.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialidadResponseDTO altaEspecialidad(@RequestBody AltaEspecialidadDTO datos) {
        return especialidadService.altaEspecialidad(datos);
    }

    @PostMapping("/csv")
    public ResultadoImportacionDTO altaEspecialidadesCSV(@RequestParam("archivo") MultipartFile archivo) {
        return especialidadService.altaEspecialidadesCSV(archivo);
    }

    @GetMapping
    public List<EspecialidadResponseDTO> listarEspecialidades(CriteriosBusquedaDTO criterios) {
        return especialidadService.listarEspecialidades(criterios);
    }

    @GetMapping("/{codigoEspecialidad}")
    public EspecialidadResponseDTO obtenerEspecialidad(@PathVariable String codigoEspecialidad) {
        return especialidadService.obtenerEspecialidad(codigoEspecialidad);
    }

    @PutMapping("/{codigoEspecialidad}")
    public EspecialidadResponseDTO modificarEspecialidad(@PathVariable String codigoEspecialidad, @RequestBody ModificarEspecialidadDTO datos) {
        return especialidadService.modificarEspecialidad(codigoEspecialidad, datos);
    }
}
