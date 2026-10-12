package com.mediturno.backend.pacientes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.auth.dto.CrearCuentaRequestDTO;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.IdentidadVerificada;
import com.mediturno.backend.pacientes.dto.AltaPacienteDTO;
import com.mediturno.backend.pacientes.dto.ModificarPacienteDTO;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.dto.RegistroPacienteDTO;
import com.mediturno.backend.pacientes.dto.VerificacionRegistroDTO;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.usuarios.model.ProveedorAcceso;
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

    private static final int LARGO_MINIMO_CONTRASENA = 6; // el mismo mínimo que exige Firebase

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository, AuthService authService) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
    }

    /**
     * CU-7. La persona se identifica con la cédula y el correo que figuran en el padrón (CU-31) y no puede tener cuenta.
     * Con correo y contraseña, el backend crea la cuenta en el proveedor de identidad; con Google, verifica el token que
     * el front obtuvo y que la cuenta de Google use ese mismo correo. En los dos casos vincula la cuenta (firebase_uid) a
     * la fila del padrón y guarda los datos de contacto.
     */
    public PacienteResponseDTO registrarCuentaPaciente(RegistroPacienteDTO datos) {
        validarDatosDelMetodoDeRegistro(datos);
        Paciente paciente = buscarPacienteSinCuenta(datos.cedula(), datos.correo());
        return datos.proveedor() == ProveedorAcceso.PASSWORD
            ? registrarConCorreoYContrasena(paciente, datos)
            : registrarConGoogle(paciente, datos);
    }

    /**
     * CU-7. Paso previo para el registro con Google: comprueba la cédula y el correo contra el padrón antes de abrir la
     * ventana de Google, así no se crea una cuenta en Google para quien no puede registrarse.
     */
    public void verificarPacienteEnPadron(VerificacionRegistroDTO datos) {
        buscarPacienteSinCuenta(datos.cedula(), datos.correo());
    }

    /**
     * La cédula y el correo tienen que coincidir con los del padrón. Si no coinciden se responde lo mismo que si la
     * persona no estuviera, para no revelar qué cédulas o correos existen.
     */
    private Paciente buscarPacienteSinCuenta(String cedulaIngresada, String correoIngresado) {
        String cedula = normalizarCedula(cedulaIngresada);
        if (cedula == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula no tiene un formato válido (ejemplo: 1.234.567-8).");
        }
        String correo = correoIngresado.trim();
        Paciente paciente = pacienteRepository.findByCedula(cedula)
            .filter(p -> p.getCorreo().equalsIgnoreCase(correo))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                "No se encontró una persona del padrón con esa cédula y ese correo electrónico. La cuenta no pudo crearse."));
        if (paciente.getFirebaseUid() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta vinculada a esa cédula. La cuenta no pudo crearse.");
        }
        return paciente;
    }

    private PacienteResponseDTO registrarConCorreoYContrasena(Paciente paciente, RegistroPacienteDTO datos) {
        String uid = authService.crearCuenta(new CrearCuentaRequestDTO(paciente.getCorreo(), datos.contrasena())).idProveedor();
        return vincularCuentaOBorrarla(paciente, uid, ProveedorAcceso.PASSWORD, datos);
    }

    private PacienteResponseDTO registrarConGoogle(Paciente paciente, RegistroPacienteDTO datos) {
        IdentidadVerificada identidad = authService.obtenerIdentidadVerificada(datos.tokenAcceso());
        if (usuarioRepository.findByFirebaseUid(identidad.uid()).isPresent()) {
            // Esa cuenta ya es de alguien: no se toca.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa cuenta de Google ya está vinculada a otra persona. La cuenta no pudo crearse.");
        }
        if (identidad.correo() == null || !identidad.correo().trim().equalsIgnoreCase(paciente.getCorreo())) {
            authService.eliminarCuenta(identidad.uid()); // se creó al abrir Google y no le corresponde a nadie
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                "La cuenta de Google tiene que usar el mismo correo electrónico registrado en el padrón. La cuenta no pudo crearse.");
        }
        return vincularCuentaOBorrarla(paciente, identidad.uid(), ProveedorAcceso.GOOGLE, datos);
    }

    /** Si la cuenta no se puede vincular, se borra del proveedor: no debe quedar una cuenta que no es de nadie. */
    private PacienteResponseDTO vincularCuentaOBorrarla(Paciente paciente, String uid, ProveedorAcceso proveedor, RegistroPacienteDTO datos) {
        try {
            return vincularCuenta(paciente, uid, proveedor, datos);
        } catch (RuntimeException e) {
            authService.eliminarCuenta(uid);
            throw e;
        }
    }

    private PacienteResponseDTO vincularCuenta(Paciente paciente, String uid, ProveedorAcceso proveedor, RegistroPacienteDTO datos) {
        paciente.setFirebaseUid(uid);
        paciente.setProveedorAcceso(proveedor);
        if (aNullSiEstaVacio(datos.telefono()) != null) {
            paciente.setTelefono(datos.telefono().trim());
        }
        if (aNullSiEstaVacio(datos.domicilio()) != null) {
            paciente.setDomicilio(datos.domicilio().trim());
        }
        try {
            pacienteRepository.saveAndFlush(paciente);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo vincular la cuenta: ya está registrada.");
        }
        return aRespuesta(paciente);
    }

    /** Lo que exige cada método de registro (los campos comunes los valida @Valid). */
    private static void validarDatosDelMetodoDeRegistro(RegistroPacienteDTO datos) {
        List<String> errores = new ArrayList<>();
        if (datos.proveedor() == ProveedorAcceso.PASSWORD) {
            if (datos.contrasena() == null || datos.contrasena().length() < LARGO_MINIMO_CONTRASENA) {
                errores.add("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
            }
        } else if (datos.tokenAcceso() == null || datos.tokenAcceso().isBlank()) {
            errores.add("Falta iniciar sesión con la cuenta de Google.");
        }
        if (!errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errores));
        }
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
