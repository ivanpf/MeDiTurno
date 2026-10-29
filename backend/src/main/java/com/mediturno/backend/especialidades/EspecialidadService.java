package com.mediturno.backend.especialidades;

import com.mediturno.backend.common.busqueda.FiltrosBusqueda;
import com.mediturno.backend.common.csv.AnalizadorCsv;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.especialidades.dto.AltaEspecialidadDTO;
import com.mediturno.backend.especialidades.dto.EspecialidadDetalleResponseDTO;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.dto.ModificarEspecialidadDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;
import com.mediturno.backend.usuarios.repository.MedicoRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-28, CU-29, CU-30, CU-43.
 */
@Service
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final TurnoSemanalRepository turnoSemanalRepository;
    private final AdministradorRepository administradorRepository;
    private final Clock reloj;

    public EspecialidadService(EspecialidadRepository especialidadRepository, MedicoRepository medicoRepository,
            TurnoSemanalRepository turnoSemanalRepository, AdministradorRepository administradorRepository, Clock reloj) {
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.turnoSemanalRepository = turnoSemanalRepository;
        this.administradorRepository = administradorRepository;
        this.reloj = reloj;
    }

    /** CU-28. Los campos obligatorios los valida el controlador (@Valid); acá se comprueba la unicidad. */
    public EspecialidadResponseDTO altaEspecialidad(AltaEspecialidadDTO datos) {
        String codigo = datos.codigo().trim();
        String nombre = datos.nombre().trim();
        validarCodigoYNombreUnicos(codigo, nombre);

        Especialidad especialidad = new Especialidad();
        especialidad.setCodigo(codigo);
        especialidad.setNombre(nombre);
        especialidad.setDescripcion(datos.descripcion() == null || datos.descripcion().isBlank() ? null : datos.descripcion().trim());
        especialidad.setRequierePase(datos.requierePase());
        especialidad.setEstado(datos.estado());
        try {
            especialidadRepository.saveAndFlush(especialidad);
        } catch (DataIntegrityViolationException e) {
            // Otro administrador la creó en el mismo instante: las restricciones de la base lo detectan.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código o el nombre ya están en uso por otra especialidad");
        }
        return convertirAEspecialidadResponseDTO(especialidad);
    }

    private void validarCodigoYNombreUnicos(String codigo, String nombre) {
        boolean codigoEnUso = especialidadRepository.existsById(codigo);
        boolean nombreEnUso = especialidadRepository.existsByNombreIgnoreCase(nombre);
        if (codigoEnUso && nombreEnUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código y el nombre ya están en uso por otras especialidades");
        }
        if (codigoEnUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya está en uso por otra especialidad");
        }
        if (nombreEnUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre ya está en uso por otra especialidad");
        }
    }

    private static EspecialidadResponseDTO convertirAEspecialidadResponseDTO(Especialidad e) {
        return new EspecialidadResponseDTO(e.getCodigo(), e.getNombre(), e.getDescripcion(), e.isRequierePase(), e.getEstado());
    }

    private static final int MAXIMO_FILAS_CSV = 1000;
    private static final String CODIGO = "Código";
    private static final String NOMBRE = "Nombre";
    private static final String DESCRIPCION = "Descripción";
    private static final String REQUIERE_PASE = "Requiere pase";
    private static final String ESTADO = "Estado";
    private static final List<AnalizadorCsv.Columna> COLUMNAS_CSV = List.of(
        AnalizadorCsv.Columna.obligatoria(CODIGO), AnalizadorCsv.Columna.obligatoria(NOMBRE),
        AnalizadorCsv.Columna.obligatoria(DESCRIPCION), AnalizadorCsv.Columna.obligatoria(REQUIERE_PASE),
        AnalizadorCsv.Columna.opcional(ESTADO));

    /**
     * CU-29, previsualización. Lee el CSV y valida cada fila (campos obligatorios, largos, valores, y códigos o nombres repetidos
     * en el archivo o ya registrados) sin guardar nada. Es el paso previo a la importación.
     */
    @Transactional(readOnly = true)
    public ResultadoImportacionDTO previsualizarEspecialidadesCSV(MultipartFile archivo) {
        return analizarCsv(archivo).resultado();
    }

    /**
     * CU-29. Vuelve a analizar el archivo (nada de lo que mostró la previsualización se da por cierto) y, solo si todas las filas
     * son válidas, crea todas las especialidades en una sola transacción; si alguna tiene errores no se crea ninguna (422).
     * Devuelve el resultado con la cantidad de especialidades creadas.
     */
    @Transactional
    public ResultadoImportacionDTO altaEspecialidadesCSV(MultipartFile archivo) {
        return analizarCsv(archivo).importar(especialidadRepository::saveAllAndFlush,
            "El archivo contiene errores: corríjalo y vuelva a cargarlo. No se creó ninguna especialidad.",
            // Otro administrador creó alguna en el mismo instante: las restricciones de la base lo detectan.
            "Alguna especialidad se registró mientras se importaba el archivo. Vuelva a cargarlo. No se creó ninguna especialidad.");
    }

    private AnalizadorCsv.Analisis<Especialidad> analizarCsv(MultipartFile archivo) {
        Set<String> codigosRegistrados = new HashSet<>();
        Set<String> nombresRegistrados = new HashSet<>();
        especialidadRepository.findAll().forEach(e -> {
            codigosRegistrados.add(e.getCodigo());
            nombresRegistrados.add(e.getNombre().toLowerCase());
        });
        AnalizadorCsv.Repetidos codigosDelArchivo = new AnalizadorCsv.Repetidos();
        AnalizadorCsv.Repetidos nombresDelArchivo = new AnalizadorCsv.Repetidos();

        return AnalizadorCsv.analizar(archivo, MAXIMO_FILAS_CSV, "especialidades", COLUMNAS_CSV, (fila, errores) -> {
            String codigo = fila.valor(CODIGO);
            String nombre = fila.valor(NOMBRE);
            String descripcion = fila.valor(DESCRIPCION);
            String textoPase = fila.valor(REQUIERE_PASE);
            String textoEstado = fila.valor(ESTADO);
            // Un código o nombre repetido en el archivo se marca en la segunda aparición, aunque la primera tuviera otros errores.
            Integer codigoPrevio = codigosDelArchivo.vistoEn(codigo, fila.numero());
            Integer nombrePrevio = nombresDelArchivo.vistoEn(nombre.toLowerCase(), fila.numero());

            if (codigo.isEmpty()) {
                errores.add("El código es obligatorio");
            } else if (codigo.length() > 20) {
                errores.add("El código admite hasta 20 caracteres");
            } else if (codigosRegistrados.contains(codigo)) {
                errores.add("El código ya está en uso por otra especialidad");
            } else if (codigoPrevio != null) {
                errores.add("El código está repetido en el archivo (también en la fila " + codigoPrevio + ")");
            }
            if (nombre.isEmpty()) {
                errores.add("El nombre es obligatorio");
            } else if (nombre.length() > 100) {
                errores.add("El nombre admite hasta 100 caracteres");
            } else if (nombresRegistrados.contains(nombre.toLowerCase())) {
                errores.add("El nombre ya está en uso por otra especialidad");
            } else if (nombrePrevio != null) {
                errores.add("El nombre está repetido en el archivo (también en la fila " + nombrePrevio + ")");
            }
            if (descripcion.length() > 500) {
                errores.add("La descripción admite hasta 500 caracteres");
            }
            Boolean requierePase = AnalizadorCsv.interpretarSiNo(textoPase);
            if (textoPase.isEmpty()) {
                errores.add("Indique si requiere pase previo (sí o no)");
            } else if (requierePase == null) {
                errores.add("«Requiere pase» debe ser sí o no");
            }
            EstadoActivacion estado = textoEstado.isEmpty() ? EstadoActivacion.ACTIVO : AnalizadorCsv.interpretarEstado(textoEstado);
            if (estado == null) {
                errores.add("El estado debe ser activa o inactiva");
            }
            if (!errores.isEmpty()) {
                return null;
            }
            Especialidad especialidad = new Especialidad();
            especialidad.setCodigo(codigo);
            especialidad.setNombre(nombre);
            especialidad.setDescripcion(descripcion.isEmpty() ? null : descripcion);
            especialidad.setRequierePase(requierePase);
            especialidad.setEstado(estado);
            return especialidad;
        });
    }

    /**
     * CU-30. Los criterios son opcionales y se combinan (todos tienen que cumplirse): {@code texto} busca en el código y en el
     * nombre (sin distinguir mayúsculas), {@code estado} filtra por ACTIVO o INACTIVO y {@code requierePase} por el requisito
     * de pase. Se ordena por nombre (por defecto) o por código, de forma ascendente salvo que {@code descendente} sea verdadero.
     * Sin criterios devuelve todas, por nombre: eso es lo que usan las pantallas para llenar sus listas desplegables.
     */
    @Transactional(readOnly = true)
    public List<EspecialidadResponseDTO> listarEspecialidades(CriteriosBusquedaDTO criterios) {
        EstadoActivacion estado = FiltrosBusqueda.interpretarEstado(criterios.estado());
        Sort orden = FiltrosBusqueda.ordenar(criterios.ordenarPor(), Boolean.TRUE.equals(criterios.descendente()), "nombre", "codigo");
        String patron = FiltrosBusqueda.patronContiene(criterios.texto());

        Specification<Especialidad> filtros = (especialidad, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (patron != null) {
                condiciones.add(cb.or(
                    cb.like(cb.lower(especialidad.get("codigo")), patron, FiltrosBusqueda.ESCAPE),
                    cb.like(cb.lower(especialidad.get("nombre")), patron, FiltrosBusqueda.ESCAPE)));
            }
            if (estado != null) {
                condiciones.add(cb.equal(especialidad.get("estado"), estado));
            }
            if (criterios.requierePase() != null) {
                condiciones.add(cb.equal(especialidad.get("requierePase"), criterios.requierePase()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return especialidadRepository.findAll(filtros, orden).stream().map(EspecialidadService::convertirAEspecialidadResponseDTO).toList();
    }

    /** CU-30. */
    public EspecialidadResponseDTO obtenerEspecialidad(String codigoEspecialidad) {
        return convertirAEspecialidadResponseDTO(buscarEspecialidad(codigoEspecialidad));
    }

    /**
     * CU-30. Detalle completo: los datos de la especialidad, los médicos activos que la atienden y sus turnos semanales
     * vigentes (los que no terminaron; los suspendidos se incluyen con su estado, porque pueden rehabilitarse). Solo lectura.
     */
    @Transactional(readOnly = true)
    public EspecialidadDetalleResponseDTO obtenerDetalleEspecialidad(String codigoEspecialidad) {
        Especialidad especialidad = buscarEspecialidad(codigoEspecialidad);
        List<EspecialidadDetalleResponseDTO.MedicoAsociado> medicos = medicoRepository
            .listarPorEspecialidad(especialidad.getCodigo(), EstadoActivacion.ACTIVO).stream()
            .map(EspecialidadService::convertirAMedicoAsociado)
            .toList();
        List<EspecialidadDetalleResponseDTO.TurnoAsociado> turnos = turnoSemanalRepository
            .listarVigentesDeEspecialidad(especialidad.getCodigo(), LocalDate.now(reloj)).stream()
            .sorted(Comparator.comparing(TurnoSemanal::getDia).thenComparing(TurnoSemanal::getHoraInicio).thenComparing(TurnoSemanal::getIdTurnoSemanal))
            .map(EspecialidadService::convertirATurnoAsociado)
            .toList();
        return new EspecialidadDetalleResponseDTO(especialidad.getCodigo(), especialidad.getNombre(), especialidad.getDescripcion(),
            especialidad.isRequierePase(), especialidad.getEstado(), medicos, turnos);
    }

    private Especialidad buscarEspecialidad(String codigoEspecialidad) {
        return especialidadRepository.findById(codigoEspecialidad)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La especialidad no existe"));
    }

    private static EspecialidadDetalleResponseDTO.MedicoAsociado convertirAMedicoAsociado(Medico medico) {
        return new EspecialidadDetalleResponseDTO.MedicoAsociado(medico.getIdUsuario(), medico.nombreCompleto(), medico.getMatricula());
    }

    private static EspecialidadDetalleResponseDTO.TurnoAsociado convertirATurnoAsociado(TurnoSemanal turno) {
        return new EspecialidadDetalleResponseDTO.TurnoAsociado(turno.getIdTurnoSemanal(), turno.getMedico().nombreCompleto(),
            turno.getDia(), turno.getHoraInicio(), turno.getHoraFin(), turno.getConsultorio().getCodigo(),
            turno.getConsultorio().getNombre(), turno.getEstado());
    }

    /**
     * CU-43. Cambia nombre, descripción, requisito de pase y estado (el código no se modifica). Los campos obligatorios los valida
     * el controlador (@Valid). El nombre no puede ser el de otra especialidad (409) y no se puede inactivar una especialidad
     * activa mientras tenga turnos semanales vigentes (409). El requisito de pase solo rige para reservas nuevas: las ya hechas
     * no se tocan. Si hubo algún cambio, se registra el administrador y la fecha y hora (el último cambio); si no cambió nada,
     * no se guarda nada.
     */
    @Transactional
    public EspecialidadResponseDTO modificarEspecialidad(Long idAdministrador, String codigoEspecialidad, ModificarEspecialidadDTO datos) {
        Especialidad especialidad = especialidadRepository.buscarConLock(codigoEspecialidad)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La especialidad no existe"));
        String nombre = datos.nombre().trim();
        String descripcion = datos.descripcion() == null || datos.descripcion().isBlank() ? null : datos.descripcion().trim();

        if (especialidadRepository.existsByNombreIgnoreCaseAndCodigoNot(nombre, especialidad.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre ya está en uso por otra especialidad");
        }
        boolean seInactiva = especialidad.getEstado() == EstadoActivacion.ACTIVO && datos.estado() == EstadoActivacion.INACTIVO;
        if (seInactiva && !turnoSemanalRepository.listarVigentesDeEspecialidad(especialidad.getCodigo(), LocalDate.now(reloj)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede inactivar la especialidad mientras tenga turnos de atención vigentes");
        }

        boolean hayCambios = !nombre.equals(especialidad.getNombre())
            || !Objects.equals(descripcion, especialidad.getDescripcion())
            || datos.requierePase() != especialidad.isRequierePase()
            || datos.estado() != especialidad.getEstado();
        if (hayCambios) {
            especialidad.setNombre(nombre);
            especialidad.setDescripcion(descripcion);
            especialidad.setRequierePase(datos.requierePase());
            especialidad.setEstado(datos.estado());
            especialidad.setAdministradorModificacion(administradorRepository.getReferenceById(idAdministrador));
            especialidad.setFechaModificacion(LocalDateTime.now(reloj));
            try {
                especialidadRepository.saveAndFlush(especialidad);
            } catch (DataIntegrityViolationException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre ya está en uso por otra especialidad");
            }
        }
        return convertirAEspecialidadResponseDTO(especialidad);
    }
}
