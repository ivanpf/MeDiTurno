import { CamposPeriodo, FormularioEstadistica, Porcentaje, SelectorOrden, SinResultados } from './ControlesEstadistica'
import { periodoPorDefecto, useEstadistica } from './useEstadistica'

const ESTADOS = [
  ['VIGENTE', 'Vigente'],
  ['ORDEN_EMITIDA', 'Orden emitida'],
  ['ATENDIDA', 'Atendida'],
  ['CANCELADA', 'Cancelada'],
  ['ANULADA', 'Anulada'],
  ['VENCIDA', 'Vencida'],
]

const ORDENES = [
  { ordenarPor: 'cantidad', descendente: true, etiqueta: 'Más reservas primero' },
  { ordenarPor: 'cantidad', descendente: false, etiqueta: 'Menos reservas primero' },
  { ordenarPor: 'nombre', descendente: false, etiqueta: 'Especialidad (A a Z)' },
  { ordenarPor: 'nombre', descendente: true, etiqueta: 'Especialidad (Z a A)' },
]

const FILTROS_INICIALES = { ...periodoPorDefecto(), estado: '', ordenarPor: 'cantidad', descendente: true }

// CU-40 Estadística de reservas por especialidad: cuántas reservas tuvo cada especialidad en un período y qué parte del total es.
export default function ReservasPorEspecialidadPage() {
  const estadistica = useEstadistica('/estadisticas/reservas-por-especialidad', FILTROS_INICIALES)
  const { filtros, setFiltros, cambiarFiltro, resultado, error } = estadistica
  const total = resultado?.reduce((suma, fila) => suma + fila.cantidadReservas, 0) ?? 0

  return (
    <section className="panel">
      <h1>Reservas por especialidad</h1>
      <p className="texto-vacio">El período se mide por la fecha de atención del turno. Sin elegir un estado se cuentan todas las reservas.</p>

      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <FormularioEstadistica estadistica={estadistica}>
        <CamposPeriodo filtros={filtros} cambiarFiltro={cambiarFiltro} />
        <label>
          Estado de la reserva
          <select name="estado" value={filtros.estado} onChange={cambiarFiltro}>
            <option value="">Todos</option>
            {ESTADOS.map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
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
                <th>Reservas</th>
                <th>Porcentaje del total</th>
              </tr>
            </thead>
            <tbody>
              {resultado.map((fila) => (
                <tr key={fila.codigoEspecialidad}>
                  <td>{fila.nombreEspecialidad}</td>
                  <td>{fila.cantidadReservas}</td>
                  <td><Porcentaje valor={fila.porcentaje} /></td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th>Total</th>
                <th>{total}</th>
                <th />
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </section>
  )
}
