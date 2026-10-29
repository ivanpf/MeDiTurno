import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'
import { fecha } from '../../utils/formatos'

const ORDENES = [
  ['apellido', 'Apellido'],
  ['nombre', 'Nombre'],
  ['fechaAlta', 'Fecha de alta'],
]

const FILTROS_VACIOS = { texto: '', tieneCuenta: '', estado: '', ordenarPor: 'apellido', descendente: false }

function Estado({ estado }) {
  return <span className={estado === 'ACTIVO' ? 'estado-activo' : 'estado-inactivo'}>{estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span>
}

// Si ya creó su cuenta o solo está en el padrón (todavía no se registró, CU-7).
function Cuenta({ tieneCuenta }) {
  return tieneCuenta ? <span className="estado-activo">Registrado</span> : <span>Sin registrar</span>
}

// CU-33 Listado y búsqueda de pacientes, con su detalle administrativo (sin historia clínica).
export default function ListadoPacientesPage() {
  const navigate = useNavigate()
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [pacientes, setPacientes] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState(null)

  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)
  const [errorDetalle, setErrorDetalle] = useState(null)
  const panelDetalle = useRef(null)

  const buscar = async (criterios) => {
    setBuscando(true)
    setError(null)
    try {
      const consulta = new URLSearchParams()
      Object.entries(criterios).forEach(([clave, valor]) => {
        if (valor !== '' && valor !== false) consulta.set(clave, valor)
      })
      setPacientes(await api.get(`/pacientes?${consulta}`))
    } catch (err) {
      setError(comoError(err, 'No fue posible consultar los pacientes. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  useEffect(() => {
    // Listado inicial: todo el padrón.
    api.get('/pacientes?ordenarPor=apellido')
      .then(setPacientes)
      .catch((err) => setError(comoError(err, 'No fue posible consultar los pacientes. Intente nuevamente.')))
  }, [])

  // El detalle se abre arriba de la tabla: se lleva la vista hasta él para que se note.
  useEffect(() => {
    if (detalle || errorDetalle) panelDetalle.current?.scrollIntoView({ block: 'start' })
  }, [detalle, errorDetalle])

  const cambiarFiltro = (e) => {
    const { name, type, checked, value } = e.target
    setFiltros({ ...filtros, [name]: type === 'checkbox' ? checked : value })
  }

  const enviarBusqueda = (e) => {
    e.preventDefault()
    buscar(filtros)
  }

  const limpiarFiltros = () => {
    setFiltros(FILTROS_VACIOS)
    buscar(FILTROS_VACIOS)
  }

  const verDetalle = async (idPaciente) => {
    setCargandoDetalle(true)
    setErrorDetalle(null)
    try {
      setDetalle(await api.get(`/pacientes/${idPaciente}`))
    } catch (err) {
      setDetalle(null)
      setErrorDetalle(comoError(err, 'No fue posible consultar el detalle del paciente. Intente nuevamente.'))
    } finally {
      setCargandoDetalle(false)
    }
  }

  const cerrarDetalle = () => {
    setDetalle(null)
    setErrorDetalle(null)
  }

  const modificar = (idPaciente) => navigate('/admin/pacientes/modificar', { state: { idPaciente } })

  return (
    <section className="panel">
      <h1>Pacientes</h1>

      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <form className="listado-filtros" onSubmit={enviarBusqueda}>
        <div className="listado-campos">
          <label>
            Buscar por cédula, nombre, apellido o correo
            <input type="search" name="texto" value={filtros.texto} onChange={cambiarFiltro} maxLength={100} />
          </label>
          <label>
            Registro en la plataforma
            <select name="tieneCuenta" value={filtros.tieneCuenta} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              <option value="true">Registrado (con cuenta)</option>
              <option value="false">Sin registrar (sin cuenta)</option>
            </select>
          </label>
          <label>
            Estado
            <select name="estado" value={filtros.estado} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
          <label>
            Ordenar por
            <select name="ordenarPor" value={filtros.ordenarPor} onChange={cambiarFiltro}>
              {ORDENES.map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
            </select>
          </label>
        </div>
        <label className="casilla-inversa">
          <input type="checkbox" name="descendente" checked={filtros.descendente} onChange={cambiarFiltro} />
          Orden descendente
        </label>
        <div className="listado-acciones">
          <button type="submit" className="principal" disabled={buscando}>Buscar</button>
          <button type="button" onClick={limpiarFiltros} disabled={buscando}>Limpiar</button>
        </div>
      </form>

      <div ref={panelDetalle} className="detalle-ancla">
        {errorDetalle && (
          <div className="mensaje-error" role="alert">
            {errorDetalle.mensaje}
            <div className="listado-acciones">
              <button type="button" onClick={cerrarDetalle}>Cerrar</button>
            </div>
          </div>
        )}

        {detalle && (
          <article className="detalle-panel" aria-labelledby="titulo-detalle">
            <h2 id="titulo-detalle">{detalle.apellido}, {detalle.nombre}</h2>
            <dl className="detalle-datos">
              <div>
                <dt>Cédula</dt>
                <dd>{detalle.cedula}</dd>
              </div>
              <div>
                <dt>Fecha de nacimiento</dt>
                <dd>{fecha(detalle.fechaNacimiento)}</dd>
              </div>
              <div>
                <dt>Correo electrónico</dt>
                <dd>{detalle.correo}</dd>
              </div>
              <div>
                <dt>Teléfono</dt>
                <dd>{detalle.telefono || 'Sin teléfono'}</dd>
              </div>
              <div className="detalle-ancho">
                <dt>Domicilio</dt>
                <dd>{detalle.domicilio || 'Sin domicilio'}</dd>
              </div>
              <div>
                <dt>Registro en la plataforma</dt>
                <dd><Cuenta tieneCuenta={detalle.tieneCuenta} /></dd>
              </div>
              <div>
                <dt>Estado</dt>
                <dd><Estado estado={detalle.estado} /></dd>
              </div>
              <div>
                <dt>Fecha de alta en el padrón</dt>
                <dd>{fecha(detalle.fechaAlta)}</dd>
              </div>
            </dl>
            <div className="listado-acciones">
              <button type="button" onClick={() => modificar(detalle.idPaciente)}>Modificar</button>
              <button type="button" onClick={cerrarDetalle}>Cerrar detalle</button>
            </div>
          </article>
        )}
      </div>

      {pacientes && pacientes.length === 0 && (
        <p className="pendiente" role="status">No se encontraron pacientes con los criterios seleccionados.</p>
      )}

      {pacientes && pacientes.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>Cédula</th>
                <th>Apellido</th>
                <th>Nombre</th>
                <th>Correo electrónico</th>
                <th>Teléfono</th>
                <th>Registro</th>
                <th>Estado</th>
                <th><span className="solo-lectores">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {pacientes.map((p) => (
                <tr key={p.idPaciente}>
                  <td>{p.cedula}</td>
                  <td>{p.apellido}</td>
                  <td>{p.nombre}</td>
                  <td>{p.correo}</td>
                  <td>{p.telefono}</td>
                  <td><Cuenta tieneCuenta={p.tieneCuenta} /></td>
                  <td><Estado estado={p.estado} /></td>
                  <td className="celda-botones">
                    <button type="button" onClick={() => verDetalle(p.idPaciente)} disabled={cargandoDetalle}>Ver detalle</button>
                    <button type="button" onClick={() => modificar(p.idPaciente)}>Modificar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
