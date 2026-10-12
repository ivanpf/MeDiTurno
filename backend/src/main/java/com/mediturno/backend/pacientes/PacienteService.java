package com.mediturno.backend.pacientes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.pacientes.dto.AltaPacienteDTO;
import com.mediturno.backend.pacientes.dto.ModificarPacienteDTO;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.dto.RegistroPacienteDTO;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-5, CU-7, CU-31, CU-32, CU-33, CU-44.
 * Los métodos que todavía no están implementados lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class PacienteService {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** CU-7. */
    public PacienteResponseDTO registrarCuentaPaciente(RegistroPacienteDTO datos) {
        throw new NoImplementadoException();
    }

    /**
     * CU-31. Incorpora al paciente al padrón: queda en las tablas usuario y paciente sin firebase_uid, que se completa
     * cuando la persona crea su cuenta (CU-7). Los campos obligatorios los valida el controlador (@Valid).
     */
    public PacienteResponseDTO altaPaciente(AltaPacienteDTO datos) {
        String cedula = normalizarCedula(datos.cedula());
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        validarFormato(cedula, correo);
        validarCedulaYCorreoUnicos(cedula, correo);

        LocalDateTime ahora = LocalDateTime.now();
        Paciente paciente = new Paciente();
        paciente.setCedula(cedula);
        paciente.setNombre(datos.nombre().trim());
        paciente.setApellido(datos.apellido().trim());
        paciente.setCorreo(correo);
        paciente.setFechaNacimiento(datos.fechaNacimiento());
        paciente.setTelefono(aNullSiEstaVacio(datos.telefono()));
        paciente.setDomicilio(aNullSiEstaVacio(datos.domicilio()));
        paciente.setEstado(EstadoActivacion.ACTIVO);
        paciente.setFechaCreacion(ahora);
        paciente.setFechaAlta(ahora);
        try {
            pacienteRepository.saveAndFlush(paciente);
        } catch (DataIntegrityViolationException e) {
            // Otro administrador la cargó en el mismo instante: las restricciones de la base lo detectan.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula o el correo electrónico ya están registrados");
        }
        return aRespuesta(paciente);
    }

    /** La cédula se guarda siempre con el mismo formato (1.234.567-8) para que la unicidad no dependa de cómo se escriba. */
    private static String normalizarCedula(String texto) {
        String limpia = texto.trim();
        if (!limpia.matches("[0-9.\\-\\s]+")) {
            return null;
        }
        String digitos = limpia.replaceAll("[^0-9]", "");
        if (digitos.length() < 7 || digitos.length() > 8) {
            return null;
        }
        String cuerpo = digitos.substring(0, digitos.length() - 1);
        String cuerpoAlReves = new StringBuilder(cuerpo).reverse().toString().replaceAll("(\\d{3})(?=\\d)", "$1.");
        String conPuntos = new StringBuilder(cuerpoAlReves).reverse().toString();
        return conPuntos + "-" + digitos.charAt(digitos.length() - 1);
    }

    private static void validarFormato(String cedula, String correo) {
        List<String> errores = new ArrayList<>();
        if (cedula == null) {
            errores.add("La cédula no tiene un formato válido (ejemplo: 1.234.567-8).");
        }
        if (!CORREO.matcher(correo).matches()) {
            errores.add("El correo electrónico no tiene un formato válido.");
        }
        if (!errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errores));
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

    private static String aNullSiEstaVacio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static PacienteResponseDTO aRespuesta(Paciente p) {
        return new PacienteResponseDTO(p.getIdUsuario(), p.getCedula(), p.getNombre(), p.getApellido(), p.getCorreo(),
            p.getFechaNacimiento(), p.getTelefono(), p.getDomicilio(), p.getEstado(), p.getFirebaseUid() != null, p.getFechaAlta());
    }

    /** CU-32. */
    public ResultadoImportacionDTO altaPacientesCSV(MultipartFile archivo) {
        throw new NoImplementadoException();
    }

    /** CU-33. */
    public List<PacienteResponseDTO> listarPacientes(CriteriosBusquedaDTO criterios) {
        throw new NoImplementadoException();
    }

    /** CU-33. */
    public PacienteResponseDTO obtenerPaciente(Long idPaciente) {
        throw new NoImplementadoException();
    }

    /** CU-44. */
    public PacienteResponseDTO modificarPaciente(Long idPaciente, ModificarPacienteDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-5. */
    public void actualizarPerfil(Long idPaciente, PerfilDTO datos) {
        throw new NoImplementadoException();
    }
}
