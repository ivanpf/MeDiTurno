import { useState } from 'react'
import { Link } from 'react-router-dom'
import { GoogleAuthProvider, signInWithPopup, signOut } from 'firebase/auth'
import PantallaAcceso from '../../components/PantallaAcceso'
import { api, ApiError } from '../../services/api'
import { auth } from '../../services/firebase'
import estilos from './RegistroPage.module.css'

const FORMULARIO_VACIO = { cedula: '', correo: '', telefono: '', domicilio: '', contrasena: '', repetirContrasena: '' }

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// CU-7 Registro de cuenta de paciente: con correo y contraseña, o con una cuenta de Google.
// La persona tiene que estar en el padrón (lo carga el administrador, CU-31) y escribir la cédula y el correo que
// figuran ahí. Con correo y contraseña, la cuenta la crea el backend; con Google, la crea la ventana de Google.
export default function RegistroPage() {
  const [datos, setDatos] = useState(FORMULARIO_VACIO)
  const [error, setError] = useState(null)
  const [creada, setCreada] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value })

  const comunes = { cedula: datos.cedula, correo: datos.correo, telefono: datos.telefono, domicilio: datos.domicilio }

  const mostrarError = (err) =>
    setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible crear la cuenta. Intente nuevamente.'))

  const registrarConCorreo = async (e) => {
    e.preventDefault()
    if (datos.contrasena !== datos.repetirContrasena) {
      setError(new ApiError(0, 'Las contraseñas no coinciden.'))
      return
    }
    setError(null)
    setEnviando(true)
    try {
      await api.post('/pacientes/registro', { ...comunes, proveedor: 'PASSWORD', contrasena: datos.contrasena })
      setCreada(true)
    } catch (err) {
      mostrarError(err)
    } finally {
      setEnviando(false)
    }
  }

  const registrarConGoogle = async () => {
    if (!datos.cedula.trim() || !datos.correo.trim()) {
      setError(new ApiError(0, 'Ingrese su cédula de identidad y su correo electrónico.'))
      return
    }
    setError(null)
    setEnviando(true)
    try {
      // Primero se comprueba el padrón: no se abre la ventana de Google a quien no puede registrarse.
      await api.post('/pacientes/registro/verificacion', { cedula: datos.cedula, correo: datos.correo })
      let tokenAcceso
      try {
        const resultado = await signInWithPopup(auth, new GoogleAuthProvider())
        tokenAcceso = await resultado.user.getIdToken()
      } catch {
        throw new ApiError(0, 'No fue posible iniciar sesión con Google. Intente nuevamente.')
      }
      await api.post('/pacientes/registro', { ...comunes, proveedor: 'GOOGLE', tokenAcceso })
      setCreada(true)
    } catch (err) {
      mostrarError(err)
    } finally {
      await signOut(auth) // con Google queda una sesión abierta en Firebase; la persona entra desde el inicio de sesión
      setEnviando(false)
    }
  }

  if (creada) {
    return (
      <PantallaAcceso>
        <div className="tarjeta">
          <h1>Crear cuenta</h1>
          <p className="mensaje-exito" role="status">Cuenta creada correctamente. Ya puede iniciar sesión.</p>
          <p><Link to="/login">Ir al inicio de sesión</Link></p>
        </div>
      </PantallaAcceso>
    )
  }

  return (
    <PantallaAcceso>
      <form className="tarjeta" onSubmit={registrarConCorreo}>
        <h1>Crear cuenta</h1>

        {error && (
          <div className={`mensaje-error ${estilos.mensaje}`} role="alert">
            {error.mensaje}
            {error.detalles.length > 0 && (
              <ul>
                {error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}
              </ul>
            )}
          </div>
        )}

        <label>
          Cédula de identidad *
          <input name="cedula" value={datos.cedula} onChange={cambiar} placeholder="1.234.567-8" maxLength={20} required />
        </label>
        <label>
          Correo electrónico registrado en el padrón *
          <input type="email" name="correo" value={datos.correo} onChange={cambiar} placeholder="correo@ejemplo.com" maxLength={255} required />
        </label>
        <label>
          Teléfono
          <input type="tel" name="telefono" value={datos.telefono} onChange={cambiar} maxLength={30} />
        </label>
        <label>
          Domicilio
          <input name="domicilio" value={datos.domicilio} onChange={cambiar} maxLength={255} />
        </label>
        <label>
          Contraseña * (mínimo 6 caracteres)
          <input type="password" name="contrasena" value={datos.contrasena} onChange={cambiar} minLength={6} maxLength={128} autoComplete="new-password" required />
        </label>
        <label>
          Repita la contraseña *
          <input type="password" name="repetirContrasena" value={datos.repetirContrasena} onChange={cambiar} minLength={6} maxLength={128} autoComplete="new-password" required />
        </label>

        <button type="submit" className="principal" disabled={enviando}>Crear cuenta</button>
        <button type="button" onClick={registrarConGoogle} disabled={enviando}>Registrarme con Google</button>
        <p>Con Google no hace falta la contraseña: use la cuenta de Google que tenga ese mismo correo electrónico.</p>
        <Link className="enlace-volver" to="/login">Volver al inicio de sesión</Link>
      </form>
    </PantallaAcceso>
  )
}
