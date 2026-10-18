import { useEffect, useState } from 'react'
import { ROLES } from '../../auth/roles'
import { api, ApiError } from '../../services/api'
import estilos from './AltaUsuarioInternoPage.module.css'

const FORMULARIO_VACIO = { cedula: '', correo: '', nombre: '', apellido: '', fechaNacimiento: '', matricula: '', rol: ROLES.FUNCIONARIO, especialidades: [] }
// El enum del DTO del alta es distinto de los roles usados por la navegación.
const ROL_API = { [ROLES.FUNCIONARIO]: 'FUNCIONARIO', [ROLES.MEDICO]: 'MEDICO', [ROLES.ADMIN]: 'ADMINISTRADOR' }
const fechaHoy = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Montevideo' }).format(new Date())
const ultimaFechaNacimiento = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Montevideo' }).format(new Date(Date.now() - 86400000))
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// CU-34. Contrato AltaUsuarioInternoDTO; el backend crea la cuenta activa.
export default function AltaUsuarioInternoPage() {
  const [datos, setDatos] = useState(FORMULARIO_VACIO)
  const [error, setError] = useState(null)
  const [registrado, setRegistrado] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [especialidades, setEspecialidades] = useState([])
  const [cargando, setCargando] = useState(false)
  const [errorEspecialidades, setErrorEspecialidades] = useState(null)
  const [intento, setIntento] = useState(0)
  const esMedico = datos.rol === ROLES.MEDICO

  useEffect(() => {
    if (!esMedico) return
    let vigente = true
    api.get('/especialidades').then((lista) => {
      if (vigente) setEspecialidades(lista.filter((especialidad) => especialidad.estado === 'ACTIVO'))
    }).catch(() => {
      if (vigente) setErrorEspecialidades('No fue posible cargar las especialidades. Intente nuevamente.')
    }).finally(() => {
      if (vigente) setCargando(false)
    })
    return () => { vigente = false }
  }, [esMedico, intento])

  const cambiar = (e) => {
    const { name, value } = e.target
    if (name === 'rol' && value === ROLES.MEDICO) {
      setCargando(true)
      setErrorEspecialidades(null)
      setEspecialidades([])
    }
    setDatos((actuales) => ({ ...actuales, [name]: value, ...(name === 'rol' && { especialidades: [], matricula: '' }) }))
    setRegistrado(false)
  }

  const cambiarEspecialidad = (e) => {
    const { value, checked } = e.target
    setDatos((actuales) => ({ ...actuales, especialidades: checked
      ? [...actuales.especialidades, value]
      : actuales.especialidades.filter((codigo) => codigo !== value) }))
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
    if (esMedico && (cargando || errorEspecialidades || !especialidades.length)) {
      setError(new ApiError(0, 'Debe disponer de especialidades activas para crear un médico.'))
      return
    }
    if (esMedico && !datos.especialidades.length) {
      setError(new ApiError(400, 'Seleccione al menos una especialidad médica.'))
      return
    }
    if ([datos.cedula, datos.correo, datos.nombre, datos.apellido, datos.fechaNacimiento].some((valor) => !valor.trim())) {
      setError(new ApiError(400, 'Complete los campos obligatorios.'))
      return
    }
    if (!datos.fechaNacimiento || datos.fechaNacimiento >= fechaHoy()) {
      setError(new ApiError(400, 'Ingrese una fecha de nacimiento anterior a hoy.'))
      return
    }
    if (esMedico && !datos.matricula.trim()) {
      setError(new ApiError(400, 'La matrícula es obligatoria para los médicos.'))
      return
    }
    setEnviando(true)
    try {
      // Campos y roles según AltaUsuarioInternoDTO.
      await api.post('/usuarios-internos', {
        cedula: datos.cedula.trim(), correo: datos.correo.trim(),
        nombre: datos.nombre.trim(), apellido: datos.apellido.trim(),
        fechaNacimiento: datos.fechaNacimiento, rol: ROL_API[datos.rol],
        ...(esMedico && { matricula: datos.matricula.trim(), especialidadIds: datos.especialidades }),
      })
      setDatos(FORMULARIO_VACIO)
      setRegistrado(true)
    } catch (err) {
      // Conserva los datos ante errores de validación, duplicados y especialidades inválidas.
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible crear el usuario interno. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Alta de usuario interno</h1>
      <div className={estilos.mensajes}>
        {registrado && <p className="mensaje-exito" role="status">Usuario interno creado correctamente. La cuenta está activa.</p>}
        {error && (
          <div className="mensaje-error" role="alert">
            {error.mensaje}
            {error.detalles.length > 0 && <ul>{error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}</ul>}
          </div>
        )}
      </div>
      <form className={estilos.formulario} onSubmit={guardar}>
        <fieldset className={estilos.campos} disabled={enviando}>
          <div className={estilos.fila}>
            <label>Cédula de identidad *<input name="cedula" value={datos.cedula} onChange={cambiar} placeholder="1.234.567-8" maxLength={20} required /></label>
            <label>Correo electrónico *<input type="email" name="correo" value={datos.correo} onChange={cambiar} maxLength={255} required /></label>
          </div>
          <div className={estilos.fila}>
            <label>Nombre *<input name="nombre" value={datos.nombre} onChange={cambiar} maxLength={100} required /></label>
            <label>Apellido *<input name="apellido" value={datos.apellido} onChange={cambiar} maxLength={100} required /></label>
          </div>
          <label>Fecha de nacimiento *
            <input type="date" name="fechaNacimiento" value={datos.fechaNacimiento} onChange={cambiar} max={ultimaFechaNacimiento()} required />
          </label>
          <label>Rol *
            <select name="rol" value={datos.rol} onChange={cambiar} required>
              <option value={ROLES.FUNCIONARIO}>Funcionario</option>
              <option value={ROLES.MEDICO}>Médico</option>
              <option value={ROLES.ADMIN}>Administrador</option>
            </select>
          </label>
          {esMedico && (
            <fieldset className={estilos.especialidades}>
              <legend>Datos del médico</legend>
              <label>Matrícula médica *
                <input name="matricula" value={datos.matricula} onChange={cambiar} maxLength={30} required />
              </label>
              <p className={estilos.ayuda}>Seleccione al menos una especialidad activa.</p>
              {cargando ? <p role="status">Cargando especialidades…</p> : errorEspecialidades ? (
                <div className="mensaje-error" role="alert">
                  <p>{errorEspecialidades}</p>
                  <button type="button" onClick={() => {
                    setCargando(true)
                    setErrorEspecialidades(null)
                    setIntento((actual) => actual + 1)
                  }}>Reintentar</button>
                </div>
              ) : especialidades.length === 0 ? (
                <p className="pendiente" role="status">No hay especialidades activas disponibles.</p>
              ) : especialidades.map((especialidad) => (
                <label key={especialidad.codigo} className={estilos.opcion}>
                  <input type="checkbox" value={especialidad.codigo} checked={datos.especialidades.includes(especialidad.codigo)} onChange={cambiarEspecialidad} />
                  {especialidad.nombre}
                </label>
              ))}
            </fieldset>
          )}
          <div className={estilos.acciones}>
            <button type="submit" className="principal" disabled={esMedico && (cargando || !!errorEspecialidades || !especialidades.length)}>{enviando ? 'Creando usuario…' : 'Crear usuario'}</button>
            <button type="button" onClick={limpiar}>Cancelar</button>
          </div>
        </fieldset>
      </form>
    </section>
  )
}
