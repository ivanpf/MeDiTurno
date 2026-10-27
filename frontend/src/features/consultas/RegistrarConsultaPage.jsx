import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import Confirmacion from '../../components/Confirmacion'
import { api, ApiError } from '../../services/api'
import estilos from './RegistrarConsultaPage.module.css'

const FORMULARIO_VACIO = { motivo: '', observaciones: '', indicaciones: '' }
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle
const hora = (valor) => valor.slice(0, 5)

// CU-23: el médico llega desde «Mis pacientes» (CU-21) con el paciente elegido; el backend valida que la orden sea de su turno.
export default function RegistrarConsultaPage() {
  const { state: paciente } = useLocation()
  const [datos, setDatos] = useState(FORMULARIO_VACIO)
  const [error, setError] = useState(null)
  const [confirmando, setConfirmando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [registrada, setRegistrada] = useState(null)

  if (!paciente?.numeroOrden) {
    return (
      <section className="panel">
        <h1>Registrar consulta</h1>
        <p className="pendiente">Elija primero al paciente atendido desde «Mis pacientes». Solo se puede registrar la consulta de un paciente con orden emitida.</p>
        <Link className="enlace-volver" to="/medico/pacientes">Ir a Mis pacientes</Link>
      </section>
    )
  }

  const cambiar = (e) => setDatos((actuales) => ({ ...actuales, [e.target.name]: e.target.value }))

  // 7.A: se avisa lo que falta antes de pedir la confirmación; el backend vuelve a validar.
  const pedirConfirmacion = (e) => {
    e.preventDefault()
    setError(null)
    if (!datos.motivo.trim() || !datos.observaciones.trim()) {
      setError(new ApiError(400, 'Complete los campos obligatorios.', []))
      return
    }
    setConfirmando(true)
  }

  const registrar = async () => {
    setEnviando(true)
    try {
      const consulta = await api.post(`/consultas/${paciente.numeroOrden}`, {
        motivo: datos.motivo.trim(),
        observaciones: datos.observaciones.trim(),
        indicaciones: datos.indicaciones.trim() || null,
      })
      setRegistrada(consulta)
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible registrar la consulta. Intente nuevamente.', []))
    } finally {
      setConfirmando(false)
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Registrar consulta</h1>

      <dl className={estilos.paciente}>
        <div><dt>Paciente</dt><dd>{paciente.nombrePaciente}</dd></div>
        <div><dt>Número</dt><dd>{paciente.numero} (hora estimada ~{hora(paciente.horaEstimada)})</dd></div>
        <div><dt>Orden</dt><dd>{paciente.numeroOrden}</dd></div>
      </dl>

      {!registrada && <Link to={`/medico/historia-clinica?${new URLSearchParams({ idPaciente: paciente.idPaciente })}`} state={paciente}>Consultar historia clínica antes de registrar la atención</Link>}

      {registrada && (
        <>
          <p className="mensaje-exito" role="status">Consulta registrada correctamente. Se avisó al paciente que se incorporó nueva información a su historia clínica.</p>
          <div className={estilos.acciones}>
            <Link to="/medico/pase" state={{ consulta: registrada }}>Emitir pase a otra especialidad</Link>
          </div>
          <Link className="enlace-volver" to="/medico/pacientes">Volver a Mis pacientes</Link>
        </>
      )}

      {error && (
        <div className="mensaje-error" role="alert">
          {error.mensaje}
          {error.detalles?.length > 0 && (
            <ul>
              {error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}
            </ul>
          )}
        </div>
      )}

      {!registrada && (
        <form className={estilos.formulario} onSubmit={pedirConfirmacion}>
          <label>Motivo de la consulta *
            <textarea name="motivo" value={datos.motivo} onChange={cambiar} rows={3} maxLength={5000} required />
          </label>
          <label>Observaciones clínicas *
            <textarea name="observaciones" value={datos.observaciones} onChange={cambiar} rows={6} maxLength={5000} required />
          </label>
          <label>Indicaciones
            <textarea name="indicaciones" value={datos.indicaciones} onChange={cambiar} rows={4} maxLength={5000} />
          </label>
          <div className={estilos.acciones}>
            <button type="submit" className="principal">Registrar consulta</button>
            <Link className="enlace-volver" to="/medico/pacientes">Cancelar</Link>
          </div>
        </form>
      )}

      {confirmando && (
        <Confirmacion titulo="Registrar consulta" textoConfirmar="Registrar" ocupado={enviando} onConfirmar={registrar} onCancelar={() => setConfirmando(false)}>
          <p>Se registrará la consulta de {paciente.nombrePaciente} en su historia clínica y se avisará al paciente. Una vez registrada no se puede modificar.</p>
        </Confirmacion>
      )}
    </section>
  )
}
