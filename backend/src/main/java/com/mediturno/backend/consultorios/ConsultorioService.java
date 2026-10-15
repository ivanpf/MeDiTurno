package com.mediturno.backend.consultorios;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.consultorios.dto.AltaConsultorioDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.consultorios.dto.ModificarConsultorioDTO;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public ConsultorioResponseDTO altaConsultorio(AltaConsultorioDTO datos) {
        throw new NoImplementadoException();
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
