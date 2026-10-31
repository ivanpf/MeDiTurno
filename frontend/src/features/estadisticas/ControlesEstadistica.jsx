import estilos from './ControlesEstadistica.module.css'

// Piezas que comparten las tres pantallas de estadísticas (CU-40, CU-41 y CU-42).

// El formulario de filtros: los campos que se le pasen, más los botones Consultar y Limpiar.
export function FormularioEstadistica({ estadistica, children }) {
  const { buscar, limpiar, buscando } = estadistica
  return (
    <form className="listado-filtros" onSubmit={buscar}>
      <div className="listado-campos">{children}</div>
      <div className="listado-acciones">
        <button type="submit" className="principal" disabled={buscando}>Consultar</button>
        <button type="button" onClick={limpiar} disabled={buscando}>Limpiar</button>
      </div>
    </form>
  )
}

export function CamposPeriodo({ filtros, cambiarFiltro }) {
  return (
    <>
      <label>
        Desde
        <input type="date" name="fechaDesde" value={filtros.fechaDesde} onChange={cambiarFiltro} max={filtros.fechaHasta || undefined} />
      </label>
      <label>
        Hasta
        <input type="date" name="fechaHasta" value={filtros.fechaHasta} onChange={cambiarFiltro} min={filtros.fechaDesde || undefined} />
      </label>
    </>
  )
}

// Lista desplegable del orden. Cada opción es { ordenarPor, descendente, etiqueta }; la elegida se guarda en los filtros.
export function SelectorOrden({ opciones, filtros, setFiltros }) {
  const clave = (o) => `${o.ordenarPor}|${o.descendente}`
  const elegir = (e) => {
    const opcion = opciones.find((o) => clave(o) === e.target.value)
    setFiltros({ ...filtros, ordenarPor: opcion.ordenarPor, descendente: opcion.descendente })
  }
  return (
    <label>
      Ordenar
      <select value={clave(filtros)} onChange={elegir}>
        {opciones.map((o) => <option key={clave(o)} value={clave(o)}>{o.etiqueta}</option>)}
      </select>
    </label>
  )
}

// Un porcentaje como número y como barra.
export function Porcentaje({ valor }) {
  const texto = `${valor.toLocaleString('es-UY', { maximumFractionDigits: 2 })} %`
  return (
    <div className={estilos.porcentaje}>
      <progress className={estilos.barra} value={valor} max={100} aria-label={texto} />
      <span>{texto}</span>
    </div>
  )
}

export function SinResultados() {
  return <p className="pendiente" role="status">No hay resultados disponibles para los filtros aplicados.</p>
}
