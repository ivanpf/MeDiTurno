import { hora, NOMBRE_DIA } from '../../utils/formatos'
import { CamposPeriodo, FormularioEstadistica, SelectorOrden, SinResultados } from './ControlesEstadistica'
import { filtroMedicos, periodoPorDefecto, useEstadistica, useListas } from './useEstadistica'

const ORDENES = [
  { ordenarPor: '', descendente: true, etiqueta: 'Más demandados primero' },
  { ordenarPor: '', descendente: false, etiqueta: 'Menos demandados primero' },
]

const FILTROS_INICIALES = { ...periodoPorDefecto(), codigoEspecialidad: '', idMedico: '', ordenarPor: '', descendente: true }

// CU-41 Estadística de turnos de atención más demandados: ranking de los turnos semanales por reservas recibidas en un período.
export default function TurnosMasDemandadosPage() {
  const estadistica = useEstadistica('/estadisticas/turnos-mas-demandados', FILTROS_INICIALES)
  const { filtros, setFiltros, cambiarFiltro, resultado, error } = estadistica
  const { listas, errorListas } = useListas('medicos', 'especialidades')

  const { medicos, cambiarEspecialidad } = filtroMedicos(listas, filtros, setFiltros)

  return (
    <section className="panel">
      <h1>Turnos más demandados</h1>
      <p className="texto-vacio">Se cuentan las reservas de todos los estados con fecha de atención dentro del período.</p>

      {(error || errorListas) && <p className="mensaje-error" role="alert">{(error || errorListas).mensaje}</p>}

      <FormularioEstadistica estadistica={estadistica}>
        <CamposPeriodo filtros={filtros} cambiarFiltro={cambiarFiltro} />
        <label>
          Especialidad
          <select name="codigoEspecialidad" value={filtros.codigoEspecialidad} onChange={cambiarEspecialidad}>
            <option value="">Todas</option>
            {listas.especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}
          </select>
        </label>
        <label>
          Médico
          <select name="idMedico" value={filtros.idMedico} onChange={cambiarFiltro}>
            <option value="">Todos</option>
            {medicos.map((m) => <option key={m.idMedico} value={m.idMedico}>{m.apellido}, {m.nombre}</option>)}
          </select>
        </label>
        <SelectorOrden opciones={ORDENES} filtros={filtros} setFiltros={setFiltros} />
      </FormularioEstadistica>

      {resultado && resultado.length === 0 && <SinResultados />}

      {resultado && resultado.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>#</th>
                <th>Especialidad</th>
                <th>Médico</th>
                <th>Día</th>
                <th>Horario</th>
                <th>Consultorio</th>
                <th>Máx. números</th>
                <th>Reservas</th>
              </tr>
            </thead>
            <tbody>
              {resultado.map((turno, i) => (
                <tr key={turno.idTurnoSemanal}>
                  <td>{i + 1}</td>
                  <td>{turno.nombreEspecialidad}</td>
                  <td>{turno.nombreMedico}</td>
                  <td>{NOMBRE_DIA[turno.dia]}</td>
                  <td>{hora(turno.horaInicio)}-{hora(turno.horaFin)}</td>
                  <td>{turno.nombreConsultorio}</td>
                  <td>{turno.cupoMaximo}</td>
                  <td>{turno.cantidadReservas}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
