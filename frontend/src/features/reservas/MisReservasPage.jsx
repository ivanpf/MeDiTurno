import { useEffect, useState } from 'react'
import Confirmacion from '../../components/Confirmacion'
import { api, ApiError } from '../../services/api'
import estilos from './MisReservasPage.module.css'

const fecha = (valor) => valor.split('-').reverse().join('/')
const hora = (valor) => valor.slice(0, 5)
const ESTADOS = { VIGENTE: 'Vigente', CANCELADA: 'Cancelada', ANULADA: 'Anulada', ORDEN_EMITIDA: 'Orden emitida', ATENDIDA: 'Atendida', VENCIDA: 'Vencida' }
const comoError = (err) => err instanceof ApiError ? err : new ApiError(0, 'No fue posible consultar o cancelar la reserva. Intente nuevamente.')

// CU-9. El backend decide el plazo y vuelve a validarlo antes de confirmar la cancelación.
export default function MisReservasPage() {
  const [reservas, setReservas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [intento, setIntento] = useState(0)
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [preparando, setPreparando] = useState(false)
  const [confirmacion, setConfirmacion] = useState(null)
  const [errorConfirmacion, setErrorConfirmacion] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let vigente = true
    api.get('/reservas/mias/proximas')
      .then((lista) => { if (vigente) setReservas(lista) })
      .catch((err) => { if (vigente) setError(comoError(err)) })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [intento])

  const actualizar = () => {
    setError(null)
    setCargando(true)
    setIntento((valor) => valor + 1)
  }
  const recargar = async () => {
    setCargando(true)
    try { setReservas(await api.get('/reservas/mias/proximas')) }
    catch (err) { setError(comoError(err)) }
    finally { setCargando(false) }
  }
  const pedirCancelacion = async (id) => {
    setPreparando(true)
    setError(null)
    setAviso(null)
    setErrorConfirmacion(null)
    try { setConfirmacion(await api.get(`/reservas/${id}/cancelacion/vista-previa`)) }
    catch (err) {
      setError(comoError(err))
      if (err.estado === 409 || err.estado === 404) await recargar()
    } finally { setPreparando(false) }
  }
  const cancelar = async () => {
    if (enviando) return
    setEnviando(true)
    setErrorConfirmacion(null)
    try {
      await api.post(`/reservas/${confirmacion.reserva.idReserva}/cancelacion`)
      setConfirmacion(null)
      setAviso('Reserva cancelada correctamente. El número quedó disponible para una nueva reserva.')
      await recargar()
    } catch (err) {
      const fallo = comoError(err)
      if (fallo.estado === 409 || fallo.estado === 404) {
        setConfirmacion(null)
        setError(fallo)
        await recargar()
      } else { setErrorConfirmacion(fallo) }
    } finally { setEnviando(false) }
  }
  const ocupado = cargando || preparando || enviando

  return (
    <section className={`panel ${estilos.pagina}`}>
      <h1>Mis reservas</h1>
      <p>Consulte sus reservas de hoy y de fechas próximas. Puede cancelar una reserva vigente hasta 48 horas antes del <strong>inicio del turno</strong>, inclusive.</p>
      <button type="button" onClick={actualizar} disabled={ocupado}>Actualizar reservas</button>
      {aviso && <p className="mensaje-exito" role="status">{aviso}</p>}
      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
      {cargando && <p role="status">Cargando reservas…</p>}
      {preparando && <p role="status">Verificando si la reserva puede cancelarse…</p>}
      {!cargando && !error && !reservas.length && <p className="pendiente" role="status">No tiene reservas para hoy ni para fechas próximas.</p>}
      {!cargando && reservas.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <caption className={estilos.leyenda}>Próximas reservas y cancelaciones registradas</caption>
            <thead><tr><th scope="col">Reserva / número</th><th scope="col">Fecha y turno</th><th scope="col">Especialidad y médico</th><th scope="col">Consultorio</th><th scope="col">Estado</th><th scope="col">Cancelación</th></tr></thead>
            <tbody>{reservas.map((item) => {
              const r = item.reserva
              return (
                <tr key={r.idReserva}>
                  <td>#{r.idReserva}<br />Número {r.numero}</td>
                  <td>{fecha(r.fecha)}<br />{hora(r.horaInicio)}–{hora(r.horaFin)}<small className={estilos.detalle}>Su hora estimada: ~{hora(r.horaEstimada)}</small></td>
                  <td>{r.especialidad}<small className={estilos.detalle}>{r.nombreMedico}</small></td>
                  <td>{r.consultorio}</td>
                  <td><span className={r.estado === 'CANCELADA' ? estilos.cancelada : estilos.estado}>{ESTADOS[r.estado] ?? r.estado}</span>
                    {item.fechaCancelacion && <small className={estilos.detalle}>Cancelada el {fecha(item.fechaCancelacion.slice(0, 10))} a las {item.fechaCancelacion.slice(11, 16)}</small>}
                  </td>
                  <td>{item.puedeCancelar
                    ? <button type="button" disabled={ocupado} onClick={() => pedirCancelacion(r.idReserva)}>Cancelar reserva</button>
                    : <small>{item.motivoNoCancelable}</small>}
                  </td>
                </tr>
              )
            })}</tbody>
          </table>
        </div>
      )}
      {confirmacion && (
        <Confirmacion titulo="Confirmar cancelación" textoConfirmar="Confirmar cancelación" ocupado={enviando} onConfirmar={cancelar} onCancelar={() => { setConfirmacion(null); setErrorConfirmacion(null) }}>
          <p>Va a cancelar la reserva <strong>#{confirmacion.reserva.idReserva}</strong>, número <strong>{confirmacion.reserva.numero}</strong>.</p>
          <dl className={estilos.datos}>
            <dt>Fecha</dt><dd>{fecha(confirmacion.reserva.fecha)}</dd>
            <dt>Turno</dt><dd>{hora(confirmacion.reserva.horaInicio)}–{hora(confirmacion.reserva.horaFin)}</dd>
            <dt>Especialidad</dt><dd>{confirmacion.reserva.especialidad}</dd>
            <dt>Médico</dt><dd>{confirmacion.reserva.nombreMedico}</dd>
            <dt>Consultorio</dt><dd>{confirmacion.reserva.consultorio}</dd>
          </dl>
          <p>Al confirmar, su número quedará libre. La cancelación se conservará registrada.</p>
          {errorConfirmacion && <p className="mensaje-error" role="alert">{errorConfirmacion.mensaje}</p>}
        </Confirmacion>
      )}
    </section>
  )
}
