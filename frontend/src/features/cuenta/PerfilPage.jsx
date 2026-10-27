import { useState } from 'react'
import { auth } from '../../services/firebase'
import { useAuth } from '../../auth/useAuth'
import { api, ApiError } from '../../services/api'
import estilos from './PerfilPage.module.css'

const ROL_VISIBLE = { paciente: 'Paciente', funcionario: 'Funcionario', medico: 'Médico', admin: 'Administrador' }
const TELEFONO_MAXIMO = 30 // mismo límite que el registro de pacientes (CU-7) y la mobile
const DOMICILIO_MAXIMO = 255

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// CU-5 Consulta y edición del perfil.
// TODO (CU-5): el PerfilDTO del backend no definió aún sus campos («datos editables del perfil»).
// Se asume { telefono, domicilio }, los mismos del registro de pacientes (CU-7); si el backend
// agrega o cambia campos, ajustar el formulario y lo que se manda en el PUT.
// TODO (CU-5): no hay endpoint GET de perfil: la «consulta» se arma con nombre y rol de la sesión
// y el correo de Firebase; el teléfono y domicilio actuales no se pueden precargar hasta que exista.
export default function PerfilPage() {
  const { rol, nombre } = useAuth()
  const [datos, setDatos] = useState({ telefono: '', domicilio: '' })
  const [error, setError] = useState(null)
  const [actualizado, setActualizado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (e) => {
    setDatos({ ...datos, [e.target.name]: e.target.value })
    setActualizado(false)
  }

  const limpiar = () => {
    setDatos({ telefono: '', domicilio: '' })
    setError(null)
    setActualizado(false)
  }

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    setActualizado(false)
    setEnviando(true)
    try {
      const telefono = datos.telefono.trim()
      const domicilio = datos.domicilio.trim()
      // Mismo criterio que el registro: vacío significa «sin dato», no cadena vacía.
      if (telefono.length > TELEFONO_MAXIMO) throw new ApiError(0, `El teléfono no puede superar ${TELEFONO_MAXIMO} caracteres.`)
      if (domicilio.length > DOMICILIO_MAXIMO) throw new ApiError(0, `El domicilio no puede superar ${DOMICILIO_MAXIMO} caracteres.`)
      // Cada tipo de usuario tiene su endpoint; el rol de la sesión define cuál usar.
      const ruta = rol === 'paciente' ? '/pacientes/me/perfil' : '/usuarios-internos/me/perfil'
      await api.put(ruta, { telefono: telefono || null, domicilio: domicilio || null })
      setActualizado(true)
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible actualizar el perfil. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Mi perfil</h1>

      <div className={estilos.mensajes}>
        {actualizado && <p className="mensaje-exito" role="status">Perfil actualizado correctamente.</p>}
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

      {/* Consulta: estos datos no son editables (la identidad y el correo los maneja Firebase). */}
      <dl className={estilos.datos}>
        <div>
          <dt>Nombre</dt>
          <dd>{nombre}</dd>
        </div>
        <div>
          <dt>Correo electrónico</dt>
          <dd>{auth.currentUser?.email}</dd>
        </div>
        <div>
          <dt>Perfil</dt>
          <dd>{ROL_VISIBLE[rol]}</dd>
        </div>
      </dl>

      <form className={estilos.formulario} onSubmit={guardar}>
        <p className={estilos.aclaracion}>Datos de contacto (opcionales)</p>
        <label>
          Teléfono
          <input name="telefono" type="tel" value={datos.telefono} onChange={cambiar} maxLength={TELEFONO_MAXIMO} />
        </label>
        <label>
          Domicilio
          <input name="domicilio" value={datos.domicilio} onChange={cambiar} maxLength={DOMICILIO_MAXIMO} />
        </label>
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={enviando}>Guardar cambios</button>
          <button type="button" onClick={limpiar} disabled={enviando}>Descartar</button>
        </div>
      </form>
    </section>
  )
}
