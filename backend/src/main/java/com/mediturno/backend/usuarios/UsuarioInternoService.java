package com.mediturno.backend.usuarios;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO;
import com.mediturno.backend.usuarios.dto.ModificarUsuarioDTO;
import com.mediturno.backend.usuarios.dto.UsuarioResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lógica de negocio de: CU-5, CU-34, CU-35, CU-36, CU-45, CU-46.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class UsuarioInternoService {

    /** CU-34. */
    public UsuarioResponseDTO altaUsuarioInterno(AltaUsuarioInternoDTO datos) {
        // 1. Validar unicidad de Cédula (Flujo 4.B)
        if (usuarioRepository.existsByCedula(dto.cedula())) {
            throw new ReglaNegocioException("La cédula ya se encuentra asociada a otra cuenta");
        }

        // 2. Validar unicidad de Correo (Flujo 4.B)
        if (usuarioRepository.existsByCorreo(dto.correo())) {
            throw new ReglaNegocioException("El correo ya se encuentra asociado a otra cuenta");
        }

        // 3. Validaciones específicas cuando el Rol es MÉDICO (Flujo 4.A y 4.C)
        if (dto.rol() == RolUsuarioInterno.MEDICO) {
            
            // Flujo 4.A.1: Si es médico, exige al menos una especialidad
            if (dto.especialidadIds() == null || dto.especialidadIds().isEmpty()) {
                throw new ReglaNegocioException("Debe seleccionar al menos una especialidad médica");
            }

            // Flujo 4.C.1: Validar que todas las especialidades existan y estén ACTIVAS
            List<Especialidad> especialidades = especialidadRepository.findAllById(dto.especialidadIds());

            if (especialidades.size() != dto.especialidadIds().size()) {
                throw new ReglaNegocioException("Una o más especialidades seleccionadas no existen");
            }

            boolean hayInactivas = especialidades.stream().anyMatch(e -> !e.isActiva());
            if (hayInactivas) {
                throw new ReglaNegocioException("Una o más especialidades seleccionadas no se encuentran activas");
            }
        }
        
        
        throw new NoImplementadoException();
    }

    /** CU-35. */
    public ResultadoImportacionDTO altaUsuariosInternosCSV(MultipartFile archivo) {
        throw new NoImplementadoException();
    }

    /** CU-36. */
    public List<UsuarioResponseDTO> listarUsuariosInternos(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-45. */
    public UsuarioResponseDTO modificarUsuarioInterno(Long idUsuarioInterno, ModificarUsuarioDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-46. */
    public void activarUsuario(Long idUsuarioInterno) {
        throw new NoImplementadoException();
    }

    /** CU-46. */
    public void inactivarUsuario(Long idUsuarioInterno) {
        throw new NoImplementadoException();
    }

    /** CU-5. */
    public void actualizarPerfil(Long idUsuarioInterno, PerfilDTO datos) {
        throw new NoImplementadoException();
    }
}
