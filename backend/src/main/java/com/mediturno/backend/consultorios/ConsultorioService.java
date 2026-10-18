package com.mediturno.backend.consultorios;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.consultorios.dto.AltaConsultorioDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.consultorios.dto.ModificarConsultorioDTO;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de: CU-37, CU-38, CU-39, CU-47.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;

    public ConsultorioService(ConsultorioRepository consultorioRepository) {
        this.consultorioRepository = consultorioRepository;
    }

    /** CU-37. */
    @Transactional
    public ConsultorioResponseDTO altaConsultorio(AltaConsultorioDTO datos) {

        // 1. Validación manual de campos obligatorios
        if (datos.codigo() == null || datos.codigo().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código del consultorio es obligatorio.");
        }
        if (datos.nombre() == null || datos.nombre().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del consultorio es obligatorio.");
        }
        if (datos.ubicacion() == null || datos.ubicacion().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ubicación del consultorio es obligatoria.");
        }
        if (datos.estado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del consultorio es obligatorio.");
        }
        // Nota: La descripción no es obligatoria según tu modelo (nullable = true).

        // 2. Validación de código duplicado (Flujo alternativo 4.B.1)
        if (consultorioRepository.existsById(datos.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ingresado ya corresponde a un consultorio existente.");
        }

        // 3. Mapear DTO a Entidad
        Consultorio consultorio = new Consultorio();
        consultorio.setCodigo(datos.codigo());
        consultorio.setNombre(datos.nombre());
        consultorio.setUbicacion(datos.ubicacion());
        consultorio.setDescripcion(datos.descripcion());
        consultorio.setEstado(datos.estado());

        // 4. Guardar en la base de datos
        Consultorio consultorioGuardado = consultorioRepository.save(consultorio);

        // 5. Devolver ResponseDTO
        return new ConsultorioResponseDTO(
                consultorioGuardado.getCodigo(),
                consultorioGuardado.getNombre(),
                consultorioGuardado.getUbicacion(),
                consultorioGuardado.getDescripcion(),
                consultorioGuardado.getEstado()
        );
    }

    /** CU-38. */
    public ResultadoImportacionDTO altaConsultoriosCSV(MultipartFile archivo) {
        throw new NoImplementadoException();
    }

    /** CU-39. Devuelve todos los consultorios (activos e inactivos) ordenados por nombre, sin búsqueda ni filtros. */
    public List<ConsultorioResponseDTO> listarConsultorios(CriteriosBusquedaDTO criterios) {
        return consultorioRepository.findAll(Sort.by("nombre")).stream()
            .map(c -> new ConsultorioResponseDTO(c.getCodigo(), c.getNombre(), c.getUbicacion(), c.getDescripcion(), c.getEstado()))
            .toList();
    }

    /** CU-47. */
    public ConsultorioResponseDTO modificarConsultorio(String codigoConsultorio, ModificarConsultorioDTO datos) {
        throw new NoImplementadoException();
    }
}
