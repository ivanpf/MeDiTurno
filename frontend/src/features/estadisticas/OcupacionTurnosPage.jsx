import { hora, NOMBRE_DIA } from '../../utils/formatos'
import { CamposPeriodo, FormularioEstadistica, Porcentaje, SelectorOrden, SinResultados } from './ControlesEstadistica'
import { filtroMedicos, periodoPorDefecto, useEstadistica, useListas } from './useEstadistica'

const ORDENES = [
  { ordenarPor: '', descendente: true, etiqueta: 'Más ocupados primero' },
  { ordenarPor: '', descendente: false, etiqueta: 'Menos ocupados primero' },
]

const FILTROS_INICIALES = {
  ...periodoPorDefecto(),
  codigoEspecialidad: '',
  idMedico: '',
  codigoConsultorio: '',
  ordenarPor: '',
  descendente: true,
}

// CU-42 Estadística de ocupación de turnos: de los cupos que ofreció cada horario de atención en un período, qué porcentaje se reservó.
export default function OcupacionTurnosPage() {
  const estadistica = useEstadistica('/estadisticas/ocupacion-turnos', FILTROS_INICIALES)
  const { filtros, setFiltros, cambiarFiltro, resultado, error } = estadistica
  const { listas, errorListas } = useListas('medicos', 'especialidades', 'consultorios')

  const { medicos, cambiarEspecialidad } = filtroMedicos(listas, filtros, setFiltros)

  return (
    <section className="panel">
      <h1>Ocupación de turnos</h1>
      <p className="texto-vacio">
        Los cupos ofrecidos suman los de las fechas del período (sin las suspendidas). Se consideran reservados los números que una
        reserva conserva: todas menos las canceladas y las anuladas.
      </p>

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
        <label>
          Consultorio
          <select name="codigoConsultorio" value={filtros.codigoConsultorio} onChange={cambiarFiltro}>
            <option value="">Todos</option>
            {listas.consultorios.map((c) => <option key={c.codigo} value={c.codigo}>{c.nombre}</option>)}
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
                <th>Especialidad</th>
                <th>Médico</th>
                <th>Día</th>
                <th>Horario</th>
                <th>Consultorio</th>
                <th>Cupos ofrecidos</th>
                <th>Reservados</th>
                <th>Reservas vigentes</th>
                <th>No reservados</th>
                <th>Ocupación</th>
              </tr>
            </thead>
            <tbody>
              {resultado.map((turno) => (
                <tr key={turno.idTurnoSemanal}>
                  <td>{turno.nombreEspecialidad}</td>
                  <td>{turno.nombreMedico}</td>
                  <td>{NOMBRE_DIA[turno.dia]}</td>
                  <td>{hora(turno.horaInicio)}-{hora(turno.horaFin)}</td>
                  <td>{turno.nombreConsultorio}</td>
                  <td>{turno.cuposOfrecidos}</td>
                  <td>{turno.cuposReservados}</td>
                  <td>{turno.reservasVigentes}</td>
                  <td>{turno.cuposNoReservados}</td>
                  <td><Porcentaje valor={turno.porcentajeOcupacion} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
