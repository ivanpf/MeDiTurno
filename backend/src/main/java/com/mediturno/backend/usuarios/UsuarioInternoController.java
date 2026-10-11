package com.mediturno.backend.usuarios;

import java.util.List;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.seguridad.UsuarioActual;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO;
import com.mediturno.backend.usuarios.dto.ModificarUsuarioDTO;
import com.mediturno.backend.usuarios.dto.UsuarioResponseDTO;
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
 * Endpoints de: CU-5, CU-34, CU-35, CU-36, CU-45, CU-46.
 * Las rutas son las de los diagramas de secuencia; donde el diagrama no la indica, es una propuesta.
 */
@RestController
@RequestMapping("/api/usuarios-internos")
public class UsuarioInternoController {

    private final UsuarioInternoService usuarioInternoService;
    private final UsuarioActual usuarioActual;

    public UsuarioInternoController(UsuarioInternoService usuarioInternoService, UsuarioActual usuarioActual) {
        this.usuarioInternoService = usuarioInternoService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO altaUsuarioInterno(@RequestBody AltaUsuarioInternoDTO datos) {
        return usuarioInternoService.altaUsuarioInterno(datos);
    }

    @PostMapping("/csv")
    public ResultadoImportacionDTO altaUsuariosInternosCSV(@RequestParam("archivo") MultipartFile archivo) {
        return usuarioInternoService.altaUsuariosInternosCSV(archivo);
    }

    @GetMapping
    public List<UsuarioResponseDTO> listarUsuariosInternos(CriteriosBusquedaDTO criterios) {
        return usuarioInternoService.listarUsuariosInternos(criterios);
    }

    @PutMapping("/{idUsuarioInterno}")
    public UsuarioResponseDTO modificarUsuarioInterno(@PathVariable Long idUsuarioInterno, @RequestBody ModificarUsuarioDTO datos) {
        return usuarioInternoService.modificarUsuarioInterno(idUsuarioInterno, datos);
    }

    @PostMapping("/{idUsuarioInterno}/activacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activarUsuario(@PathVariable Long idUsuarioInterno) {
        usuarioInternoService.activarUsuario(idUsuarioInterno);
    }

    @PostMapping("/{idUsuarioInterno}/inactivacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inactivarUsuario(@PathVariable Long idUsuarioInterno) {
        usuarioInternoService.inactivarUsuario(idUsuarioInterno);
    }

    @PutMapping("/me/perfil")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarPerfil(@RequestBody PerfilDTO datos) {
        usuarioInternoService.actualizarPerfil(usuarioActual.obtenerIdUsuarioAutenticado(), datos);
    }
}
