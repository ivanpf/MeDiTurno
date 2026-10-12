import { useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './AltaEspecialidadPage.module.css'

const FORMULARIO_VACIO = { codigo: '', nombre: '', descripcion: '', requierePase: 'no', estado: 'ACTIVO' }

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// CU-28 Alta individual de especialidades médicas.
export default function AltaEspecialidadPage() {
  const [datos, setDatos] = useState(FORMULARIO_VACIO)
  const [error, setError] = useState(null)
  const [registrada, setRegistrada] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (e) => {
    setDatos({ ...datos, [e.target.name]: e.target.value })
    setRegistrada(false)
  }

  const limpiar = () => {
    setDatos(FORMULARIO_VACIO)
    setError(null)
    setRegistrada(false)
  }

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    setRegistrada(false)
    setEnviando(true)
    try {
      await api.post('/especialidades', { ...datos, requierePase: datos.requierePase === 'si' })
      setDatos(FORMULARIO_VACIO)
      setRegistrada(true)
    } catch (err) {
      // Con un error del backend el formulario conserva los datos ingresados (flujos 4.A y 4.B).
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible registrar la especialidad. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Alta de especialidad</h1>

      <div className={estilos.mensajes}>
        {registrada && <p className="mensaje-exito" role="status">Especialidad registrada correctamente.</p>}
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

      <form className={estilos.formulario} onSubmit={guardar}>
        <div className={estilos.fila}>
          <label>
            Código *
            <input name="codigo" value={datos.codigo} onChange={cambiar} maxLength={20} required />
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
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={enviando}>Guardar</button>
          <button type="button" onClick={limpiar} disabled={enviando}>Cancelar</button>
        </div>
      </form>
    </section>
  )
}
