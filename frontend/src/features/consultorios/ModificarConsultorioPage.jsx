import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { api } from '../../services/api'
import { comoError, soloMensaje } from '../../utils/errores'
import estilos from './ModificarConsultorioPage.module.css'

const aFormulario = (c) => ({
  nombre: c.nombre,
  ubicacion: c.ubicacion,
  descripcion: c.descripcion ?? '',
  estado: c.estado,
})

// CU-47 Modificación de consultorios. El código no se puede modificar.
export default function ModificarConsultorioPage() {
  // Desde el listado (CU-39) se llega con el consultorio ya elegido.
  const { state } = useLocation()
  const [consultorios, setConsultorios] = useState([])
  const [codigo, setCodigo] = useState(state?.codigo ?? '')
  const [borrador, setBorrador] = useState(null) // lo escrito en el formulario; vacío = los datos guardados
  const [error, setError] = useState(null)
  const [modificado, setModificado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cargarLista = () =>
    api.get('/consultorios')
      .then(setConsultorios)
      .catch((err) => setError(comoError(err, 'No fue posible cargar los consultorios.')))

  useEffect(() => {
    cargarLista()
  }, [])

  // Los datos del formulario son los guardados del consultorio elegido, salvo lo que la persona haya escrito.
  const original = consultorios.find((c) => c.codigo === codigo)
  const datos = borrador ?? (original ? aFormulario(original) : null)

  const elegir = (e) => {
    setCodigo(e.target.value)
    setBorrador(null)
    setError(null)
    setModificado(false)
  }

  const cambiar = (e) => {
    setBorrador({ ...datos, [e.target.name]: e.target.value })
    setModificado(false)
  }

  const restablecer = () => {
    setBorrador(null)
    setError(null)
    setModificado(false)
  }

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    setModificado(false)
    setEnviando(true)
    try {
      await api.put(`/consultorios/${encodeURIComponent(codigo)}`, {
        nombre: datos.nombre,
        ubicacion: datos.ubicacion,
        descripcion: datos.descripcion,
        estado: datos.estado,
      })
      await cargarLista()
      setBorrador(null)
      setModificado(true)
    } catch (err) {
      // Con un error del backend el formulario conserva lo que se escribió.
      setError(comoError(err, 'No fue posible modificar el consultorio. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Modificación de consultorio</h1>

      <div className={estilos.mensajes}>
        {modificado && <p className="mensaje-exito" role="status">Consultorio modificado correctamente.</p>}
        {error && (
          <div className="mensaje-error" role="alert">
            {error.mensaje}
            {error.detalles.length > 0 && (
              <ul>
                {error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}
              </ul>
            )}
          </div>
        )}
      </div>

      <div className={estilos.formulario}>
        <label>
          Consultorio
          <select value={codigo} onChange={elegir}>
            <option value="">Seleccione un consultorio</option>
            {consultorios.map((c) => <option key={c.codigo} value={c.codigo}>{c.nombre} ({c.codigo})</option>)}
          </select>
        </label>
      </div>

      {datos && (
        <form className={estilos.formulario} onSubmit={guardar}>
          <div className={estilos.fila}>
            <label>
              Código
              <input value={codigo} disabled />
            </label>
            <label>
              Nombre *
              <input name="nombre" value={datos.nombre} onChange={cambiar} maxLength={100} required />
            </label>
          </div>
          <label>
            Ubicación *
            <input name="ubicacion" value={datos.ubicacion} onChange={cambiar} maxLength={200} required />
          </label>
          <label>
            Descripción
            <textarea name="descripcion" value={datos.descripcion} onChange={cambiar} maxLength={500} rows={4} />
          </label>
          <label>
            Estado *
            <select name="estado" value={datos.estado} onChange={cambiar} required>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
          <p className={estilos.ayuda}>
            No se puede inactivar un consultorio que tenga turnos de atención vigentes, ni cambiar su ubicación mientras existan
            reservas futuras vinculadas a esos turnos. Las órdenes ya emitidas no se modifican.
          </p>
          <div className={estilos.acciones}>
            <button type="submit" className="principal" disabled={enviando}>Guardar cambios</button>
            <button type="button" onClick={restablecer} disabled={enviando}>Deshacer cambios</button>
          </div>
        </form>
      )}
    </section>
  )
}
