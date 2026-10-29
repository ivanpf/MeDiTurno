package com.mediturno.backend.pacientes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.auth.dto.CrearCuentaRequestDTO;
import com.mediturno.backend.common.busqueda.FiltrosBusqueda;
import com.mediturno.backend.common.csv.AnalizadorCsv;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.IdentidadVerificada;
import com.mediturno.backend.common.validation.FormatoUsuario;
import com.mediturno.backend.pacientes.dto.AltaPacienteDTO;
import com.mediturno.backend.pacientes.dto.ModificarPacienteDTO;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.dto.RegistroPacienteDTO;
import com.mediturno.backend.pacientes.dto.VerificacionRegistroDTO;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.usuarios.model.ProveedorAcceso;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-5, CU-7, CU-31, CU-32, CU-33, CU-44.
 * Los métodos que todavía no están implementados lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class PacienteService {

    private static final int LARGO_MINIMO_CONTRASENA = 6; // el mismo mínimo que exige Firebase

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final AdministradorRepository administradorRepository;
    private final Clock reloj;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository, AuthService authService,
            AdministradorRepository administradorRepository, Clock reloj) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.administradorRepository = administradorRepository;
        this.reloj = reloj;
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
        String cedula = FormatoUsuario.normalizarCedula(cedulaIngresada);
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
        String uid = authService.crearCuenta(new CrearCuentaRequestDTO(paciente.getCorreo(), datos.contrasena()));
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
        return convertirAPacienteResponseDTO(paciente);
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
        String cedula = FormatoUsuario.normalizarCedula(datos.cedula());
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        validarFormato(cedula, correo);
        validarCedulaYCorreoUnicos(cedula, correo);

        LocalDateTime ahora = LocalDateTime.now(reloj);
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
        return convertirAPacienteResponseDTO(paciente);
    }

    /** La cédula se guarda siempre con el mismo formato (1.234.567-8) para que la unicidad no dependa de cómo se escriba. */
    private static void validarFormato(String cedula, String correo) {
        List<String> errores = new ArrayList<>();
        if (cedula == null) {
            errores.add("La cédula no tiene un formato válido (ejemplo: 1.234.567-8).");
        }
        if (!FormatoUsuario.correoValido(correo)) {
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

    private static PacienteResponseDTO convertirAPacienteResponseDTO(Paciente p) {
        return new PacienteResponseDTO(p.getIdUsuario(), p.getCedula(), p.getNombre(), p.getApellido(), p.getCorreo(),
            p.getFechaNacimiento(), p.getTelefono(), p.getDomicilio(), p.getEstado(), p.getFirebaseUid() != null, p.getFechaAlta());
    }

    private static final int MAXIMO_FILAS_CSV = 1000;
    private static final String CEDULA = "Cédula";
    private static final String NOMBRE = "Nombre";
    private static final String APELLIDO = "Apellido";
    private static final String CORREO = "Correo";
    private static final String FECHA_NACIMIENTO = "Fecha de nacimiento";
    private static final String TELEFONO = "Teléfono";
    private static final String DOMICILIO = "Domicilio";
    private static final List<AnalizadorCsv.Columna> COLUMNAS_CSV = List.of(
        AnalizadorCsv.Columna.obligatoria(CEDULA), AnalizadorCsv.Columna.obligatoria(NOMBRE),
        AnalizadorCsv.Columna.obligatoria(APELLIDO), AnalizadorCsv.Columna.obligatoria(CORREO),
        AnalizadorCsv.Columna.obligatoria(FECHA_NACIMIENTO),
        AnalizadorCsv.Columna.opcional(TELEFONO), AnalizadorCsv.Columna.opcional(DOMICILIO));
    // La fecha puede venir como 1985-03-27 (ISO) o como 27/03/1985 (la forma habitual en Uruguay).
    private static final DateTimeFormatter FECHA_CON_BARRAS = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /**
     * CU-32, previsualización. Lee el CSV y valida cada fila (campos obligatorios, formatos, largos y cédulas o correos repetidos
     * en el archivo o ya registrados) sin guardar nada. Es el paso previo a la importación.
     */
    @Transactional(readOnly = true)
    public ResultadoImportacionDTO previsualizarPacientesCSV(MultipartFile archivo) {
        return analizarCsv(archivo).resultado();
    }

    /**
     * CU-32. Vuelve a analizar el archivo (nada de lo que mostró la previsualización se da por cierto) y, solo si todas las filas
     * son válidas, incorpora todos los pacientes al padrón en una sola transacción; si alguna tiene errores no se incorpora
     * ninguno (422). Como en el alta individual (CU-31), no se crean cuentas de acceso ni contraseñas: la persona crea la suya
     * después (CU-7). Devuelve el resultado con la cantidad de pacientes incorporados.
     */
    @Transactional
    public ResultadoImportacionDTO altaPacientesCSV(MultipartFile archivo) {
        return analizarCsv(archivo).importar(pacienteRepository::saveAllAndFlush,
            "El archivo contiene errores: corríjalo y vuelva a cargarlo. No se incorporó ningún paciente.",
            // Otro administrador cargó alguno en el mismo instante: las restricciones de la base lo detectan.
            "Algún paciente se registró mientras se importaba el archivo. Vuelva a cargarlo. No se incorporó ningún paciente.");
    }

    private AnalizadorCsv.Analisis<Paciente> analizarCsv(MultipartFile archivo) {
        // Se comparan contra todas las personas del sistema (no solo los pacientes): la cédula y el correo son únicos entre roles.
        Set<String> cedulasRegistradas = new HashSet<>(usuarioRepository.listarCedulas());
        Set<String> correosRegistrados = new HashSet<>(usuarioRepository.listarCorreosEnMinusculas());
        AnalizadorCsv.Repetidos cedulasDelArchivo = new AnalizadorCsv.Repetidos();
        AnalizadorCsv.Repetidos correosDelArchivo = new AnalizadorCsv.Repetidos();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        return AnalizadorCsv.analizar(archivo, MAXIMO_FILAS_CSV, "pacientes", COLUMNAS_CSV, (fila, errores) -> {
            String textoCedula = fila.valor(CEDULA);
            String nombre = fila.valor(NOMBRE);
            String apellido = fila.valor(APELLIDO);
            String textoCorreo = fila.valor(CORREO);
            String textoFecha = fila.valor(FECHA_NACIMIENTO);
            String telefono = fila.valor(TELEFONO);
            String domicilio = fila.valor(DOMICILIO);

            // Lo repetido en el archivo se marca en la segunda aparición, aunque la primera tuviera otros errores.
            String cedula = FormatoUsuario.normalizarCedula(textoCedula);
            String correo = textoCorreo.toLowerCase(Locale.ROOT);
            Integer cedulaPrevia = cedula == null ? null : cedulasDelArchivo.vistoEn(cedula, fila.numero());
            Integer correoPrevio = correosDelArchivo.vistoEn(correo, fila.numero());

            if (textoCedula.isEmpty()) {
                errores.add("La cédula es obligatoria");
            } else if (cedula == null) {
                errores.add("La cédula no tiene un formato válido (ejemplo: 1.234.567-8)");
            } else if (cedulasRegistradas.contains(cedula)) {
                errores.add("La cédula ya está registrada");
            } else if (cedulaPrevia != null) {
                errores.add("La cédula está repetida en el archivo (también en la fila " + cedulaPrevia + ")");
            }
            if (nombre.isEmpty()) {
                errores.add("El nombre es obligatorio");
            } else if (nombre.length() > 100) {
                errores.add("El nombre admite hasta 100 caracteres");
            }
            if (apellido.isEmpty()) {
                errores.add("El apellido es obligatorio");
            } else if (apellido.length() > 100) {
                errores.add("El apellido admite hasta 100 caracteres");
            }
            if (textoCorreo.isEmpty()) {
                errores.add("El correo electrónico es obligatorio");
            } else if (textoCorreo.length() > 255) {
                errores.add("El correo electrónico admite hasta 255 caracteres");
            } else if (!FormatoUsuario.correoValido(textoCorreo)) {
                errores.add("El correo electrónico no tiene un formato válido");
            } else if (correosRegistrados.contains(correo)) {
                errores.add("El correo electrónico ya está registrado");
            } else if (correoPrevio != null) {
                errores.add("El correo electrónico está repetido en el archivo (también en la fila " + correoPrevio + ")");
            }
            LocalDate fechaNacimiento = interpretarFecha(textoFecha);
            if (textoFecha.isEmpty()) {
                errores.add("La fecha de nacimiento es obligatoria");
            } else if (fechaNacimiento == null) {
                errores.add("La fecha de nacimiento no tiene un formato válido (use AAAA-MM-DD o DD/MM/AAAA)");
            } else if (!fechaNacimiento.isBefore(LocalDate.now(reloj))) {
                errores.add("La fecha de nacimiento debe ser anterior a hoy");
            }
            if (telefono.length() > 30) {
                errores.add("El teléfono admite hasta 30 caracteres");
            }
            if (domicilio.length() > 255) {
                errores.add("El domicilio admite hasta 255 caracteres");
            }
            if (!errores.isEmpty()) {
                return null;
            }
            Paciente paciente = new Paciente();
            paciente.setCedula(cedula);
            paciente.setNombre(nombre);
            paciente.setApellido(apellido);
            paciente.setCorreo(correo);
            paciente.setFechaNacimiento(fechaNacimiento);
            paciente.setTelefono(aNullSiEstaVacio(telefono));
            paciente.setDomicilio(aNullSiEstaVacio(domicilio));
            paciente.setEstado(EstadoActivacion.ACTIVO);
            paciente.setFechaCreacion(ahora);
            paciente.setFechaAlta(ahora);
            return paciente;
        });
    }

    /** Fecha en formato AAAA-MM-DD o DD/MM/AAAA; nulo si está vacía o no es una fecha real. */
    private static LocalDate interpretarFecha(String texto) {
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return texto.contains("/") ? LocalDate.parse(texto, FECHA_CON_BARRAS) : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * CU-33. Los criterios son opcionales y se combinan (todos tienen que cumplirse): {@code texto} busca en la cédula (con o sin
     * puntos y guion), el nombre, el apellido y el correo, sin distinguir mayúsculas, y también «apellido nombre» o «nombre
     * apellido»; {@code tieneCuenta} filtra por si el paciente ya creó su cuenta (CU-7); {@code estado}, por ACTIVO o INACTIVO.
     * Se ordena por apellido (por defecto), nombre o fecha de alta (fechaAlta), de forma ascendente salvo que {@code descendente}
     * sea verdadero. Sin criterios devuelve todo el padrón. Nunca incluye datos de la historia clínica.
     */
    @Transactional(readOnly = true)
    public List<PacienteResponseDTO> listarPacientes(CriteriosBusquedaDTO criterios) {
        EstadoActivacion estado = FiltrosBusqueda.interpretarEstado(criterios.estado());
        Sort orden = ordenDeLosPacientes(criterios);
        String patron = FiltrosBusqueda.patronContiene(criterios.texto());
        String digitos = criterios.texto() == null ? "" : criterios.texto().replaceAll("[.\\-\\s]", "");
        String patronCedulaSinPuntos = !digitos.isEmpty() && digitos.chars().allMatch(Character::isDigit) ? "%" + digitos + "%" : null;
        Boolean tieneCuenta = criterios.tieneCuenta();

        Specification<Paciente> filtros = (paciente, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (patron != null) {
                List<Predicate> coincidencias = new ArrayList<>(List.of(
                    cb.like(cb.lower(paciente.get("cedula")), patron, FiltrosBusqueda.ESCAPE),
                    cb.like(cb.lower(paciente.get("nombre")), patron, FiltrosBusqueda.ESCAPE),
                    cb.like(cb.lower(paciente.get("apellido")), patron, FiltrosBusqueda.ESCAPE),
                    cb.like(cb.lower(paciente.get("correo")), patron, FiltrosBusqueda.ESCAPE),
                    cb.like(cb.lower(cb.concat(cb.concat(paciente.get("apellido"), " "), paciente.get("nombre"))), patron, FiltrosBusqueda.ESCAPE),
                    cb.like(cb.lower(cb.concat(cb.concat(paciente.get("nombre"), " "), paciente.get("apellido"))), patron, FiltrosBusqueda.ESCAPE)));
                if (patronCedulaSinPuntos != null) {
                    // La cédula se guarda como 1.234.567-8; quien escribe 1234567 también la encuentra.
                    Expression<String> sinPuntos = cb.function("replace", String.class,
                        cb.function("replace", String.class, paciente.<String>get("cedula"), cb.literal("."), cb.literal("")),
                        cb.literal("-"), cb.literal(""));
                    coincidencias.add(cb.like(sinPuntos, patronCedulaSinPuntos));
                }
                condiciones.add(cb.or(coincidencias.toArray(new Predicate[0])));
            }
            if (tieneCuenta != null) {
                condiciones.add(tieneCuenta ? cb.isNotNull(paciente.get("firebaseUid")) : cb.isNull(paciente.get("firebaseUid")));
            }
            if (estado != null) {
                condiciones.add(cb.equal(paciente.get("estado"), estado));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        return pacienteRepository.findAll(filtros, orden).stream()
            .map(PacienteService::convertirAPacienteResponseDTO)
            .toList();
    }

    /** El criterio pedido y, para que el orden sea siempre el mismo, el desempate: la otra parte del nombre y el id. */
    private static Sort ordenDeLosPacientes(CriteriosBusquedaDTO criterios) {
        Sort principal = FiltrosBusqueda.ordenar(criterios.ordenarPor(), Boolean.TRUE.equals(criterios.descendente()),
            "apellido", "nombre", "fechaAlta");
        Sort desempate = switch (principal.iterator().next().getProperty()) {
            case "apellido" -> Sort.by("nombre", "idUsuario");
            case "nombre" -> Sort.by("apellido", "idUsuario");
            default -> Sort.by("apellido", "nombre", "idUsuario"); // la fecha de alta se repite en una carga masiva
        };
        return principal.and(desempate);
    }

    /** CU-33. Detalle administrativo del paciente: sus datos de contacto y si ya tiene cuenta. No incluye su historia clínica. */
    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerPaciente(Long idPaciente) {
        return pacienteRepository.findById(idPaciente)
            .map(PacienteService::convertirAPacienteResponseDTO)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El paciente no existe"));
    }

    /**
     * CU-44. Corrige nombre, apellido, correo, fecha de nacimiento, teléfono y domicilio (la cédula no se modifica; no toca la
     * historia clínica). Los campos obligatorios los valida el controlador (@Valid). El correo nuevo no puede ser de otra persona
     * (409) y, si el paciente ya tiene cuenta de acceso, se cambia también en ella: si eso falla, no se guarda nada. Si hubo
     * algún cambio, se registra el administrador y la fecha y hora (el último cambio); si no cambió nada, no se guarda nada.
     */
    @Transactional
    public PacienteResponseDTO modificarPaciente(Long idAdministrador, Long idPaciente, ModificarPacienteDTO datos) {
        Paciente paciente = pacienteRepository.buscarConLock(idPaciente)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El paciente no existe"));
        String nombre = datos.nombre().trim();
        String apellido = datos.apellido().trim();
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        String telefono = aNullSiEstaVacio(datos.telefono());
        String domicilio = aNullSiEstaVacio(datos.domicilio());

        if (!FormatoUsuario.correoValido(correo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo electrónico no tiene un formato válido.");
        }
        // Escribir el mismo correo con otras mayúsculas no es un cambio.
        boolean cambiaCorreo = !correo.equalsIgnoreCase(paciente.getCorreo());
        if (cambiaCorreo && usuarioRepository.existsByCorreoIgnoreCaseAndIdUsuarioNot(correo, idPaciente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya pertenece a otra persona");
        }

        boolean hayCambios = cambiaCorreo
            || !nombre.equals(paciente.getNombre())
            || !apellido.equals(paciente.getApellido())
            || !datos.fechaNacimiento().equals(paciente.getFechaNacimiento())
            || !Objects.equals(telefono, paciente.getTelefono())
            || !Objects.equals(domicilio, paciente.getDomicilio());
        if (hayCambios) {
            paciente.setNombre(nombre);
            paciente.setApellido(apellido);
            if (cambiaCorreo) {
                paciente.setCorreo(correo);
            }
            paciente.setFechaNacimiento(datos.fechaNacimiento());
            paciente.setTelefono(telefono);
            paciente.setDomicilio(domicilio);
            paciente.setAdministradorModificacion(administradorRepository.getReferenceById(idAdministrador));
            paciente.setFechaModificacion(LocalDateTime.now(reloj));
            try {
                pacienteRepository.saveAndFlush(paciente);
            } catch (DataIntegrityViolationException e) {
                // Otra persona tomó ese correo en el mismo instante: la restricción de la base lo detecta.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya pertenece a otra persona");
            }
            if (cambiaCorreo && paciente.getFirebaseUid() != null) {
                // Va después de guardar: si falla, la excepción deshace la transacción y el correo no cambia en ningún lado.
                authService.actualizarCorreo(paciente.getFirebaseUid(), correo);
            }
        }
        return convertirAPacienteResponseDTO(paciente);
    }

    /** CU-5. */
    public void actualizarPerfil(Long idPaciente, PerfilDTO datos) {
        throw new NoImplementadoException();
    }
}
