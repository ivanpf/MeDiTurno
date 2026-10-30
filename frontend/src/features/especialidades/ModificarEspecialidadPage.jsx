import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { api } from '../../services/api'
import { comoError, soloMensaje } from '../../utils/errores'
import estilos from './ModificarEspecialidadPage.module.css'

const aFormulario = (e) => ({
  nombre: e.nombre,
  descripcion: e.descripcion ?? '',
  requierePase: e.requierePase ? 'si' : 'no',
  estado: e.estado,
})

// CU-43 Modificación de especialidades médicas. El código no se puede modificar.
export default function ModificarEspecialidadPage() {
  // Desde el listado (CU-30) se llega con la especialidad ya elegida.
  const { state } = useLocation()
  const [especialidades, setEspecialidades] = useState([])
  const [codigo, setCodigo] = useState(state?.codigo ?? '')
  const [borrador, setBorrador] = useState(null) // lo escrito en el formulario; vacío = los datos guardados
  const [error, setError] = useState(null)
  const [modificada, setModificada] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cargarLista = () =>
    api.get('/especialidades')
      .then(setEspecialidades)
      .catch((err) => setError(comoError(err, 'No fue posible cargar las especialidades.')))

  useEffect(() => {
    cargarLista()
  }, [])

  // Los datos del formulario son los guardados de la especialidad elegida, salvo lo que la persona haya escrito.
  const original = especialidades.find((e) => e.codigo === codigo)
  const datos = borrador ?? (original ? aFormulario(original) : null)

  const elegir = (e) => {
    setCodigo(e.target.value)
    setBorrador(null)
    setError(null)
    setModificada(false)
  }

  const cambiar = (e) => {
    setBorrador({ ...datos, [e.target.name]: e.target.value })
    setModificada(false)
  }

  const restablecer = () => {
    setBorrador(null)
    setError(null)
    setModificada(false)
  }

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    setModificada(false)
    setEnviando(true)
    try {
      await api.put(`/especialidades/${encodeURIComponent(codigo)}`, {
        nombre: datos.nombre,
        descripcion: datos.descripcion,
        requierePase: datos.requierePase === 'si',
        estado: datos.estado,
      })
      await cargarLista()
      setBorrador(null)
      setModificada(true)
    } catch (err) {
      // Con un error del backend el formulario conserva lo que se escribió.
      setError(comoError(err, 'No fue posible modificar la especialidad. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Modificación de especialidad</h1>

      <div className={estilos.mensajes}>
        {modificada && <p className="mensaje-exito" role="status">Especialidad modificada correctamente.</p>}
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
          Especialidad
          <select value={codigo} onChange={elegir}>
            <option value="">Seleccione una especialidad</option>
            {especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre} ({e.codigo})</option>)}
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
            Descripción
            <textarea name="descripcion" value={datos.descripcion} onChange={cambiar} maxLength={500} rows={4} />
          </label>
          <div className={estilos.fila}>
            <fieldset className={estilos.opciones}>
              <legend>¿Requiere pase previo? *</legend>
              <label className={estilos.opcion}>
                <input type="radio" name="requierePase" value="si" checked={datos.requierePase === 'si'} onChange={cambiar} />
                Sí
              </label>
              <label className={estilos.opcion}>
                <input type="radio" name="requierePase" value="no" checked={datos.requierePase === 'no'} onChange={cambiar} />
                No
              </label>
            </fieldset>
            <label>
              Estado *
              <select name="estado" value={datos.estado} onChange={cambiar} required>
                <option value="ACTIVO">Activa</option>
                <option value="INACTIVO">Inactiva</option>
              </select>
            </label>
          </div>
          <p className={estilos.ayuda}>
            El cambio en el pase previo se aplica a las reservas nuevas; las ya realizadas no se modifican. No se puede inactivar una
            especialidad que tenga turnos de atención vigentes.
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
