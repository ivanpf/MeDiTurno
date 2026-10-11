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
