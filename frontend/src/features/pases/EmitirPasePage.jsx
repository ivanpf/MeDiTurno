import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import Confirmacion from '../../components/Confirmacion'
import { api, ApiError } from '../../services/api'
import estilos from './EmitirPasePage.module.css'

const comoError = (error) => error instanceof ApiError ? error : new ApiError(0, 'No fue posible completar la operación. Intente nuevamente.')
const fecha = (valor) => valor ? new Intl.DateTimeFormat('es-UY', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(valor)) : '—'
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

export default function EmitirPasePage() {
  const { state } = useLocation()
  const consulta = state?.consulta
  const [especialidades, setEspecialidades] = useState([])
  const [destino, setDestino] = useState('')
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState(null)
  const [error, setError] = useState(null)
  const [intento, setIntento] = useState(0)
  const [confirmando, setConfirmando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [pase, setPase] = useState(null)
  const envioEnCurso = useRef(false)

  useEffect(() => {
    let vigente = true
    if (!consulta?.idConsulta) return () => { vigente = false }
    api.get('/especialidades')
      .then((lista) => {
        if (vigente) setEspecialidades(lista.filter((item) => item.estado === 'ACTIVO')
          .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')))
      })
      .catch((err) => { if (vigente) setErrorCarga(comoError(err)) })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [consulta?.idConsulta, intento])

  if (!consulta?.idConsulta) {
    return <section className="panel">
      <h1>Emitir pase</h1>
      <p className="pendiente">Registre primero la consulta desde «Mis pacientes». Al finalizar, seleccione «Emitir pase a otra especialidad».</p>
      <Link className="enlace-volver" to="/medico/pacientes">Ir a Mis pacientes</Link>
    </section>
  }

  const especialidad = especialidades.find((item) => item.codigo === destino)
  const nombreEspecialidad = (codigo) => especialidades.find((item) => item.codigo === codigo)?.nombre ?? codigo
  const reintentar = () => {
    setCargando(true)
    setErrorCarga(null)
    setIntento((actual) => actual + 1)
  }
  const pedirConfirmacion = (evento) => {
    evento.preventDefault()
    setError(null)
    if (!especialidad) {
      setError(new ApiError(400, 'Seleccione una especialidad de destino disponible.'))
      return
    }
    setConfirmando(true)
  }
  const emitir = async () => {
    if (envioEnCurso.current) return
    envioEnCurso.current = true
    setEnviando(true)
    setError(null)
    try {
      setPase(await api.post(`/consultas/${consulta.idConsulta}/pases`, { especialidadDestino: destino }))
    } catch (err) {
      setError(comoError(err))
    } finally {
      setConfirmando(false)
      setEnviando(false)
      envioEnCurso.current = false
    }
  }

  return <section className={`panel ${estilos.pagina}`}>
    <h1>Emitir pase a otra especialidad</h1>
    <h2>Consulta de origen</h2>
    <dl className={estilos.resumen}>
      <div><dt>Paciente</dt><dd>{consulta.nombrePaciente}</dd></div>
      <div><dt>Especialidad</dt><dd>{consulta.especialidad}</dd></div>
      <div><dt>Médico</dt><dd>{consulta.nombreMedico}</dd></div>
      <div><dt>Fecha de atención</dt><dd>{fecha(consulta.fecha)}</dd></div>
    </dl>
    {pase ? <>
      <p className="mensaje-exito" role="status">Pase emitido correctamente. No genera una reserva por sí mismo.</p>
      <dl className={estilos.resumen}>
        <div><dt>Número de pase</dt><dd>{pase.idPase}</dd></div>
        <div><dt>Especialidad de destino</dt><dd>{nombreEspecialidad(pase.especialidadDestino)}</dd></div>
        <div><dt>Fecha de emisión</dt><dd>{fecha(pase.fechaEmision)}</dd></div>
        <div><dt>Válido hasta</dt><dd>{fecha(pase.fechaVigenciaCalculada)}</dd></div>
        <div><dt>Estado</dt><dd>{pase.paseEstado === 'VIGENTE' ? 'Vigente' : pase.paseEstado}</dd></div>
      </dl>
      <Link className="enlace-volver" to="/medico/pacientes">Volver a Mis pacientes</Link>
    </> : <>
      <p className="pendiente">El pase tendrá una vigencia de 60 días desde su emisión. No genera una reserva por sí mismo.</p>
      {cargando && <p role="status">Cargando especialidades…</p>}
      {errorCarga && <div className={estilos.aviso}><p className="mensaje-error" role="alert">{errorCarga.mensaje}</p><button type="button" onClick={reintentar}>Reintentar</button></div>}
      {!cargando && !errorCarga && !especialidades.length && <p className={estilos.aviso} role="status">No hay especialidades activas disponibles para emitir un pase.</p>}
      {error && <div className="mensaje-error" role="alert">{error.mensaje}{error.detalles?.length > 0 && <ul>{error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}</ul>}</div>}
      <form className={estilos.formulario} onSubmit={pedirConfirmacion}>
        <label>Especialidad de destino *
          <select value={destino} onChange={(evento) => { setDestino(evento.target.value); setError(null) }} disabled={cargando || !!errorCarga || !especialidades.length || enviando} required>
            <option value="">Seleccione una especialidad</option>
            {especialidades.map((item) => <option key={item.codigo} value={item.codigo}>{item.nombre}</option>)}
          </select>
        </label>
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={cargando || !!errorCarga || !especialidad || enviando}>Emitir pase</button>
          <Link to="/medico/pacientes">Cancelar</Link>
        </div>
      </form>
    </>}
    {confirmando && <Confirmacion titulo="Confirmar emisión del pase" textoConfirmar={enviando ? 'Emitiendo…' : 'Emitir pase'} ocupado={enviando} onConfirmar={emitir} onCancelar={() => setConfirmando(false)}>
      <p>Se emitirá un pase para {consulta.nombrePaciente} a {especialidad?.nombre}, con una vigencia de 60 días desde su emisión.</p>
      <p>Esta acción no reserva una consulta.</p>
    </Confirmacion>}
  </section>
}
