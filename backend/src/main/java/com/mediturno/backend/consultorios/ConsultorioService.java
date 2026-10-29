package com.mediturno.backend.consultorios;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.busqueda.FiltrosBusqueda;
import com.mediturno.backend.common.csv.AnalizadorCsv;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.dto.AltaConsultorioDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioDetalleResponseDTO;
import com.mediturno.backend.consultorios.dto.ConsultorioResponseDTO;
import com.mediturno.backend.consultorios.dto.ModificarConsultorioDTO;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;

/**
 * Lógica de negocio de: CU-37, CU-38, CU-39, CU-47.
 */
@Service
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;
    private final TurnoSemanalRepository turnoSemanalRepository;
    private final ReservaRepository reservaRepository;
    private final AdministradorRepository administradorRepository;
    private final Clock reloj;

    public ConsultorioService(ConsultorioRepository consultorioRepository, TurnoSemanalRepository turnoSemanalRepository,
            ReservaRepository reservaRepository, AdministradorRepository administradorRepository, Clock reloj) {
        this.consultorioRepository = consultorioRepository;
        this.turnoSemanalRepository = turnoSemanalRepository;
        this.reservaRepository = reservaRepository;
        this.administradorRepository = administradorRepository;
        this.reloj = reloj;
    }

    /** CU-37. */
    @Transactional
    public ConsultorioResponseDTO altaConsultorio(AltaConsultorioDTO datos) {

        // 1. Validación manual de campos obligatorios
        if (datos.codigo() == null || datos.codigo().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código del consultorio es obligatorio.");
        }
        if (datos.nombre() == null || datos.nombre().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del consultorio es obligatorio.");
        }
        if (datos.ubicacion() == null || datos.ubicacion().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ubicación del consultorio es obligatoria.");
        }
        if (datos.estado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del consultorio es obligatorio.");
        }
        // Nota: La descripción no es obligatoria según tu modelo (nullable = true).

        // 2. Validación de código duplicado (Flujo alternativo 4.B.1)
        if (consultorioRepository.existsById(datos.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ingresado ya corresponde a un consultorio existente.");
        }

        // 3. Mapear DTO a Entidad
        Consultorio consultorio = new Consultorio();
        consultorio.setCodigo(datos.codigo());
        consultorio.setNombre(datos.nombre());
        consultorio.setUbicacion(datos.ubicacion());
        consultorio.setDescripcion(datos.descripcion());
        consultorio.setEstado(datos.estado());

        // 4. Guardar en la base de datos
        Consultorio consultorioGuardado = consultorioRepository.save(consultorio);

        // 5. Devolver ResponseDTO
        return convertirAConsultorioResponseDTO(consultorioGuardado, null);
    }

    private static ConsultorioResponseDTO convertirAConsultorioResponseDTO(Consultorio c, Integer cantidadTurnos) {
        return new ConsultorioResponseDTO(c.getCodigo(), c.getNombre(), c.getUbicacion(), c.getDescripcion(), c.getEstado(), cantidadTurnos);
    }

    private static final int MAXIMO_FILAS_CSV = 1000;
    private static final String CODIGO = "Código";
    private static final String NOMBRE = "Nombre";
    private static final String UBICACION = "Ubicación";
    private static final String DESCRIPCION = "Descripción";
    private static final String ESTADO = "Estado";
    private static final List<AnalizadorCsv.Columna> COLUMNAS_CSV = List.of(
        AnalizadorCsv.Columna.obligatoria(CODIGO), AnalizadorCsv.Columna.obligatoria(NOMBRE),
        AnalizadorCsv.Columna.obligatoria(UBICACION), AnalizadorCsv.Columna.obligatoria(DESCRIPCION),
        AnalizadorCsv.Columna.obligatoria(ESTADO));

    /**
     * CU-38, previsualización. Lee el CSV y valida cada fila (campos obligatorios, largos, estado y códigos repetidos en el
     * archivo o ya registrados) sin guardar nada. Es el paso previo a la importación.
     */
    @Transactional(readOnly = true)
    public ResultadoImportacionDTO previsualizarConsultoriosCSV(MultipartFile archivo) {
        return analizarCsv(archivo).resultado();
    }

    /**
     * CU-38. Vuelve a analizar el archivo (nada de lo que mostró la previsualización se da por cierto) y, solo si todas las filas
     * son válidas, crea todos los consultorios en una sola transacción; si alguna tiene errores no se crea ninguno (422).
     * Devuelve el resultado con la cantidad de consultorios creados.
     */
    @Transactional
    public ResultadoImportacionDTO altaConsultoriosCSV(MultipartFile archivo) {
        return analizarCsv(archivo).importar(consultorioRepository::saveAllAndFlush,
            "El archivo contiene errores: corríjalo y vuelva a cargarlo. No se creó ningún consultorio.",
            // Otro administrador creó alguno en el mismo instante: la clave primaria lo detecta.
            "Algún consultorio se registró mientras se importaba el archivo. Vuelva a cargarlo. No se creó ningún consultorio.");
    }

    private AnalizadorCsv.Analisis<Consultorio> analizarCsv(MultipartFile archivo) {
        Set<String> codigosRegistrados = new HashSet<>();
        consultorioRepository.findAll().forEach(c -> codigosRegistrados.add(c.getCodigo()));
        AnalizadorCsv.Repetidos codigosDelArchivo = new AnalizadorCsv.Repetidos();

        return AnalizadorCsv.analizar(archivo, MAXIMO_FILAS_CSV, "consultorios", COLUMNAS_CSV, (fila, errores) -> {
            String codigo = fila.valor(CODIGO);
            String nombre = fila.valor(NOMBRE);
            String ubicacion = fila.valor(UBICACION);
            String descripcion = fila.valor(DESCRIPCION);
            String textoEstado = fila.valor(ESTADO);
            // Un código repetido en el archivo se marca en la segunda aparición, aunque la primera tuviera otros errores.
            Integer codigoPrevio = codigosDelArchivo.vistoEn(codigo, fila.numero());

            if (codigo.isEmpty()) {
                errores.add("El código es obligatorio");
            } else if (codigo.length() > 20) {
                errores.add("El código admite hasta 20 caracteres");
            } else if (codigosRegistrados.contains(codigo)) {
                errores.add("El código ya está en uso por otro consultorio");
            } else if (codigoPrevio != null) {
                errores.add("El código está repetido en el archivo (también en la fila " + codigoPrevio + ")");
            }
            if (nombre.isEmpty()) {
                errores.add("El nombre es obligatorio");
            } else if (nombre.length() > 100) {
                errores.add("El nombre admite hasta 100 caracteres");
            }
            if (ubicacion.isEmpty()) {
                errores.add("La ubicación es obligatoria");
            } else if (ubicacion.length() > 200) {
                errores.add("La ubicación admite hasta 200 caracteres");
            }
            if (descripcion.length() > 500) {
                errores.add("La descripción admite hasta 500 caracteres");
            }
            EstadoActivacion estado = AnalizadorCsv.interpretarEstado(textoEstado);
            if (textoEstado.isEmpty()) {
                errores.add("El estado es obligatorio (activo o inactivo)");
            } else if (estado == null) {
                errores.add("El estado debe ser activo o inactivo");
            }
            if (!errores.isEmpty()) {
                return null;
            }
            Consultorio consultorio = new Consultorio();
            consultorio.setCodigo(codigo);
            consultorio.setNombre(nombre);
            consultorio.setUbicacion(ubicacion);
            consultorio.setDescripcion(descripcion.isEmpty() ? null : descripcion);
            consultorio.setEstado(estado);
            return consultorio;
        });
    }

    /**
     * CU-39. Los criterios son opcionales y se combinan (todos tienen que cumplirse): {@code texto} busca en el código y en el
     * nombre (sin distinguir mayúsculas), {@code ubicacion} filtra por esa ubicación exacta (sin distinguir mayúsculas) y
     * {@code estado} por ACTIVO o INACTIVO. Se ordena por nombre (por defecto), código o ubicación, de forma ascendente salvo
     * que {@code descendente} sea verdadero. Cada consultorio trae la cantidad de turnos semanales vigentes que lo usan.
     * Sin criterios devuelve todos, por nombre: eso es lo que usan las pantallas para llenar sus listas desplegables.
     */
    @Transactional(readOnly = true)
    public List<ConsultorioResponseDTO> listarConsultorios(CriteriosBusquedaDTO criterios) {
        EstadoActivacion estado = FiltrosBusqueda.interpretarEstado(criterios.estado());
        Sort orden = FiltrosBusqueda.ordenar(criterios.ordenarPor(), Boolean.TRUE.equals(criterios.descendente()),
            "nombre", "codigo", "ubicacion");
        String patron = FiltrosBusqueda.patronContiene(criterios.texto());
        String ubicacion = criterios.ubicacion() == null || criterios.ubicacion().isBlank() ? null : criterios.ubicacion().trim().toLowerCase();

        Specification<Consultorio> filtros = (consultorio, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (patron != null) {
                condiciones.add(cb.or(
                    cb.like(cb.lower(consultorio.get("codigo")), patron, FiltrosBusqueda.ESCAPE),
                    cb.like(cb.lower(consultorio.get("nombre")), patron, FiltrosBusqueda.ESCAPE)));
            }
            if (ubicacion != null) {
                condiciones.add(cb.equal(cb.lower(consultorio.get("ubicacion")), ubicacion));
            }
            if (estado != null) {
                condiciones.add(cb.equal(consultorio.get("estado"), estado));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        Map<String, Integer> turnosPorConsultorio = new HashMap<>();
        turnoSemanalRepository.contarVigentesPorConsultorio(LocalDate.now(reloj))
            .forEach(c -> turnosPorConsultorio.put(c.codigoConsultorio(), (int) c.cantidad()));
        return consultorioRepository.findAll(filtros, orden).stream()
            .map(c -> convertirAConsultorioResponseDTO(c, turnosPorConsultorio.getOrDefault(c.getCodigo(), 0)))
            .toList();
    }

    /**
     * CU-39. Detalle completo: los datos del consultorio y los turnos semanales vigentes que lo utilizan (los que no terminaron;
     * los suspendidos se incluyen con su estado, porque pueden rehabilitarse). Solo lectura.
     */
    @Transactional(readOnly = true)
    public ConsultorioDetalleResponseDTO obtenerDetalleConsultorio(String codigoConsultorio) {
        Consultorio consultorio = consultorioRepository.findById(codigoConsultorio)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El consultorio no existe"));
        List<ConsultorioDetalleResponseDTO.TurnoAsociado> turnos = turnoSemanalRepository
            .listarVigentesDeConsultorio(consultorio.getCodigo(), LocalDate.now(reloj)).stream()
            .sorted(Comparator.comparing(TurnoSemanal::getDia).thenComparing(TurnoSemanal::getHoraInicio).thenComparing(TurnoSemanal::getIdTurnoSemanal))
            .map(ConsultorioService::convertirATurnoAsociado)
            .toList();
        return new ConsultorioDetalleResponseDTO(consultorio.getCodigo(), consultorio.getNombre(), consultorio.getUbicacion(),
            consultorio.getDescripcion(), consultorio.getEstado(), turnos);
    }

    private static ConsultorioDetalleResponseDTO.TurnoAsociado convertirATurnoAsociado(TurnoSemanal turno) {
        return new ConsultorioDetalleResponseDTO.TurnoAsociado(turno.getIdTurnoSemanal(), turno.getMedico().nombreCompleto(),
            turno.getEspecialidad().getNombre(), turno.getDia(), turno.getHoraInicio(), turno.getHoraFin(), turno.getEstado());
    }

    /**
     * CU-47. Cambia nombre, ubicación, descripción y estado (el código no se modifica). Los campos obligatorios los valida el
     * controlador (@Valid). No se puede inactivar un consultorio activo mientras tenga turnos semanales vigentes (409), ni
     * cambiar su ubicación mientras haya reservas pendientes de esos turnos que todavía no se atendieron (409). Las órdenes ya
     * emitidas no se tocan. Si hubo algún cambio, se registra el administrador y la fecha y hora (el último cambio); si no
     * cambió nada, no se guarda nada.
     */
    @Transactional
    public ConsultorioResponseDTO modificarConsultorio(Long idAdministrador, String codigoConsultorio, ModificarConsultorioDTO datos) {
        Consultorio consultorio = consultorioRepository.buscarConLock(codigoConsultorio)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El consultorio no existe"));
        String nombre = datos.nombre().trim();
        String ubicacion = datos.ubicacion().trim();
        String descripcion = datos.descripcion() == null || datos.descripcion().isBlank() ? null : datos.descripcion().trim();

        boolean seInactiva = consultorio.getEstado() == EstadoActivacion.ACTIVO && datos.estado() == EstadoActivacion.INACTIVO;
        if (seInactiva && !turnoSemanalRepository.listarVigentesDeConsultorio(consultorio.getCodigo(), LocalDate.now(reloj)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede inactivar el consultorio mientras tenga turnos de atención vigentes");
        }
        boolean cambiaUbicacion = !ubicacion.equals(consultorio.getUbicacion());
        if (cambiaUbicacion && reservaRepository.existenReservasPendientesEnConsultorio(
                consultorio.getCodigo(), LocalDate.now(reloj), LocalTime.now(reloj))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede cambiar la ubicación del consultorio mientras existan reservas futuras vinculadas a sus turnos");
        }

        boolean hayCambios = !nombre.equals(consultorio.getNombre())
            || cambiaUbicacion
            || !Objects.equals(descripcion, consultorio.getDescripcion())
            || datos.estado() != consultorio.getEstado();
        if (hayCambios) {
            consultorio.setNombre(nombre);
            consultorio.setUbicacion(ubicacion);
            consultorio.setDescripcion(descripcion);
            consultorio.setEstado(datos.estado());
            consultorio.setAdministradorModificacion(administradorRepository.getReferenceById(idAdministrador));
            consultorio.setFechaModificacion(LocalDateTime.now(reloj));
            consultorioRepository.saveAndFlush(consultorio);
        }
        return convertirAConsultorioResponseDTO(consultorio, null);
    }
}
