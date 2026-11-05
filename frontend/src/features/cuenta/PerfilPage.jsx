/* oxlint-disable react/set-state-in-effect -- sincroniza carga, errores y selección con la consulta al servidor */
import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { api } from '../../services/api'
import { comoError, soloMensaje } from '../../utils/errores'
import { fecha } from '../../utils/formatos'
import estilos from './PerfilPage.module.css'

const ROLES = { paciente: 'Paciente', funcionario: 'Funcionario', medico: 'Médico', admin: 'Administrador' }
const contacto = (p) => ({ correo: p.correo, telefono: p.telefono ?? '', domicilio: p.domicilio ?? '' })

export default function PerfilPage() {
  const { rol } = useAuth()
  const ruta = rol === 'paciente' ? '/pacientes/me/perfil' : '/usuarios-internos/me/perfil'
  const [perfil, setPerfil] = useState(null)
  const [datos, setDatos] = useState({ correo: '', telefono: '', domicilio: '' })
  const [error, setError] = useState(null)
  const [actualizado, setActualizado] = useState(false)
  const [ocupado, setOcupado] = useState(true)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setOcupado(true)
    setError(null)
    api.get(ruta).then((p) => { if (vigente) { setPerfil(p); setDatos(contacto(p)) } })
      .catch((err) => { if (vigente) setError(comoError(err, 'No fue posible consultar su perfil.')) })
      .finally(() => { if (vigente) setOcupado(false) })
    return () => { vigente = false }
  }, [ruta, intento])

  const cambiar = (e) => { setDatos({ ...datos, [e.target.name]: e.target.value }); setActualizado(false) }
  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    setActualizado(false)
    setOcupado(true)
    try {
      const nuevos = { correo: datos.correo.trim(), telefono: datos.telefono.trim() || null, domicilio: rol === 'paciente' ? datos.domicilio.trim() || null : null }
      await api.put(ruta, nuevos)
      const guardado = { ...perfil, ...nuevos }
      setPerfil(guardado)
      setDatos(contacto(guardado))
      setActualizado(true)
    } catch (err) { setError(comoError(err, 'No fue posible actualizar su perfil. Intente nuevamente.')) }
    finally { setOcupado(false) }
  }

  return (
    <section className="panel">
      <h1>Mi perfil</h1>
      {actualizado && <p className="mensaje-exito" role="status">Perfil actualizado correctamente.</p>}
      {error && <div className="mensaje-error" role="alert">{error.mensaje}{error.detalles?.length > 0 && <ul>{error.detalles.map((d) => <li key={d}>{soloMensaje(d)}</li>)}</ul>}</div>}
      {ocupado && <p role="status">{perfil ? 'Guardando…' : 'Cargando perfil…'}</p>}
      {!perfil && !ocupado && <button onClick={() => setIntento(intento + 1)}>Reintentar</button>}
      {perfil && <>
        <dl className={estilos.datos}>
          <div><dt>Nombre</dt><dd>{perfil.nombre}</dd></div>
          <div><dt>Cédula</dt><dd>{perfil.cedula}</dd></div>
          <div><dt>Perfil</dt><dd>{ROLES[perfil.rol]}</dd></div>
          <div><dt>Fecha de nacimiento</dt><dd>{fecha(perfil.fechaNacimiento)}</dd></div>
        </dl>
        <form className={estilos.formulario} onSubmit={guardar}>
          <label>Correo electrónico<input name="correo" type="email" required maxLength={255} value={datos.correo} onChange={cambiar} disabled={ocupado} /></label>
          <p>Si cambia su correo, utilice el nuevo para su próximo inicio de sesión.</p>
          <label>Teléfono (opcional)<input name="telefono" type="tel" maxLength={30} value={datos.telefono} onChange={cambiar} disabled={ocupado} /></label>
          {rol === 'paciente' && <label>Domicilio (opcional)<input name="domicilio" maxLength={255} value={datos.domicilio} onChange={cambiar} disabled={ocupado} /></label>}
          <div className={estilos.acciones}>
            <button className="principal" disabled={ocupado}>Guardar cambios</button>
            <button type="button" disabled={ocupado} onClick={() => { setDatos(contacto(perfil)); setError(null); setActualizado(false) }}>Descartar</button>
          </div>
        </form>
      </>}
    </section>
  )
}
