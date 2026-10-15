package com.mediturno.backend.usuarios;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.auth.dto.CrearCuentaRequestDTO;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.validation.FormatoUsuario;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import com.mediturno.backend.notificaciones.EmailService;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO.RolUsuarioInterno;
import com.mediturno.backend.usuarios.dto.ModificarUsuarioDTO;
import com.mediturno.backend.usuarios.dto.UsuarioResponseDTO;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.ProveedorAcceso;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.repository.MedicoRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lógica de negocio de: CU-5, CU-34, CU-35, CU-36, CU-45, CU-46.
 * Los métodos que todavía no están implementados lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class UsuarioInternoService {

    private static final SecureRandom GENERADOR = new SecureRandom();
    private static final String CARACTERES_CONTRASENA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final AuthService authService;
    private final EmailService emailService;

    public UsuarioInternoService(UsuarioRepository usuarioRepository, EspecialidadRepository especialidadRepository,
            MedicoRepository medicoRepository, AuthService authService, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.authService = authService;
        this.emailService = emailService;
    }

    /** CU-34. Registra un usuario interno activo y, si es médico, sus especialidades. */
    @Transactional
    public UsuarioResponseDTO altaUsuarioInterno(AltaUsuarioInternoDTO datos) {
        if (datos.rol() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }
        String cedula = FormatoUsuario.normalizarCedula(datos.cedula());
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        validarFormato(cedula, correo);
        validarCedulaYCorreoUnicos(cedula, correo);

        List<Especialidad> especialidades = List.of();
        String matricula = null;
        if (datos.rol() == RolUsuarioInterno.MEDICO) {
            matricula = validarMatricula(datos.matricula());
            validarMatriculaUnica(matricula);
            especialidades = validarEspecialidades(datos.especialidadIds());
        }

        Usuario usuario = nuevoUsuario(datos.rol());
        usuario.setCedula(cedula);
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setCorreo(correo);
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setEstado(EstadoActivacion.ACTIVO);

        if (usuario instanceof Medico medico) {
            medico.setMatricula(matricula);
            medico.setEspecialidades(new HashSet<>(especialidades));
        }

        String contrasenaTemporal = generarContrasenaTemporal();
        String uid = authService.crearCuenta(new CrearCuentaRequestDTO(correo, contrasenaTemporal));
        usuario.setFirebaseUid(uid);
        usuario.setProveedorAcceso(ProveedorAcceso.PASSWORD);
        try {
            Usuario guardado = usuarioRepository.saveAndFlush(usuario);
            emailService.enviarCredencialesTemporales(correo, contrasenaTemporal);
            return aRespuesta(guardado);
        } catch (RuntimeException e) {
            authService.eliminarCuenta(uid);
            if (e instanceof DataIntegrityViolationException) {
                // Las restricciones de la base cubren duplicados creados concurrentemente.
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La cédula, el correo o la matrícula ya se encuentran asociados a otra cuenta");
            }
            throw e;
        }
    }

    private static String generarContrasenaTemporal() {
        char[] contrasena = new char[18];
        for (int i = 0; i < contrasena.length; i++) {
            contrasena[i] = CARACTERES_CONTRASENA.charAt(GENERADOR.nextInt(CARACTERES_CONTRASENA.length()));
        }
        return new String(contrasena);
    }

    private static Usuario nuevoUsuario(RolUsuarioInterno rol) {
        return switch (rol) {
            case ADMINISTRADOR -> new Administrador();
            case FUNCIONARIO -> new Funcionario();
            case MEDICO -> new Medico();
        };
    }

    private static void validarFormato(String cedula, String correo) {
        if (cedula == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cédula no tiene un formato válido (ejemplo: 1.234.567-8).");
        }
        if (!FormatoUsuario.correoValido(correo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo electrónico no tiene un formato válido.");
        }
    }

    private void validarCedulaYCorreoUnicos(String cedula, String correo) {
        boolean cedulaRegistrada = usuarioRepository.existsByCedula(cedula);
        boolean correoRegistrado = usuarioRepository.existsByCorreoIgnoreCase(correo);
        if (cedulaRegistrada && correoRegistrado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula y el correo electrónico ya están registrados");
        }
        if (cedulaRegistrada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada");
        }
        if (correoRegistrado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya está registrado");
        }
    }

    private String validarMatricula(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La matrícula es obligatoria para los médicos");
        }
        return valor.trim();
    }

    private void validarMatriculaUnica(String matricula) {
        if (medicoRepository.existsByMatriculaIgnoreCase(matricula)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La matrícula ya está registrada");
        }
    }

    private List<Especialidad> validarEspecialidades(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar al menos una especialidad médica");
        }
        if (ids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las especialidades seleccionadas no son válidas");
        }

        Set<String> idsUnicos = new LinkedHashSet<>(ids);
        List<Especialidad> encontradas = especialidadRepository.findAllById(idsUnicos);
        Set<String> codigosEncontrados = encontradas.stream().map(Especialidad::getCodigo).collect(Collectors.toSet());
        List<String> inexistentes = idsUnicos.stream().filter(id -> !codigosEncontrados.contains(id)).toList();
        if (!inexistentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No existen las especialidades: " + String.join(", ", inexistentes));
        }

        List<String> inactivas = encontradas.stream()
                .filter(especialidad -> especialidad.getEstado() != EstadoActivacion.ACTIVO)
                .map(Especialidad::getCodigo)
                .toList();
        if (!inactivas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No están activas las especialidades: " + String.join(", ", inactivas));
        }
        return encontradas;
    }

    private static UsuarioResponseDTO aRespuesta(Usuario usuario) {
        RolUsuarioInterno rol;
        String matricula = null;
        List<String> especialidadIds = List.of();
        if (usuario instanceof Medico medico) {
            rol = RolUsuarioInterno.MEDICO;
            matricula = medico.getMatricula();
            especialidadIds = medico.getEspecialidades().stream()
                    .map(Especialidad::getCodigo)
                    .sorted()
                    .toList();
        } else if (usuario instanceof Funcionario) {
            rol = RolUsuarioInterno.FUNCIONARIO;
        } else {
            rol = RolUsuarioInterno.ADMINISTRADOR;
        }
        return new UsuarioResponseDTO(usuario.getIdUsuario(), usuario.getCedula(), usuario.getNombre(),
                usuario.getApellido(), usuario.getCorreo(), usuario.getFechaNacimiento(), usuario.getEstado(),
                rol, matricula, especialidadIds, usuario.getFechaCreacion());
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
