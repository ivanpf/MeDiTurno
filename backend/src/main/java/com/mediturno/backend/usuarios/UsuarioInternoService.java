package com.mediturno.backend.usuarios;

import java.security.SecureRandom;
import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.auth.dto.CrearCuentaRequestDTO;
import com.mediturno.backend.common.busqueda.FiltrosBusqueda;
import com.mediturno.backend.common.csv.AnalizadorCsv;
import com.mediturno.backend.common.csv.LectorCsv;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.validation.FormatoUsuario;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import com.mediturno.backend.notificaciones.EmailService;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO;
import com.mediturno.backend.usuarios.dto.AltaUsuarioInternoDTO.RolUsuarioInterno;
import com.mediturno.backend.usuarios.dto.ModificarUsuarioDTO;
import com.mediturno.backend.usuarios.dto.UsuarioResponseDTO;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.ProveedorAcceso;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.model.UsuarioInterno;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;
import com.mediturno.backend.usuarios.repository.MedicoRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lógica de negocio de: CU-5, CU-34, CU-35, CU-36, CU-45, CU-46.
 * Los métodos que todavía no están implementados lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class UsuarioInternoService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioInternoService.class);
    private static final SecureRandom GENERADOR = new SecureRandom();
    private static final String CARACTERES_CONTRASENA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final AdministradorRepository administradorRepository;
    private final TurnoSemanalRepository turnoSemanalRepository;
    private final AuthService authService;
    private final EmailService emailService;
    private final Clock reloj;

    public UsuarioInternoService(UsuarioRepository usuarioRepository, EspecialidadRepository especialidadRepository,
            MedicoRepository medicoRepository, AdministradorRepository administradorRepository,
            TurnoSemanalRepository turnoSemanalRepository, AuthService authService, EmailService emailService, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.administradorRepository = administradorRepository;
        this.turnoSemanalRepository = turnoSemanalRepository;
        this.authService = authService;
        this.emailService = emailService;
        this.reloj = reloj;
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
            especialidades = validarEspecialidades(datos.especialidadIds(), Set.of());
        }

        Usuario usuario = nuevoUsuario(datos.rol());
        usuario.setCedula(cedula);
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setCorreo(correo);
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setFechaCreacion(LocalDateTime.now(reloj));
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
            return convertirAUsuarioResponseDTO(guardado);
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

    /**
     * Las especialidades seleccionadas tienen que existir y estar activas. {@code yaAsignadas} son los códigos que el médico
     * ya tenía (CU-45): esas se aceptan aunque se hayan inactivado después, para poder corregir otros datos sin quitarlas.
     */
    private List<Especialidad> validarEspecialidades(List<String> ids, Set<String> yaAsignadas) {
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
                .filter(especialidad -> !yaAsignadas.contains(especialidad.getCodigo()))
                .map(Especialidad::getCodigo)
                .toList();
        if (!inactivas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No están activas las especialidades: " + String.join(", ", inactivas));
        }
        return encontradas;
    }

    /** Un usuario interno, o null si es un paciente. */
    private static RolUsuarioInterno rolDe(Usuario usuario) {
        if (usuario instanceof Medico) {
            return RolUsuarioInterno.MEDICO;
        }
        if (usuario instanceof Funcionario) {
            return RolUsuarioInterno.FUNCIONARIO;
        }
        if (usuario instanceof Administrador) {
            return RolUsuarioInterno.ADMINISTRADOR;
        }
        return null;
    }

    private static UsuarioResponseDTO convertirAUsuarioResponseDTO(Usuario usuario) {
        String matricula = null;
        List<UsuarioResponseDTO.EspecialidadAsignada> especialidades = List.of();
        if (usuario instanceof Medico medico) {
            matricula = medico.getMatricula();
            especialidades = medico.getEspecialidades().stream()
                    .sorted(Comparator.comparing(Especialidad::getNombre, String.CASE_INSENSITIVE_ORDER))
                    .map(e -> new UsuarioResponseDTO.EspecialidadAsignada(e.getCodigo(), e.getNombre(), e.getEstado()))
                    .toList();
        }
        return new UsuarioResponseDTO(usuario.getIdUsuario(), usuario.getCedula(), usuario.getNombre(),
                usuario.getApellido(), usuario.getCorreo(), usuario.getFechaNacimiento(), usuario.getEstado(),
                rolDe(usuario), matricula, especialidades, usuario.getFechaCreacion());
    }

    /** El usuario interno con ese id; 404 si no existe o es un paciente (los pacientes no se gestionan desde acá). */
    private Usuario cargarUsuarioInterno(Long id) {
        return usuarioRepository.findById(id)
                .filter(usuario -> !(usuario instanceof Paciente))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
    }

    /** Bloquea a la persona hasta el fin de la transacción y la devuelve; 404 si no existe o es un paciente. */
    private Usuario cargarUsuarioInternoConLock(Long id) {
        usuarioRepository.bloquear(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
        return cargarUsuarioInterno(id);
    }

    private void registrarCambio(UsuarioInterno usuario, Long idAdministrador) {
        usuario.setAdministradorModificacion(administradorRepository.getReferenceById(idAdministrador));
        usuario.setFechaModificacion(LocalDateTime.now(reloj));
    }

    // ------------------------------------------------------------------ CU-36

    /**
     * CU-36. Los criterios son opcionales y se combinan (todos tienen que cumplirse): {@code texto} busca en la cédula (con o
     * sin puntos y guion), el nombre, el apellido y el correo, sin distinguir mayúsculas, y también «apellido nombre» o «nombre
     * apellido»; {@code rol} (ADMINISTRADOR, FUNCIONARIO o MEDICO); {@code estado} (ACTIVO o INACTIVO) y
     * {@code codigoEspecialidad}, para los médicos que la atienden. Se ordena por apellido (por defecto), nombre, rol o fecha de
     * creación (fechaCreacion), de forma ascendente salvo que {@code descendente} sea verdadero. Sin criterios devuelve todas
     * las cuentas de administradores, funcionarios y médicos; nunca las de pacientes.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuariosInternos(CriteriosBusquedaDTO criterios) {
        EstadoActivacion estado = FiltrosBusqueda.interpretarEstado(criterios.estado());
        RolUsuarioInterno rol = interpretarRol(criterios.rol());
        Comparator<Usuario> orden = ordenDeLosUsuarios(criterios);
        String codigoEspecialidad = criterios.codigoEspecialidad() == null || criterios.codigoEspecialidad().isBlank()
                ? null : criterios.codigoEspecialidad().trim();

        Specification<Usuario> filtros = (usuario, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            Class<? extends Usuario> clase = rol == null ? null : claseDe(rol);
            condiciones.add(clase == null
                    ? usuario.type().in(Administrador.class, Funcionario.class, Medico.class)
                    : usuario.type().in(clase));
            Predicate texto = FiltrosBusqueda.coincidenciasDePersona(usuario, cb, criterios.texto());
            if (texto != null) {
                condiciones.add(texto);
            }
            if (estado != null) {
                condiciones.add(cb.equal(usuario.get("estado"), estado));
            }
            if (codigoEspecialidad != null) {
                Subquery<Long> atiende = consulta.subquery(Long.class);
                Root<Medico> medico = atiende.from(Medico.class);
                atiende.select(medico.<Long>get("idUsuario")).where(
                        cb.equal(medico.get("idUsuario"), usuario.get("idUsuario")),
                        cb.equal(medico.join("especialidades").get("codigo"), codigoEspecialidad));
                condiciones.add(cb.exists(atiende));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        return usuarioRepository.findAll(filtros).stream()
                .sorted(orden)
                .map(UsuarioInternoService::convertirAUsuarioResponseDTO)
                .toList();
    }

    private static Class<? extends Usuario> claseDe(RolUsuarioInterno rol) {
        return switch (rol) {
            case ADMINISTRADOR -> Administrador.class;
            case FUNCIONARIO -> Funcionario.class;
            case MEDICO -> Medico.class;
        };
    }

    /** ADMINISTRADOR, FUNCIONARIO o MEDICO (sin distinguir mayúsculas); {@code null} si no se indicó; 400 si es otra cosa. */
    private static RolUsuarioInterno interpretarRol(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return RolUsuarioInterno.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El rol indicado no es válido (use ADMINISTRADOR, FUNCIONARIO o MEDICO).");
        }
    }

    /**
     * El criterio pedido y, para que el orden sea siempre el mismo, el desempate por el resto del nombre y el id. Los nombres se
     * comparan sin distinguir mayúsculas ni tildes, como los ordenaría una persona.
     */
    private static Comparator<Usuario> ordenDeLosUsuarios(CriteriosBusquedaDTO criterios) {
        String[] permitidas = {"apellido", "nombre", "rol", "fechaCreacion"};
        String pedido = criterios.ordenarPor() == null || criterios.ordenarPor().isBlank() ? permitidas[0] : Arrays.stream(permitidas)
                .filter(p -> p.equalsIgnoreCase(criterios.ordenarPor().trim()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El orden indicado no es válido (use apellido, nombre, rol o fechaCreacion)."));
        Collator collator = Collator.getInstance(Locale.forLanguageTag("es-UY"));
        collator.setStrength(Collator.PRIMARY);
        Comparator<Usuario> porApellido = Comparator.comparing(Usuario::getApellido, collator);
        Comparator<Usuario> porNombre = Comparator.comparing(Usuario::getNombre, collator);
        Comparator<Usuario> porId = Comparator.comparing(Usuario::getIdUsuario);

        Comparator<Usuario> principal = switch (pedido) {
            case "nombre" -> porNombre;
            case "rol" -> Comparator.comparing(u -> rolDe(u).ordinal());
            case "fechaCreacion" -> Comparator.comparing(Usuario::getFechaCreacion);
            default -> porApellido;
        };
        if (Boolean.TRUE.equals(criterios.descendente())) {
            principal = principal.reversed();
        }
        Comparator<Usuario> desempate = switch (pedido) {
            case "nombre" -> porApellido.thenComparing(porId);
            case "apellido" -> porNombre.thenComparing(porId);
            default -> porApellido.thenComparing(porNombre).thenComparing(porId);
        };
        return principal.thenComparing(desempate);
    }

    /** CU-36. Detalle de un usuario interno: sus datos y, si es médico, la matrícula y las especialidades asociadas. */
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerUsuarioInterno(Long idUsuarioInterno) {
        return convertirAUsuarioResponseDTO(cargarUsuarioInterno(idUsuarioInterno));
    }

    // ------------------------------------------------------------------ CU-46

    /**
     * CU-46. Activa una cuenta inactiva. Si ya estaba activa, 409. Registra el administrador y la fecha y hora del cambio.
     */
    @Transactional
    public void activarUsuario(Long idAdministrador, Long idUsuarioInterno) {
        cambiarEstado(idAdministrador, idUsuarioInterno, EstadoActivacion.ACTIVO);
    }

    /**
     * CU-46. Inactiva una cuenta activa: la persona no puede volver a iniciar sesión (el filtro de autenticación rechaza las
     * cuentas inactivas en la siguiente llamada), pero se conservan sus reservas, órdenes y registros clínicos. 409 si ya estaba
     * inactiva, si es la cuenta de quien lo pide, si es el último administrador activo o si es un médico con turnos de atención
     * vigentes. Registra el administrador y la fecha y hora del cambio.
     */
    @Transactional
    public void inactivarUsuario(Long idAdministrador, Long idUsuarioInterno) {
        cambiarEstado(idAdministrador, idUsuarioInterno, EstadoActivacion.INACTIVO);
    }

    private void cambiarEstado(Long idAdministrador, Long idUsuarioInterno, EstadoActivacion nuevoEstado) {
        Usuario usuario = cargarUsuarioInternoConLock(idUsuarioInterno);
        if (usuario.getEstado() == nuevoEstado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    nuevoEstado == EstadoActivacion.ACTIVO ? "La cuenta ya está activa" : "La cuenta ya está inactiva");
        }
        if (nuevoEstado == EstadoActivacion.INACTIVO) {
            if (idUsuarioInterno.equals(idAdministrador)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No puede inactivar su propia cuenta");
            }
            if (usuario instanceof Administrador) {
                validarQueQuedaOtroAdministradorActivo("No se puede inactivar al último administrador activo");
            }
            if (usuario instanceof Medico && tieneTurnosVigentes(idUsuarioInterno)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede inactivar al médico mientras tenga turnos de atención vigentes");
            }
        }
        usuario.setEstado(nuevoEstado);
        registrarCambio((UsuarioInterno) usuario, idAdministrador);
        usuarioRepository.saveAndFlush(usuario);
    }

    /**
     * Para el administrador activo que se está por inactivar o dejar de ser administrador: tiene que haber otro. Bloquea a todos
     * los administradores antes de contar, así dos que se inactivan entre sí a la vez no dejan el sistema sin ninguno.
     */
    private void validarQueQuedaOtroAdministradorActivo(String mensaje) {
        usuarioRepository.bloquearAdministradores();
        if (administradorRepository.countByEstado(EstadoActivacion.ACTIVO) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
        }
    }

    private boolean tieneTurnosVigentes(Long idMedico) {
        return !turnoSemanalRepository.listarVigentesDeMedico(idMedico, LocalDate.now(reloj)).isEmpty();
    }

    // ------------------------------------------------------------------ CU-45

    /**
     * CU-45. Corrige el nombre, el apellido, el correo y el rol de un usuario interno y, si es médico, la matrícula y las
     * especialidades (la cédula no se modifica). Los campos obligatorios los valida el controlador (@Valid).
     * <ul>
     *   <li>El correo y la matrícula no pueden ser de otra persona (409) y las especialidades nuevas tienen que existir y estar
     *       activas (400). Si la persona tiene cuenta de acceso, el correo se cambia también en ella: si eso falla, no se guarda
     *       nada.</li>
     *   <li>No se le puede quitar a un médico una especialidad en la que tiene turnos vigentes (409).</li>
     *   <li>Rol: no se puede retirar el rol de médico con turnos vigentes ni dejar al sistema sin administrador activo, y nadie
     *       cambia su propio rol (409). Tampoco se puede si la persona tiene actividad registrada con el rol que deja (turnos,
     *       órdenes, consultas, cambios hechos a otras cuentas): esa actividad histórica se conserva, y para impedir que siga
     *       usando el sistema se inactiva la cuenta (409).</li>
     *   <li>Si hubo algún cambio, se registra el administrador y la fecha y hora (el último cambio); si no cambió nada, no se
     *       guarda nada.</li>
     * </ul>
     */
    @Transactional
    public UsuarioResponseDTO modificarUsuarioInterno(Long idAdministrador, Long idUsuarioInterno, ModificarUsuarioDTO datos) {
        Usuario usuario = cargarUsuarioInternoConLock(idUsuarioInterno);
        RolUsuarioInterno rolActual = rolDe(usuario);
        RolUsuarioInterno rolNuevo = datos.rol();
        String nombre = datos.nombre().trim();
        String apellido = datos.apellido().trim();
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);

        if (!FormatoUsuario.correoValido(correo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo electrónico no tiene un formato válido.");
        }
        // Escribir el mismo correo con otras mayúsculas no es un cambio.
        boolean cambiaCorreo = !correo.equalsIgnoreCase(usuario.getCorreo());
        if (cambiaCorreo && usuarioRepository.existsByCorreoIgnoreCaseAndIdUsuarioNot(correo, idUsuarioInterno)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya pertenece a otra persona");
        }

        String matricula = null;
        List<Especialidad> especialidades = List.of();
        if (rolNuevo == RolUsuarioInterno.MEDICO) {
            Medico medicoActual = usuario instanceof Medico m ? m : null;
            Set<String> yaAsignadas = medicoActual == null ? Set.of() : medicoActual.getEspecialidades().stream()
                    .map(Especialidad::getCodigo).collect(Collectors.toSet());
            matricula = validarMatricula(datos.matricula());
            if (medicoRepository.existsByMatriculaIgnoreCaseAndIdUsuarioNot(matricula, idUsuarioInterno)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La matrícula ya pertenece a otro médico");
            }
            especialidades = validarEspecialidades(datos.especialidadIds(), yaAsignadas);
            if (medicoActual != null) {
                validarQueNoSeQuitanEspecialidadesConTurnos(idUsuarioInterno, yaAsignadas, especialidades);
            }
        }

        boolean cambiaRol = rolActual != rolNuevo;
        if (cambiaRol) {
            validarCambioDeRol(idAdministrador, usuario, rolActual);
        }
        boolean hayCambios = cambiaRol || cambiaCorreo
                || !nombre.equals(usuario.getNombre())
                || !apellido.equals(usuario.getApellido())
                || (usuario instanceof Medico m && rolNuevo == RolUsuarioInterno.MEDICO
                        && (!matricula.equals(m.getMatricula()) || !codigos(m.getEspecialidades()).equals(codigos(especialidades))));
        if (!hayCambios) {
            return convertirAUsuarioResponseDTO(usuario);
        }

        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        if (cambiaCorreo) {
            usuario.setCorreo(correo);
        }
        try {
            if (cambiaRol) {
                usuarioRepository.saveAndFlush(usuario);
                usuario = cambiarDeRol(idUsuarioInterno, rolActual, rolNuevo, matricula);
            }
            if (usuario instanceof Medico medico) {
                medico.setMatricula(matricula);
                medico.setEspecialidades(new HashSet<>(especialidades));
            }
            registrarCambio((UsuarioInterno) usuario, idAdministrador);
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Otra persona tomó ese correo o esa matrícula en el mismo instante: las restricciones de la base lo detectan.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico o la matrícula ya pertenecen a otra persona");
        }
        if (cambiaCorreo && usuario.getFirebaseUid() != null) {
            // Va después de guardar: si falla, la excepción deshace la transacción y el correo no cambia en ningún lado.
            authService.actualizarCorreo(usuario.getFirebaseUid(), correo);
        }
        return convertirAUsuarioResponseDTO(usuario);
    }

    private static Set<String> codigos(Collection<Especialidad> especialidades) {
        return especialidades.stream().map(Especialidad::getCodigo).collect(Collectors.toSet());
    }

    private void validarQueNoSeQuitanEspecialidadesConTurnos(Long idMedico, Set<String> yaAsignadas, List<Especialidad> nuevas) {
        Set<String> quitadas = new HashSet<>(yaAsignadas);
        quitadas.removeAll(codigos(nuevas));
        if (quitadas.isEmpty()) {
            return;
        }
        List<String> conTurnos = turnoSemanalRepository.listarVigentesDeMedico(idMedico, LocalDate.now(reloj)).stream()
                .map(TurnoSemanal::getEspecialidad)
                .map(Especialidad::getCodigo)
                .filter(quitadas::contains)
                .distinct()
                .sorted()
                .toList();
        if (!conTurnos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pueden quitar las especialidades " + String.join(", ", conTurnos)
                            + " mientras el médico tenga turnos de atención vigentes en ellas");
        }
    }

    private void validarCambioDeRol(Long idAdministrador, Usuario usuario, RolUsuarioInterno rolActual) {
        if (usuario.getIdUsuario().equals(idAdministrador)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No puede cambiar su propio rol: el sistema debe conservar siempre un administrador activo");
        }
        if (rolActual == RolUsuarioInterno.MEDICO && tieneTurnosVigentes(usuario.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede retirar el rol de médico mientras tenga turnos de atención vigentes");
        }
        if (rolActual == RolUsuarioInterno.ADMINISTRADOR && usuario.getEstado() == EstadoActivacion.ACTIVO) {
            validarQueQuedaOtroAdministradorActivo("No se puede retirar el rol de administrador al último administrador activo");
        }
    }

    /**
     * Pasa a la persona de la tabla de su rol a la del nuevo y devuelve la persona ya cargada con su nuevo rol. Si tiene actividad
     * registrada con el rol que deja, la base impide borrar esa fila y se responde 409: la actividad histórica se conserva.
     */
    private Usuario cambiarDeRol(Long id, RolUsuarioInterno rolActual, RolUsuarioInterno rolNuevo, String matricula) {
        try {
            switch (rolActual) {
                case MEDICO -> {
                    usuarioRepository.borrarEspecialidadesDeMedico(id);
                    usuarioRepository.borrarRolMedico(id);
                }
                case FUNCIONARIO -> usuarioRepository.borrarRolFuncionario(id);
                case ADMINISTRADOR -> usuarioRepository.borrarRolAdministrador(id);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el rol: la persona tiene actividad registrada con su rol actual (turnos, órdenes, "
                            + "consultas u otros registros). Para que no pueda usar el sistema, inactive la cuenta.");
        }
        switch (rolNuevo) {
            case MEDICO -> usuarioRepository.agregarRolMedico(id, matricula);
            case FUNCIONARIO -> usuarioRepository.agregarRolFuncionario(id);
            case ADMINISTRADOR -> usuarioRepository.agregarRolAdministrador(id);
        }
        return cargarUsuarioInterno(id);
    }

    // ------------------------------------------------------------------ CU-35

    private static final int MAXIMO_FILAS_CSV = 100; // cada fila crea una cuenta en Firebase y manda un correo
    private static final String CEDULA = "Cédula";
    private static final String NOMBRE = "Nombre";
    private static final String APELLIDO = "Apellido";
    private static final String CORREO = "Correo";
    private static final String FECHA_NACIMIENTO = "Fecha de nacimiento";
    private static final String ROL = "Rol";
    private static final String MATRICULA = "Matrícula";
    private static final String ESPECIALIDADES = "Especialidades";
    private static final List<AnalizadorCsv.Columna> COLUMNAS_CSV = List.of(
            AnalizadorCsv.Columna.obligatoria(CEDULA), AnalizadorCsv.Columna.obligatoria(NOMBRE),
            AnalizadorCsv.Columna.obligatoria(APELLIDO), AnalizadorCsv.Columna.obligatoria(CORREO),
            AnalizadorCsv.Columna.obligatoria(FECHA_NACIMIENTO), AnalizadorCsv.Columna.obligatoria(ROL),
            AnalizadorCsv.Columna.opcional(MATRICULA), AnalizadorCsv.Columna.opcional(ESPECIALIDADES));

    /**
     * CU-35, previsualización. Lee el CSV y valida cada fila (campos obligatorios, formatos, roles, y cédulas, correos o matrículas
     * repetidos en el archivo o ya registrados, y especialidades inexistentes o inactivas) sin guardar nada.
     */
    @Transactional(readOnly = true)
    public ResultadoImportacionDTO previsualizarUsuariosInternosCSV(MultipartFile archivo) {
        return analizarCsv(archivo).resultado();
    }

    /**
     * CU-35. Vuelve a analizar el archivo y, solo si todas las filas son válidas, crea todas las cuentas activas: la identidad de
     * cada persona en el proveedor de autenticación con una contraseña temporal, el registro en la base y el correo con las
     * credenciales. Si algo falla en el camino (422 por errores, 409 si otra persona registró uno en ese instante, 503 si el
     * proveedor o el correo no responden) no se crea ninguna cuenta y se borran las identidades que se habían creado.
     */
    @Transactional
    public ResultadoImportacionDTO altaUsuariosInternosCSV(MultipartFile archivo) {
        return analizarCsv(archivo).importar(this::crearCuentasYGuardar,
                "El archivo contiene errores: corríjalo y vuelva a cargarlo. No se creó ninguna cuenta.",
                "Alguna cédula, correo o matrícula se registró mientras se importaba el archivo. Vuelva a cargarlo. No se creó ninguna cuenta.");
    }

    private void crearCuentasYGuardar(List<Usuario> nuevos) {
        List<String> uidsCreados = new ArrayList<>();
        try {
            List<String> contrasenas = new ArrayList<>();
            for (Usuario usuario : nuevos) {
                String contrasena = generarContrasenaTemporal();
                try {
                    String uid = authService.crearCuenta(new CrearCuentaRequestDTO(usuario.getCorreo(), contrasena));
                    uidsCreados.add(uid);
                    usuario.setFirebaseUid(uid);
                } catch (ResponseStatusException e) {
                    throw new ResponseStatusException(e.getStatusCode(), "No se pudo crear la cuenta de " + usuario.getCorreo()
                            + ". " + e.getReason() + " No se creó ninguna cuenta.");
                }
                contrasenas.add(contrasena);
            }
            usuarioRepository.saveAllAndFlush(nuevos);
            enviarCredencialesDespuesDeConfirmar(nuevos, contrasenas);
        } catch (RuntimeException e) {
            // Las identidades creadas quedarían sin dueño: la base se deshace sola, el proveedor no.
            uidsCreados.forEach(authService::eliminarCuenta);
            throw e;
        }
    }

    /**
     * Los correos salen recién cuando las cuentas quedaron confirmadas en la base: si salieran antes y la importación se
     * deshiciera, las personas tendrían credenciales de cuentas que ya no existen. Un correo que falla no revierte nada y no
     * frena a los demás: se deja en el log con la dirección para reenviarlo a mano.
     */
    private void enviarCredencialesDespuesDeConfirmar(List<Usuario> nuevos, List<String> contrasenas) {
        Runnable envio = () -> {
            for (int i = 0; i < nuevos.size(); i++) {
                String correo = nuevos.get(i).getCorreo();
                try {
                    emailService.enviarCredencialesTemporales(correo, contrasenas.get(i));
                } catch (RuntimeException e) {
                    log.error("La cuenta de {} se creó, pero no se pudo enviar el correo con las credenciales", correo, e);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    private AnalizadorCsv.Analisis<Usuario> analizarCsv(MultipartFile archivo) {
        // Se comparan contra todas las personas del sistema (no solo las internas): la cédula y el correo son únicos entre roles.
        Set<String> cedulasRegistradas = new HashSet<>(usuarioRepository.listarCedulas());
        Set<String> correosRegistrados = new HashSet<>(usuarioRepository.listarCorreosEnMinusculas());
        Set<String> matriculasRegistradas = new HashSet<>(medicoRepository.listarMatriculasEnMinusculas());
        Map<String, Especialidad> especialidadesPorCodigo = especialidadRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getCodigo().toLowerCase(Locale.ROOT), Function.identity()));
        AnalizadorCsv.Repetidos cedulasDelArchivo = new AnalizadorCsv.Repetidos();
        AnalizadorCsv.Repetidos correosDelArchivo = new AnalizadorCsv.Repetidos();
        AnalizadorCsv.Repetidos matriculasDelArchivo = new AnalizadorCsv.Repetidos();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        return AnalizadorCsv.analizar(archivo, MAXIMO_FILAS_CSV, "usuarios", COLUMNAS_CSV, (fila, errores) -> {
            String textoCedula = fila.valor(CEDULA);
            String nombre = fila.valor(NOMBRE);
            String apellido = fila.valor(APELLIDO);
            String textoCorreo = fila.valor(CORREO);
            String textoFecha = fila.valor(FECHA_NACIMIENTO);
            String textoRol = fila.valor(ROL);
            String matricula = fila.valor(MATRICULA);
            String textoEspecialidades = fila.valor(ESPECIALIDADES);

            // Lo repetido en el archivo se marca en la segunda aparición, aunque la primera tuviera otros errores.
            String cedula = FormatoUsuario.normalizarCedula(textoCedula);
            String correo = textoCorreo.toLowerCase(Locale.ROOT);
            Integer cedulaPrevia = cedula == null ? null : cedulasDelArchivo.vistoEn(cedula, fila.numero());
            Integer correoPrevio = correosDelArchivo.vistoEn(correo, fila.numero());
            Integer matriculaPrevia = matriculasDelArchivo.vistoEn(matricula.toLowerCase(Locale.ROOT), fila.numero());

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
            LocalDate fechaNacimiento = FormatoUsuario.interpretarFecha(textoFecha);
            if (textoFecha.isEmpty()) {
                errores.add("La fecha de nacimiento es obligatoria");
            } else if (fechaNacimiento == null) {
                errores.add("La fecha de nacimiento no tiene un formato válido (use AAAA-MM-DD o DD/MM/AAAA)");
            } else if (!fechaNacimiento.isBefore(LocalDate.now(reloj))) {
                errores.add("La fecha de nacimiento debe ser anterior a hoy");
            }

            RolUsuarioInterno rol = interpretarRolDelArchivo(textoRol);
            List<Especialidad> especialidades = List.of();
            if (textoRol.isEmpty()) {
                errores.add("El rol es obligatorio");
            } else if (rol == null) {
                errores.add("El rol no es válido (use administrador, funcionario o médico)");
            } else if (rol == RolUsuarioInterno.MEDICO) {
                if (matricula.isEmpty()) {
                    errores.add("La matrícula es obligatoria para los médicos");
                } else if (matricula.length() > 30) {
                    errores.add("La matrícula admite hasta 30 caracteres");
                } else if (matriculasRegistradas.contains(matricula.toLowerCase(Locale.ROOT))) {
                    errores.add("La matrícula ya está registrada");
                } else if (matriculaPrevia != null) {
                    errores.add("La matrícula está repetida en el archivo (también en la fila " + matriculaPrevia + ")");
                }
                especialidades = especialidadesDelArchivo(textoEspecialidades, especialidadesPorCodigo, errores);
            } else if (!matricula.isEmpty() || !textoEspecialidades.isEmpty()) {
                errores.add("Solo los médicos tienen matrícula y especialidades: deje esas columnas vacías");
            }
            if (!errores.isEmpty()) {
                return null;
            }

            Usuario usuario = nuevoUsuario(rol);
            usuario.setCedula(cedula);
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setCorreo(correo);
            usuario.setFechaNacimiento(fechaNacimiento);
            usuario.setFechaCreacion(ahora);
            usuario.setEstado(EstadoActivacion.ACTIVO);
            usuario.setProveedorAcceso(ProveedorAcceso.PASSWORD);
            if (usuario instanceof Medico medico) {
                medico.setMatricula(matricula);
                medico.setEspecialidades(new HashSet<>(especialidades));
            }
            return usuario;
        });
    }

    /** «administrador» (o «admin»), «funcionario» o «médico», sin importar tildes ni mayúsculas; nulo si no es ninguno. */
    private static RolUsuarioInterno interpretarRolDelArchivo(String texto) {
        return switch (LectorCsv.normalizar(texto)) {
            case "administrador", "admin" -> RolUsuarioInterno.ADMINISTRADOR;
            case "funcionario" -> RolUsuarioInterno.FUNCIONARIO;
            case "medico" -> RolUsuarioInterno.MEDICO;
            default -> null;
        };
    }

    /**
     * Los códigos de las especialidades de un médico, separados por «|», «;», «,» o «/» (si el separador del archivo es el mismo,
     * la celda va entre comillas). Tiene que haber al menos una y todas deben existir y estar activas.
     */
    private static List<Especialidad> especialidadesDelArchivo(String texto, Map<String, Especialidad> porCodigo, List<String> errores) {
        List<String> codigos = Arrays.stream(texto.split("[|;,/]")).map(String::trim).filter(c -> !c.isEmpty()).distinct().toList();
        if (codigos.isEmpty()) {
            errores.add("Los médicos necesitan al menos una especialidad");
            return List.of();
        }
        List<Especialidad> encontradas = new ArrayList<>();
        List<String> inexistentes = new ArrayList<>();
        List<String> inactivas = new ArrayList<>();
        for (String codigo : codigos) {
            Especialidad especialidad = porCodigo.get(codigo.toLowerCase(Locale.ROOT));
            if (especialidad == null) {
                inexistentes.add(codigo);
            } else if (especialidad.getEstado() != EstadoActivacion.ACTIVO) {
                inactivas.add(codigo);
            } else if (!encontradas.contains(especialidad)) {
                encontradas.add(especialidad);
            }
        }
        if (!inexistentes.isEmpty()) {
            errores.add("No existen las especialidades: " + String.join(", ", inexistentes));
        }
        if (!inactivas.isEmpty()) {
            errores.add("No están activas las especialidades: " + String.join(", ", inactivas));
        }
        return encontradas;
    }

    /** CU-5. */
    public void actualizarPerfil(Long idUsuarioInterno, PerfilDTO datos) {
        throw new NoImplementadoException();
    }
}
