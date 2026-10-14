import { useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './AltaConsultorioPage.module.css'

const FORMULARIO_VACIO = { codigo: '', nombre: '', ubicacion: '', descripcion: '', estado: 'ACTIVO' }
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// CU-37 Alta individual de consultorios, siguiendo los campos del mock.
export default function AltaConsultorioPage() {
  const [datos, setDatos] = useState(FORMULARIO_VACIO)
  const [error, setError] = useState(null)
  const [registrado, setRegistrado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (e) => {
    setDatos({ ...datos, [e.target.name]: e.target.value })
    setRegistrado(false)
  }

  const limpiar = () => {
    setDatos(FORMULARIO_VACIO)
    setError(null)
    setRegistrado(false)
  }

  const guardar = async (e) => {
    e.preventDefault()
    if (enviando) return
    setError(null)
    setRegistrado(false)
    if ([datos.codigo, datos.nombre, datos.ubicacion].some((valor) => !valor.trim())) {
      setError(new ApiError(400, 'Complete los campos obligatorios.'))
      return
    }
    setEnviando(true)
    try {
      // Contrato propuesto según CU-37; el DTO del backend aún está pendiente.
      await api.post('/consultorios', {
        codigo: datos.codigo.trim(),
        nombre: datos.nombre.trim(),
        ubicacion: datos.ubicacion.trim(),
        descripcion: datos.descripcion.trim(),
        estado: datos.estado,
      })
      setDatos(FORMULARIO_VACIO)
      setRegistrado(true)
    } catch (err) {
      // Conserva los datos ante campos inválidos o código duplicado (4.A y 4.B).
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible registrar el consultorio. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Alta de consultorio</h1>

      <div className={estilos.mensajes}>
        {registrado && <p className="mensaje-exito" role="status">Consultorio registrado correctamente.</p>}
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
        <fieldset className={estilos.campos} disabled={enviando}>
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
            Ubicación *
            <input name="ubicacion" value={datos.ubicacion} onChange={cambiar} placeholder="Piso 2, ala norte" maxLength={200} required />
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
          <div className={estilos.acciones}>
            <button type="submit" className="principal">{enviando ? 'Guardando…' : 'Guardar'}</button>
            <button type="button" onClick={limpiar}>Cancelar</button>
          </div>
        </fieldset>
      </form>
    </section>
  )
}
