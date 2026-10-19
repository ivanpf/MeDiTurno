import { useState } from 'react'
import Confirmacion from '../../components/Confirmacion'
import { api, ApiError } from '../../services/api'
import estilos from './ConsultaReservaPage.module.css'

const ESTADOS = {
  VIGENTE: 'Vigente',
  ORDEN_EMITIDA: 'Orden emitida',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
  ANULADA: 'Anulada',
  VENCIDA: 'Vencida',
}

// Lo que se informa cuando la reserva se muestra pero no se puede emitir la orden (CU-15, flujo 4.A)
const MOTIVO_SIN_EMISION = {
  ORDEN_EMITIDA: 'Ya tiene una orden emitida.',
  ATENDIDA: 'Ya fue atendida.',
  CANCELADA: 'La reserva está cancelada. No se puede emitir una orden.',
  ANULADA: 'La reserva fue anulada porque se suspendió el turno. No se puede emitir una orden.',
  VENCIDA: 'La reserva está vencida. No se puede emitir una orden.',
}

const fecha = (iso) => iso.split('-').reverse().join('/')
const hora = (texto) => texto.slice(0, 5)

const comoError = (err, mensaje) => (err instanceof ApiError ? err : new ApiError(0, mensaje))

export default function ConsultaReservaPage() {
  const [identificador, setIdentificador] = useState('')
  const [reserva, setReserva] = useState(null)
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [abriendo, setAbriendo] = useState(false)
  const [mandando, setMandando] = useState(false)

  const cargar = async (id) => {
    setBuscando(true)
    try {
      setReserva(await api.get(`/reservas/${id}`))
    } catch (err) {
      setReserva(null)
      // 3.A: un identificador que no existe no muestra datos ni permite emitir
      setError(comoError(err, 'No se pudo consultar la reserva. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  const buscar = (e) => {
    e.preventDefault()
    setAviso('')
    setError(null)
    const texto = identificador.trim()
    // 2.A: vacío o con un formato que no corresponde (un número entero positivo)
    if (!/^\d+$/.test(texto) || Number(texto) < 1) {
      setReserva(null)
      setError(new ApiError(400, 'El identificador ingresado no es válido. Ingrese el número de la reserva.'))
      return
    }
    cargar(texto)
  }

  const emitir = async () => {
    setEnviando(true)
    setError(null)
    try {
      const orden = await api.post(`/reservas/${reserva.idReserva}/orden`)
      setConfirmando(false)
      setAviso(`Orden ${orden.numeroOrden} emitida correctamente. Ya puede imprimirla o generar el PDF.`)
      await cargar(reserva.idReserva)
    } catch (err) {
      setConfirmando(false)
      setError(comoError(err, 'No se pudo emitir la orden. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  // Se abre la ventana antes de pedir el PDF: si se abriera después, el navegador podría bloquearla.
  const abrirPdf = async () => {
    const ventana = window.open('', '_blank')
    setAbriendo(true)
    setError(null)
    try {
      const archivo = await api.descargar(`/ordenes/${reserva.numeroOrden}/pdf`)
      const direccion = URL.createObjectURL(archivo)
      if (ventana) ventana.location = direccion
      else window.location.assign(direccion)
    } catch (err) {
      ventana?.close()
      setError(comoError(err, 'No se pudo generar el PDF. Intente nuevamente.'))
    } finally {
      setAbriendo(false)
    }
  }

  // Paso 6 (opcional): el PDF también se puede mandar por correo al paciente.
  const enviarPorCorreo = async () => {
    setMandando(true)
    setError(null)
    setAviso('')
    try {
      const envio = await api.post(`/ordenes/${reserva.numeroOrden}/correo`)
      setAviso(`La orden se envió por correo a ${envio.destino}.`)
    } catch (err) {
      setError(comoError(err, 'No se pudo enviar el correo. Intente nuevamente.'))
    } finally {
      setMandando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Consulta de reserva y orden</h1>

      {aviso && <p className="mensaje-exito" role="status">{aviso}</p>}
      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}

      <form className={estilos.busqueda} onSubmit={buscar}>
        <label>
          Identificador de la reserva
          <input
            inputMode="numeric"
            value={identificador}
            onChange={(e) => setIdentificador(e.target.value)}
            placeholder="Por ejemplo, 123"
            autoFocus
          />
        </label>
        <button type="submit" disabled={buscando}>{buscando ? 'Buscando…' : 'Buscar'}</button>
      </form>

      {reserva && (
        <>
          <h2>Datos de la reserva</h2>
          <div className="tabla-scroll">
            <table className="tabla">
              <tbody>
                <tr><th scope="row">Reserva</th><td>{reserva.idReserva}</td></tr>
                <tr><th scope="row">Paciente</th><td>{reserva.nombrePaciente} (CI {reserva.cedulaPaciente})</td></tr>
                <tr><th scope="row">Médico</th><td>{reserva.nombreMedico}</td></tr>
                <tr><th scope="row">Especialidad</th><td>{reserva.especialidad}</td></tr>
                <tr>
                  <th scope="row">Fecha y horario</th>
                  <td>{fecha(reserva.fecha)} · {hora(reserva.horaInicio)} a {hora(reserva.horaFin)}</td>
                </tr>
                <tr><th scope="row">Consultorio</th><td>{reserva.consultorio}</td></tr>
                <tr><th scope="row">Número</th><td>{reserva.numero} (hora estimada ~{hora(reserva.horaEstimada)})</td></tr>
                <tr><th scope="row">Estado</th><td>{ESTADOS[reserva.estado]}</td></tr>
              </tbody>
            </table>
          </div>

          {reserva.estado !== 'VIGENTE' && <p className="pendiente">{MOTIVO_SIN_EMISION[reserva.estado]}</p>}

          <div className={estilos.acciones}>
            {reserva.estado === 'VIGENTE' && (
              <button className="principal" onClick={() => setConfirmando(true)}>Emitir orden</button>
            )}
            {reserva.numeroOrden && (
              <>
                <button onClick={abrirPdf} disabled={abriendo}>
                  {abriendo ? 'Generando…' : `Imprimir o generar PDF (orden ${reserva.numeroOrden})`}
                </button>
                <button onClick={enviarPorCorreo} disabled={mandando}>
                  {mandando ? 'Enviando…' : 'Enviar por correo al paciente'}
                </button>
              </>
            )}
          </div>
        </>
      )}

      {confirmando && (
        <Confirmacion
          titulo="Emitir orden"
          textoConfirmar="Emitir orden"
          ocupado={enviando}
          onConfirmar={emitir}
          onCancelar={() => setConfirmando(false)}
        >
          <p>
            Se emitirá la orden del número {reserva.numero} de {reserva.nombrePaciente} ({fecha(reserva.fecha)}, hora estimada{' '}
            ~{hora(reserva.horaEstimada)}). Solo se puede emitir hasta 15 minutos antes de la hora estimada.
          </p>
        </Confirmacion>
      )}
    </section>
  )
}
