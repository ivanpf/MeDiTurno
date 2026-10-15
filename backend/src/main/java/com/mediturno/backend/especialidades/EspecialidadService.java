package com.mediturno.backend.especialidades;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.especialidades.dto.AltaEspecialidadDTO;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.dto.ModificarEspecialidadDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-28, CU-29, CU-30, CU-43.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadService(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    /** CU-28. Los campos obligatorios los valida el controlador (@Valid); acá se comprueba la unicidad. */
    public EspecialidadResponseDTO altaEspecialidad(AltaEspecialidadDTO datos) {
        String codigo = datos.codigo().trim();
        String nombre = datos.nombre().trim();
        validarCodigoYNombreUnicos(codigo, nombre);

        Especialidad especialidad = new Especialidad();
        especialidad.setCodigo(codigo);
        especialidad.setNombre(nombre);
        especialidad.setDescripcion(datos.descripcion() == null || datos.descripcion().isBlank() ? null : datos.descripcion().trim());
        especialidad.setRequierePase(datos.requierePase());
        especialidad.setEstado(datos.estado());
        try {
            especialidadRepository.saveAndFlush(especialidad);
        } catch (DataIntegrityViolationException e) {
            // Otro administrador la creó en el mismo instante: las restricciones de la base lo detectan.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código o el nombre ya están en uso por otra especialidad");
        }
        return convertirAEspecialidadResponseDTO(especialidad);
    }

    private void validarCodigoYNombreUnicos(String codigo, String nombre) {
        boolean codigoEnUso = especialidadRepository.existsById(codigo);
        boolean nombreEnUso = especialidadRepository.existsByNombreIgnoreCase(nombre);
        if (codigoEnUso && nombreEnUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código y el nombre ya están en uso por otras especialidades");
        }
        if (codigoEnUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya está en uso por otra especialidad");
        }
        if (nombreEnUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre ya está en uso por otra especialidad");
        }
    }

    private static EspecialidadResponseDTO convertirAEspecialidadResponseDTO(Especialidad e) {
        return new EspecialidadResponseDTO(e.getCodigo(), e.getNombre(), e.getDescripcion(), e.isRequierePase(), e.getEstado());
    }

    /** CU-29. */
    public ResultadoImportacionDTO altaEspecialidadesCSV(MultipartFile archivo) {
        throw new NoImplementadoException();
    }

    /** CU-30. Por ahora devuelve todas las especialidades (activas e inactivas) ordenadas por nombre, sin búsqueda ni filtros. */
    public List<EspecialidadResponseDTO> listarEspecialidades(CriteriosBusquedaDTO criterios) {
        return especialidadRepository.findAll(Sort.by("nombre")).stream().map(EspecialidadService::convertirAEspecialidadResponseDTO).toList();
    }

    /** CU-30. */
    public EspecialidadResponseDTO obtenerEspecialidad(String codigoEspecialidad) {
        return convertirAEspecialidadResponseDTO(especialidadRepository.findById(codigoEspecialidad)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La especialidad no existe")));
    }

    /** CU-43. */
    public EspecialidadResponseDTO modificarEspecialidad(String codigoEspecialidad, ModificarEspecialidadDTO datos) {
        throw new NoImplementadoException();
    }
}
