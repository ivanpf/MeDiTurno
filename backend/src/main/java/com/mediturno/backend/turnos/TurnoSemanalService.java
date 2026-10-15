package com.mediturno.backend.turnos;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;
import com.mediturno.backend.especialidades.EspecialidadService;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.notificaciones.NotificacionService;
import com.mediturno.backend.pases.PaseService;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.dto.AltaTurnoSemanalDTO;
import com.mediturno.backend.turnos.dto.OcurrenciaTurnoDTO;
import com.mediturno.backend.turnos.dto.RehabilitacionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoResponseDTO;
import com.mediturno.backend.turnos.dto.TurnoSemanalResponseDTO;
import com.mediturno.backend.turnos.dto.VistaPreviaSuspensionDTO;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.SuspensionTurno;
import com.mediturno.backend.turnos.model.TipoSuspension;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.turnos.repository.SuspensionTurnoRepository;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;
import com.mediturno.backend.usuarios.repository.FuncionarioRepository;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.repository.MedicoRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-8, CU-12, CU-13, CU-14, CU-20.
 * Están implementados CU-8 (consulta de ocurrencias disponibles), CU-12, CU-13 y CU-20. CU-14 conserva su respuesta 501.
 */
@Service
public class TurnoSemanalService {

    private static final Logger log = LoggerFactory.getLogger(TurnoSemanalService.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Para consultar «de esta fecha en adelante»: una suspensión permanente no termina. */
    private static final LocalDate SIN_FIN = LocalDate.of(9999, 12, 31);

    private final TurnoSemanalRepository turnoSemanalRepository;
    private final MedicoRepository medicoRepository;
    private final ConsultorioRepository consultorioRepository;
    private final OcurrenciaTurnoRepository ocurrenciaTurnoRepository;
    private final ReservaRepository reservaRepository;
    private final EspecialidadService especialidadService;
    private final PaseService paseService;
    private final DisponibilidadService disponibilidadService;
    private final GeneradorOcurrenciasService generadorOcurrenciasService;
    private final SuspensionTurnoRepository suspensionTurnoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final NotificacionService notificacionService;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public TurnoSemanalService(TurnoSemanalRepository turnoSemanalRepository, MedicoRepository medicoRepository,
            ConsultorioRepository consultorioRepository, OcurrenciaTurnoRepository ocurrenciaTurnoRepository, ReservaRepository reservaRepository,
            EspecialidadService especialidadService, PaseService paseService, DisponibilidadService disponibilidadService,
            GeneradorOcurrenciasService generadorOcurrenciasService, SuspensionTurnoRepository suspensionTurnoRepository,
            FuncionarioRepository funcionarioRepository, NotificacionService notificacionService,
            PlatformTransactionManager administradorDeTransacciones, Clock reloj) {
        this.turnoSemanalRepository = turnoSemanalRepository;
        this.medicoRepository = medicoRepository;
        this.consultorioRepository = consultorioRepository;
        this.ocurrenciaTurnoRepository = ocurrenciaTurnoRepository;
        this.reservaRepository = reservaRepository;
        this.especialidadService = especialidadService;
        this.paseService = paseService;
        this.disponibilidadService = disponibilidadService;
        this.generadorOcurrenciasService = generadorOcurrenciasService;
        this.suspensionTurnoRepository = suspensionTurnoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.notificacionService = notificacionService;
        this.transaccion = new TransactionTemplate(administradorDeTransacciones);
        this.reloj = reloj;
    }

    /**
     * CU-12. Crea un turno semanal por cada día elegido, todos o ninguno, y genera sus fechas reservables. Los campos
     * obligatorios los valida el controlador (@Valid). Se bloquean el médico y el consultorio hasta el fin de la
     * transacción: la búsqueda de superposiciones y el guardado no pueden intercalarse con otra alta del mismo médico
     * o consultorio.
     */
    @Transactional
    public List<TurnoSemanalResponseDTO> altaTurnoSemanal(AltaTurnoSemanalDTO datos) {
        validarHorarioYVigencia(datos);
        Medico medico = medicoRepository.buscarConLock(datos.idMedico())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El médico seleccionado no existe."));
        Consultorio consultorio = consultorioRepository.buscarConLock(datos.codigoConsultorio())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El consultorio seleccionado no existe."));
        Especialidad especialidad = validarMedicoEspecialidadYConsultorio(medico, consultorio, datos.codigoEspecialidad());

        List<DiaSemana> dias = datos.dias().stream().distinct().sorted().toList();
        validarSinSuperposiciones(datos, dias);

        List<TurnoSemanal> turnos = new ArrayList<>();
        for (DiaSemana dia : dias) {
            TurnoSemanal turno = new TurnoSemanal();
            turno.setMedico(medico);
            turno.setEspecialidad(especialidad);
            turno.setConsultorio(consultorio);
            turno.setDia(dia);
            turno.setHoraInicio(datos.horaInicio());
            turno.setHoraFin(datos.horaFin());
            turno.setCupoMaximo(datos.cupoMaximo());
            turno.setVigenteDesde(datos.vigenteDesde());
            turno.setEstado(EstadoTurnoSemanal.ACTIVO);
            turnos.add(turno);
        }
        List<TurnoSemanal> guardados = turnoSemanalRepository.saveAll(turnos);

        // Las fechas hasta donde se puede reservar se generan ahora, en la misma transacción: el turno y sus fechas se
        // guardan juntos o no se guarda ninguno. Así un turno de hoy (que todavía no empezó) ya se puede reservar.
        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (TurnoSemanal turno : guardados) {
            generadorOcurrenciasService.generarOcurrenciasDe(turno, ahora, disponibilidadService.diasDeReserva());
        }
        return guardados.stream().map(t -> convertirATurnoSemanalResponseDTO(t, null)).toList();
    }

    /** Datos inválidos que no se ven mirando un solo campo, o que dependen de la fecha de hoy. */
    private void validarHorarioYVigencia(AltaTurnoSemanalDTO datos) {
        if (!datos.horaFin().isAfter(datos.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (datos.vigenteDesde().isBefore(LocalDate.now(reloj))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de vigencia no puede ser anterior a hoy.");
        }
    }

    /** El médico tiene que estar activo y tener esa especialidad, y el consultorio tiene que estar activo. */
    private static Especialidad validarMedicoEspecialidadYConsultorio(Medico medico, Consultorio consultorio, String codigoEspecialidad) {
        if (medico.getEstado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El médico seleccionado está inactivo.");
        }
        Especialidad especialidad = medico.getEspecialidades().stream()
            .filter(e -> e.getCodigo().equals(codigoEspecialidad))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                "El médico seleccionado no tiene asignada la especialidad indicada."));
        if (consultorio.getEstado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El consultorio seleccionado está inactivo.");
        }
        return especialidad;
    }

    /** Ningún día puede superponerse con otro turno del mismo médico o del mismo consultorio. */
    private void validarSinSuperposiciones(AltaTurnoSemanalDTO datos, List<DiaSemana> dias) {
        List<String> conflictos = new ArrayList<>();
        for (DiaSemana dia : dias) {
            for (TurnoSemanal existente : turnoSemanalRepository.buscarSuperposiciones(
                    dia, datos.idMedico(), datos.codigoConsultorio(), datos.horaInicio(), datos.horaFin(), datos.vigenteDesde())) {
                conflictos.add(describir(existente));
            }
        }
        if (!conflictos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Superposición con: " + String.join("; ", conflictos) + ".");
        }
    }

    private static String describir(TurnoSemanal t) {
        return nombreDe(t.getMedico()) + " · " + nombreDelDia(t.getDia()) + " " + t.getHoraInicio().format(HORA) + "-"
            + t.getHoraFin().format(HORA) + " · " + t.getConsultorio().getNombre() + " (turno #" + t.getIdTurnoSemanal() + ")";
    }

    private static String nombreDelDia(DiaSemana dia) {
        return switch (dia) {
            case LUNES -> "Lunes";
            case MARTES -> "Martes";
            case MIERCOLES -> "Miércoles";
            case JUEVES -> "Jueves";
            case VIERNES -> "Viernes";
            case SABADO -> "Sábado";
            case DOMINGO -> "Domingo";
        };
    }

    private static String nombreDe(Medico medico) {
        return medico.getApellido() + ", " + medico.getNombre();
    }

    private static TurnoSemanalResponseDTO convertirATurnoSemanalResponseDTO(TurnoSemanal t, SuspensionTurno suspension) {
        TurnoSemanalResponseDTO.SuspensionResumen resumen = suspension == null ? null
            : new TurnoSemanalResponseDTO.SuspensionResumen(suspension.getSuspensionTipo(), suspension.getFechaInicio(), suspension.getFechaFin());
        return new TurnoSemanalResponseDTO(t.getIdTurnoSemanal(), t.getMedico().getIdUsuario(), nombreDe(t.getMedico()),
            t.getEspecialidad().getCodigo(), t.getEspecialidad().getNombre(), t.getConsultorio().getCodigo(),
            t.getConsultorio().getNombre(), t.getDia(), t.getHoraInicio(), t.getHoraFin(), t.getCupoMaximo(),
            t.getVigenteDesde(), t.getVigenteHasta(), t.getEstado(), resumen);
    }

    /**
     * CU-13. Registra la suspensión del turno, deja suspendidas las fechas alcanzadas, anula sus reservas vigentes y, ya
     * confirmado todo, avisa a los pacientes. Se suma {@code idFuncionario} a la firma del diseño: es quien registra la suspensión.
     * <p>
     * La suspensión rige desde {@code fechaDesde}: hasta ese día el turno y sus fechas siguen igual, y el turno pasa a
     * SUSPENDIDO cuando llega la fecha (lo hace la tarea programada; si la fecha es hoy, pasa enseguida). Las fechas alcanzadas
     * se suspenden y sus reservas se anulan al registrar la suspensión, para avisar con tiempo.
     */
    public SuspensionTurnoResponseDTO suspenderTurno(Long idTurnoSemanal, SuspensionTurnoDTO datos, Long idFuncionario) {
        SuspensionTurnoResponseDTO respuesta = transaccion.execute(estado -> registrarSuspension(idTurnoSemanal, datos, idFuncionario));
        // Los avisos van después de confirmar la suspensión: si fallan, la suspensión queda registrada igual.
        try {
            notificacionService.notificarSuspensionTurno(respuesta.idSuspensionTurno());
        } catch (RuntimeException e) {
            log.error("La suspensión {} quedó registrada, pero no se pudieron generar los avisos: {}", respuesta.idSuspensionTurno(), e.getMessage(), e);
        }
        return respuesta;
    }

    private SuspensionTurnoResponseDTO registrarSuspension(Long idTurnoSemanal, SuspensionTurnoDTO datos, Long idFuncionario) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        TurnoSemanal turno = turnoSemanalRepository.buscarConLock(idTurnoSemanal)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El turno seleccionado no existe."));
        validarQueSePuedeSuspender(turno, hoy);
        validarPeriodoDeSuspension(datos, hoy);
        LocalDate hasta = datos.modalidad() == TipoSuspension.TEMPORAL ? datos.fechaHasta() : SIN_FIN;

        SuspensionTurno suspension = new SuspensionTurno();
        suspension.setTurnoSemanal(turno);
        suspension.setFuncionario(funcionarioRepository.getReferenceById(idFuncionario));
        suspension.setSuspensionTipo(datos.modalidad());
        suspension.setFechaInicio(datos.fechaDesde());
        suspension.setFechaFin(datos.modalidad() == TipoSuspension.TEMPORAL ? datos.fechaHasta() : null);
        suspension.setPeriodo(describirPeriodo(datos));
        suspension.setMotivo(datos.motivo() == null || datos.motivo().isBlank() ? null : datos.motivo().trim());
        suspension.setFechaRegistro(ahora);
        suspensionTurnoRepository.saveAndFlush(suspension);

        // Las fechas alcanzadas se bloquean primero: ninguna reserva nueva puede colarse mientras se anulan las existentes.
        int fechasSuspendidas = 0;
        for (OcurrenciaTurno ocurrencia : ocurrenciaTurnoRepository.buscarDelTurnoEntreFechasConLock(idTurnoSemanal, datos.fechaDesde(), hasta)) {
            if (ocurrencia.getOcurrenciaTurnoEstado() == EstadoOcurrenciaTurno.HABILITADA) {
                ocurrencia.setOcurrenciaTurnoEstado(EstadoOcurrenciaTurno.SUSPENDIDA);
                fechasSuspendidas++;
            }
        }
        List<Reserva> reservas = reservaRepository.buscarVigentesDelTurnoEntreFechas(idTurnoSemanal, datos.fechaDesde(), hasta);
        for (Reserva reserva : reservas) {
            reserva.setReservaEstado(EstadoReserva.ANULADA);
            reserva.setFechaCancelacion(ahora);
            reserva.setMotivoCancelacion(MotivoAnulacionPorSuspension.de(suspension.getIdSuspensionTurno()));
        }
        if (!datos.fechaDesde().isAfter(hoy)) {
            turno.setEstado(EstadoTurnoSemanal.SUSPENDIDO);
        }
        return new SuspensionTurnoResponseDTO(suspension.getIdSuspensionTurno(), idTurnoSemanal, datos.modalidad(), datos.fechaDesde(),
            suspension.getFechaFin(), suspension.getMotivo(), ahora, fechasSuspendidas, reservas.size(), turno.getEstado());
    }

    /**
     * CU-13, pasos 4, 6 y 7: qué pasaría si se suspendiera el turno con esos datos. No modifica nada. Valida lo mismo que la
     * suspensión, así la persona ve los errores antes de confirmar.
     */
    @Transactional(readOnly = true)
    public VistaPreviaSuspensionDTO vistaPreviaSuspension(Long idTurnoSemanal, SuspensionTurnoDTO datos) {
        LocalDate hoy = LocalDate.now(reloj);
        TurnoSemanal turno = turnoSemanalRepository.findById(idTurnoSemanal)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El turno seleccionado no existe."));
        validarQueSePuedeSuspender(turno, hoy);
        validarPeriodoDeSuspension(datos, hoy);
        LocalDate hasta = datos.modalidad() == TipoSuspension.TEMPORAL ? datos.fechaHasta() : SIN_FIN;

        List<VistaPreviaSuspensionDTO.ReservaAfectada> reservas = reservaRepository
            .buscarVigentesDelTurnoEntreFechas(idTurnoSemanal, datos.fechaDesde(), hasta).stream()
            .map(r -> new VistaPreviaSuspensionDTO.ReservaAfectada(r.getIdReserva(), r.getOcurrenciaTurno().getFecha(),
                r.getOcurrenciaTurno().getHoraInicio(), r.getNumero(),
                r.getPaciente().getApellido() + ", " + r.getPaciente().getNombre(), r.getPaciente().getCedula()))
            .toList();
        int fechas = ocurrenciaTurnoRepository.fechasExistentes(idTurnoSemanal, datos.fechaDesde(), hasta).size();
        return new VistaPreviaSuspensionDTO(idTurnoSemanal, datos.modalidad(), datos.fechaDesde(),
            datos.modalidad() == TipoSuspension.TEMPORAL ? datos.fechaHasta() : null, fechas, reservas.size(), reservas);
    }

    /** Flujo 6.A: el turno tiene que estar activo y no tener ya una suspensión registrada que siga sin terminar. */
    private void validarQueSePuedeSuspender(TurnoSemanal turno, LocalDate hoy) {
        if (turno.getEstado() != EstadoTurnoSemanal.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya no está activo (estado actual: suspendido).");
        }
        suspensionTurnoRepository.buscarSinRehabilitarDelTurno(turno.getIdTurnoSemanal()).stream()
            .filter(s -> s.getFechaFin() == null || !s.getFechaFin().isBefore(hoy))
            .findFirst()
            .ifPresent(s -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno ya tiene una suspensión registrada (" + s.getPeriodo() + "). Para cambiarla hay que rehabilitar el turno.");
            });
    }

    /** Flujo 6.B: período de suspensión inválido. */
    private static void validarPeriodoDeSuspension(SuspensionTurnoDTO datos, LocalDate hoy) {
        List<String> errores = new ArrayList<>();
        if (datos.fechaDesde().isBefore(hoy)) {
            errores.add("La fecha desde la que rige la suspensión no puede ser anterior a hoy.");
        }
        if (datos.modalidad() == TipoSuspension.TEMPORAL) {
            if (datos.fechaHasta() == null) {
                errores.add("Indique hasta qué fecha dura la suspensión temporal.");
            } else if (!datos.fechaHasta().isAfter(datos.fechaDesde())) {
                errores.add("La fecha hasta debe ser posterior a la fecha desde.");
            }
        }
        if (!errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errores));
        }
    }

    private static String describirPeriodo(SuspensionTurnoDTO datos) {
        return datos.modalidad() == TipoSuspension.TEMPORAL
            ? "Temporal: del " + datos.fechaDesde().format(FECHA) + " al " + datos.fechaHasta().format(FECHA)
            : "Permanente desde el " + datos.fechaDesde().format(FECHA);
    }

    /** CU-14. */
    public TurnoSemanalResponseDTO rehabilitarTurno(Long idTurnoSemanal, RehabilitacionTurnoDTO datos) {
        throw new NoImplementadoException();
    }

    /**
     * CU-20. Los criterios son opcionales y se combinan (todos tienen que cumplirse). Sin {@code ordenarPor}, el orden es por día
     * de la semana, horario y médico. No modifica nada.
     */
    @Transactional(readOnly = true)
    public List<TurnoSemanalResponseDTO> buscarTurnosSemanales(CriteriosBusquedaDTO criterios) {
        DiaSemana dia = interpretar(criterios.dia(), DiaSemana.class, "El día indicado no es válido.");
        EstadoTurnoSemanal estado = interpretar(criterios.estado(), EstadoTurnoSemanal.class, "El estado indicado no es válido.");
        Comparator<TurnoSemanal> orden = comparadorPara(criterios.ordenarPor(), Boolean.TRUE.equals(criterios.descendente()));

        Specification<TurnoSemanal> filtros = (turno, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (criterios.idMedico() != null) {
                condiciones.add(cb.equal(turno.get("medico").get("idUsuario"), criterios.idMedico()));
            }
            if (criterios.codigoEspecialidad() != null && !criterios.codigoEspecialidad().isBlank()) {
                condiciones.add(cb.equal(turno.get("especialidad").get("codigo"), criterios.codigoEspecialidad().trim()));
            }
            if (dia != null) {
                condiciones.add(cb.equal(turno.get("dia"), dia));
            }
            if (criterios.codigoConsultorio() != null && !criterios.codigoConsultorio().isBlank()) {
                condiciones.add(cb.equal(turno.get("consultorio").get("codigo"), criterios.codigoConsultorio().trim()));
            }
            if (estado != null) {
                condiciones.add(cb.equal(turno.get("estado"), estado));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        List<TurnoSemanal> turnos = turnoSemanalRepository.findAll(filtros).stream().sorted(orden).toList();

        Map<Long, SuspensionTurno> suspensiones = new HashMap<>();
        if (!turnos.isEmpty()) {
            suspensionTurnoRepository.buscarSinRehabilitarDeTurnos(turnos.stream().map(TurnoSemanal::getIdTurnoSemanal).toList())
                .forEach(s -> suspensiones.put(s.getTurnoSemanal().getIdTurnoSemanal(), s));
        }
        return turnos.stream().map(t -> convertirATurnoSemanalResponseDTO(t, suspensiones.get(t.getIdTurnoSemanal()))).toList();
    }

    private static <E extends Enum<E>> E interpretar(String texto, Class<E> tipo, String mensajeSiNoEsValido) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeSiNoEsValido);
        }
    }

    private static Comparator<TurnoSemanal> comparadorPara(String ordenarPor, boolean descendente) {
        Comparator<TurnoSemanal> porDia = Comparator.comparing(TurnoSemanal::getDia).thenComparing(TurnoSemanal::getHoraInicio);
        Comparator<TurnoSemanal> porMedico = Comparator.comparing((TurnoSemanal t) -> t.getMedico().getApellido(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(t -> t.getMedico().getNombre(), String.CASE_INSENSITIVE_ORDER);
        Comparator<TurnoSemanal> elegido;
        String clave = ordenarPor == null ? "" : ordenarPor.trim().toLowerCase();
        switch (clave) {
            case "" -> {
                elegido = porDia.thenComparing(porMedico);
                descendente = false; // el orden por defecto no se invierte
            }
            case "medico" -> elegido = porMedico.thenComparing(porDia);
            case "especialidad" -> elegido = Comparator.comparing((TurnoSemanal t) -> t.getEspecialidad().getNombre(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(porDia);
            case "dia" -> elegido = porDia.thenComparing(porMedico);
            case "horario" -> elegido = Comparator.comparing(TurnoSemanal::getHoraInicio).thenComparing(TurnoSemanal::getHoraFin).thenComparing(porDia);
            case "consultorio" -> elegido = Comparator.comparing((TurnoSemanal t) -> t.getConsultorio().getNombre(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(porDia);
            case "estado" -> elegido = Comparator.comparing(TurnoSemanal::getEstado).thenComparing(porDia);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El orden indicado no es válido (use medico, especialidad, dia, horario, consultorio o estado).");
        }
        elegido = elegido.thenComparing(TurnoSemanal::getIdTurnoSemanal);
        return descendente ? elegido.reversed() : elegido;
    }

    /** CU-8. */
    @Transactional(readOnly = true)
    public List<OcurrenciaTurnoDTO> listarOcurrenciasDisponibles(String codigoEspecialidad, Long idPaciente) {
        validarEspecialidadReservable(codigoEspecialidad, idPaciente);
        LocalDate hoy = LocalDate.now(reloj);
        return ocurrenciaTurnoRepository.buscarPorEspecialidadEntreFechas(codigoEspecialidad, hoy, disponibilidadService.ultimaFechaReservable(hoy))
                .stream()
                .filter(disponibilidadService::estaHabilitada)
                .map(this::convertirAOcurrenciaTurnoDTO)
                .filter(TurnoSemanalService::tieneNumerosLibres)
                .toList();
    }

    /** La especialidad tiene que estar activa y, si exige pase, el paciente tiene que tener uno vigente. */
    private void validarEspecialidadReservable(String codigoEspecialidad, Long idPaciente) {
        EspecialidadResponseDTO especialidad = especialidadService.obtenerEspecialidad(codigoEspecialidad);
        if (especialidad.estado() != EstadoActivacion.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La especialidad no está activa");
        }
        if (especialidad.requierePase() && !paseService.tienePaseVigente(idPaciente, codigoEspecialidad)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No posee un pase o habilitación de reconsulta vigente para esta especialidad");
        }
    }

    private OcurrenciaTurnoDTO convertirAOcurrenciaTurnoDTO(OcurrenciaTurno ocurrencia) {
        TurnoSemanal turno = ocurrencia.getTurnoSemanal();
        Medico medico = turno.getMedico();
        return new OcurrenciaTurnoDTO(ocurrencia.getIdOcurrenciaTurno(), medico.getIdUsuario(),
                medico.getNombre() + " " + medico.getApellido(), ocurrencia.getFecha(), ocurrencia.getHoraInicio(),
                ocurrencia.getHoraFin(), turno.getConsultorio().getCodigo(), ocurrencia.getCupos(),
                List.copyOf(numerosNoDisponibles(ocurrencia)));
    }

    /** Números que el paciente no puede elegir: los ya reservados y los de hoy cuya hora estimada ya pasó. */
    private SortedSet<Integer> numerosNoDisponibles(OcurrenciaTurno ocurrencia) {
        SortedSet<Integer> noDisponibles = new TreeSet<>(reservaRepository.buscarNumerosOcupados(ocurrencia.getIdOcurrenciaTurno()));
        for (int numero = 1; numero <= ocurrencia.getCupos(); numero++) {
            if (!disponibilidadService.esNumeroFuturo(ocurrencia, numero)) {
                noDisponibles.add(numero);
            }
        }
        return noDisponibles;
    }

    /** Se ofrece solo la fecha en la que todavía queda algún número que elegir. */
    private static boolean tieneNumerosLibres(OcurrenciaTurnoDTO ocurrencia) {
        return ocurrencia.numerosNoDisponibles().size() < ocurrencia.cupos();
    }
}
