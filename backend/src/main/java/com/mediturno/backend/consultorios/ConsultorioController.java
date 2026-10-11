package com.mediturno.backend.consultorios;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.consultorios.dto.AltaConsultorioDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.consultorios.dto.ModificarConsultorioDTO;
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
 * Endpoints de: CU-37, CU-38, CU-39, CU-47.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RestController
@RequestMapping("/api/consultorios")
public class ConsultorioController {

    private final ConsultorioService consultorioService;

    public ConsultorioController(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultorioResponseDTO altaConsultorio(@RequestBody AltaConsultorioDTO datos) {
        return consultorioService.altaConsultorio(datos);
    }

    @PostMapping("/csv")
    public ResultadoImportacionDTO altaConsultoriosCSV(@RequestParam("archivo") MultipartFile archivo) {
        return consultorioService.altaConsultoriosCSV(archivo);
    }

    @GetMapping
    public List<ConsultorioResponseDTO> listarConsultorios(CriteriosBusquedaDTO criterios) {
        return consultorioService.listarConsultorios(criterios);
    }

    @PutMapping("/{codigoConsultorio}")
    public ConsultorioResponseDTO modificarConsultorio(@PathVariable String codigoConsultorio, @RequestBody ModificarConsultorioDTO datos) {
        return consultorioService.modificarConsultorio(codigoConsultorio, datos);
    }
}
