import { useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './AltaPacientePage.module.css'

const FORMULARIO_VACIO = { cedula: '', nombre: '', apellido: '', correo: '', fechaNacimiento: '', telefono: '', domicilio: '' }

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// CU-31 Alta individual de pacientes en el padrón.
export default function AltaPacientePage() {
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
    setError(null)
    setRegistrado(false)
    setEnviando(true)
    try {
      await api.post('/pacientes', datos)
      setDatos(FORMULARIO_VACIO)
      setRegistrado(true)
    } catch (err) {
      // Con un error del backend el formulario conserva los datos ingresados (flujos 4.A, 4.B y 4.C).
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible registrar al paciente. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Alta de paciente</h1>

      <div className={estilos.mensajes}>
        {registrado && <p className="mensaje-exito" role="status">Paciente incorporado al padrón. Ya puede registrarse como usuario.</p>}
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
            Cédula de identidad *
            <input name="cedula" value={datos.cedula} onChange={cambiar} placeholder="1.234.567-8" maxLength={20} required />
          </label>
          <label>
            Fecha de nacimiento *
            <input type="date" name="fechaNacimiento" value={datos.fechaNacimiento} onChange={cambiar} required />
          </label>
        </div>
        <div className={estilos.fila}>
          <label>
            Nombre *
            <input name="nombre" value={datos.nombre} onChange={cambiar} maxLength={100} required />
          </label>
          <label>
            Apellido *
            <input name="apellido" value={datos.apellido} onChange={cambiar} maxLength={100} required />
          </label>
        </div>
        <label>
          Correo electrónico *
          <input type="email" name="correo" value={datos.correo} onChange={cambiar} placeholder="correo@ejemplo.com" maxLength={255} required />
        </label>
        <div className={estilos.fila}>
          <label>
            Teléfono
            <input type="tel" name="telefono" value={datos.telefono} onChange={cambiar} maxLength={30} />
          </label>
          <label>
            Domicilio
            <input name="domicilio" value={datos.domicilio} onChange={cambiar} maxLength={255} />
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
