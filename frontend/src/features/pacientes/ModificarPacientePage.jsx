import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { api } from '../../services/api'
import { comoError, soloMensaje } from '../../utils/errores'
import estilos from './ModificarPacientePage.module.css'

const aFormulario = (p) => ({
  nombre: p.nombre,
  apellido: p.apellido,
  correo: p.correo,
  fechaNacimiento: p.fechaNacimiento ?? '',
  telefono: p.telefono ?? '',
  domicilio: p.domicilio ?? '',
})

// CU-44 Modificación de datos de pacientes. La cédula no se puede modificar y esta función no da acceso a la historia clínica.
export default function ModificarPacientePage() {
  // Desde el listado (CU-33) se llega con el paciente ya elegido.
  const { state } = useLocation()
  const [texto, setTexto] = useState('')
  const [resultados, setResultados] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [paciente, setPaciente] = useState(null) // los datos guardados del paciente elegido
  const [borrador, setBorrador] = useState(null) // lo escrito en el formulario; vacío = los datos guardados
  const [error, setError] = useState(null)
  const [modificado, setModificado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const cargarPaciente = async (idPaciente) => {
    setError(null)
    setModificado(false)
    setBorrador(null)
    try {
      setPaciente(await api.get(`/pacientes/${idPaciente}`))
    } catch (err) {
      setPaciente(null)
      setError(comoError(err, 'No fue posible consultar al paciente. Intente nuevamente.'))
    }
  }

  useEffect(() => {
    if (!state?.idPaciente) return
    api.get(`/pacientes/${state.idPaciente}`)
      .then(setPaciente)
      .catch((err) => setError(comoError(err, 'No fue posible consultar al paciente. Intente nuevamente.')))
  }, [state])

  const buscar = async (e) => {
    e.preventDefault()
    setBuscando(true)
    setError(null)
    try {
      setResultados(await api.get(`/pacientes?${new URLSearchParams({ texto: texto.trim() })}`))
    } catch (err) {
      setResultados(null)
      setError(comoError(err, 'No fue posible buscar pacientes. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  const elegir = (e) => {
    if (e.target.value) {
      cargarPaciente(e.target.value)
    } else {
      setPaciente(null)
      setBorrador(null)
    }
  }

  // Los datos del formulario son los guardados del paciente elegido, salvo lo que la persona haya escrito.
  const datos = borrador ?? (paciente ? aFormulario(paciente) : null)

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
      const actualizado = await api.put(`/pacientes/${paciente.idPaciente}`, datos)
      setPaciente(actualizado)
      setBorrador(null)
      setModificado(true)
    } catch (err) {
      // Con un error del backend el formulario conserva lo que se escribió.
      setError(comoError(err, 'No fue posible modificar al paciente. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  const cambiaCorreo = paciente && datos && datos.correo.trim().toLowerCase() !== paciente.correo.toLowerCase()

  return (
    <section className="panel">
      <h1>Modificación de paciente</h1>

      <div className={estilos.mensajes}>
        {modificado && <p className="mensaje-exito" role="status">Paciente modificado correctamente.</p>}
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

      <form className={estilos.formulario} onSubmit={buscar}>
        <label>
          Buscar paciente por cédula, nombre, apellido o correo
          <input type="search" value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={100} />
        </label>
        <div className={estilos.acciones}>
          <button type="submit" disabled={buscando || !texto.trim()}>Buscar</button>
        </div>
      </form>

      {resultados && resultados.length === 0 && (
        <p className="pendiente" role="status">No se encontraron pacientes con ese criterio.</p>
      )}

      {resultados && resultados.length > 0 && (
        <div className={estilos.formulario}>
          <label>
            Paciente
            <select value={paciente?.idPaciente ?? ''} onChange={elegir}>
              <option value="">Seleccione un paciente</option>
              {resultados.map((p) => (
                <option key={p.idPaciente} value={p.idPaciente}>{p.apellido}, {p.nombre} ({p.cedula})</option>
              ))}
            </select>
          </label>
        </div>
      )}

      {datos && (
        <form className={estilos.formulario} onSubmit={guardar}>
          <div className={estilos.fila}>
            <label>
              Cédula de identidad
              <input value={paciente.cedula} disabled />
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
            <input type="email" name="correo" value={datos.correo} onChange={cambiar} maxLength={255} required />
          </label>
          {cambiaCorreo && paciente.tieneCuenta && (
            <p className="pendiente">
              Este paciente ya tiene una cuenta de acceso: el cambio de correo también se aplica a esa cuenta, y deberá usar
              el nuevo correo para iniciar sesión.
            </p>
          )}
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
          <p className={estilos.ayuda}>
            La cédula no se puede modificar. El correo no puede pertenecer a otra persona. Esta función no permite consultar
            ni modificar la historia clínica.
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
