import { useEffect, useState } from 'react'
import Confirmacion from '../../components/Confirmacion'
import { api, ApiError } from '../../services/api'
import estilos from './TurnosSemanalesPage.module.css'

const DIAS = [
  ['LUNES', 'Lunes'],
  ['MARTES', 'Martes'],
  ['MIERCOLES', 'Miércoles'],
  ['JUEVES', 'Jueves'],
  ['VIERNES', 'Viernes'],
  ['SABADO', 'Sábado'],
  ['DOMINGO', 'Domingo'],
]
const NOMBRE_DIA = Object.fromEntries(DIAS)

const ORDENES = [
  ['', 'Día y horario'],
  ['medico', 'Médico'],
  ['especialidad', 'Especialidad'],
  ['horario', 'Horario'],
  ['consultorio', 'Consultorio'],
  ['estado', 'Estado'],
]

const FILTROS_VACIOS = { idMedico: '', codigoEspecialidad: '', dia: '', codigoConsultorio: '', estado: '', ordenarPor: '', descendente: false }

// Fecha de hoy en la zona horaria de quien usa la pantalla (toISOString devolvería la de UTC).
const hoy = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const suspensionVacia = () => ({ modalidad: 'TEMPORAL', fechaDesde: hoy(), fechaHasta: '', motivo: '' })

const fecha = (iso) => iso.split('-').reverse().join('/')
const hora = (texto) => texto.slice(0, 5)
const nombreMedico = (m) => `${m.apellido}, ${m.nombre}`

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// «1 reserva», «2 reservas»
const cantidad = (n, singular, plural) => `${n} ${n === 1 ? singular : plural}`

const comoError = (err, mensaje) => (err instanceof ApiError ? err : new ApiError(0, mensaje))

function Mensaje({ error }) {
  return (
    <div className="mensaje-error" role="alert">
      {error.mensaje}
      {error.detalles.length > 0 && (
        <ul>
          {error.detalles.map((detalle) => <li key={detalle}>{soloMensaje(detalle)}</li>)}
        </ul>
      )}
    </div>
  )
}

function describirSuspension(s) {
  return s.modalidad === 'TEMPORAL'
    ? `Temporal: del ${fecha(s.fechaDesde)} al ${fecha(s.fechaHasta)}`
    : `Permanente desde el ${fecha(s.fechaDesde)}`
}

// CU-20 Búsqueda y listado de turnos semanales, y CU-13 Suspensión de un turno semanal (desde cada turno activo).
export default function TurnosSemanalesPage() {
  const [opciones, setOpciones] = useState({ medicos: [], especialidades: [], consultorios: [] })
  const [filtros, setFiltros] = useState(FILTROS_VACIOS)
  const [turnos, setTurnos] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(null)

  const [aSuspender, setASuspender] = useState(null) // turno elegido
  const [suspension, setSuspension] = useState(suspensionVacia)
  const [errorSuspension, setErrorSuspension] = useState(null)
  const [vistaPrevia, setVistaPrevia] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const consultar = (criterios) => {
    const consulta = new URLSearchParams()
    Object.entries(criterios).forEach(([clave, valor]) => {
      if (valor !== '' && valor !== false) consulta.set(clave, valor)
    })
    return api.get(`/turnos-semanales?${consulta}`)
  }

  const buscar = async (criterios) => {
    setBuscando(true)
    setError(null)
    try {
      setTurnos(await consultar(criterios))
    } catch (err) {
      setError(comoError(err, 'No fue posible consultar los turnos. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  useEffect(() => {
    // Las listas de los filtros y el listado inicial (todos los turnos).
    Promise.all([api.get('/medicos'), api.get('/especialidades'), api.get('/consultorios')])
      .then(([medicos, especialidades, consultorios]) => setOpciones({ medicos, especialidades, consultorios }))
      .catch(() => setError(new ApiError(0, 'No fue posible cargar las listas de los filtros.')))
    consultar(FILTROS_VACIOS)
      .then(setTurnos)
      .catch((err) => setError(comoError(err, 'No fue posible consultar los turnos. Intente nuevamente.')))
  }, [])

  const cambiarFiltro = (e) => {
    const { name, type, checked, value } = e.target
    setFiltros({ ...filtros, [name]: type === 'checkbox' ? checked : value })
  }

  const enviarBusqueda = (e) => {
    e.preventDefault()
    setAviso(null)
    buscar(filtros)
  }

  const limpiarFiltros = () => {
    setFiltros(FILTROS_VACIOS)
    setAviso(null)
    buscar(FILTROS_VACIOS)
  }

  // ------------------------------------------------------------------ suspensión (CU-13)

  const abrirSuspension = (turno) => {
    setASuspender(turno)
    setSuspension(suspensionVacia())
    setErrorSuspension(null)
    setAviso(null)
  }

  const cancelarSuspension = () => {
    setASuspender(null)
    setVistaPrevia(null)
    setErrorSuspension(null)
  }

  const cambiarSuspension = (e) => setSuspension({ ...suspension, [e.target.name]: e.target.value })

  // En una suspensión permanente no hay fecha de fin: el campo «Hasta» queda deshabilitado (en gris) y no se manda.
  const temporal = suspension.modalidad === 'TEMPORAL'

  const datosDeSuspension = () => ({
    modalidad: suspension.modalidad,
    fechaDesde: suspension.fechaDesde,
    ...(suspension.modalidad === 'TEMPORAL' && suspension.fechaHasta && { fechaHasta: suspension.fechaHasta }),
    ...(suspension.motivo.trim() && { motivo: suspension.motivo.trim() }),
  })

  // Pasos 4, 6 y 7: antes de confirmar se muestra qué reservas se anularían.
  const verResumen = async (e) => {
    e.preventDefault()
    setErrorSuspension(null)
    setEnviando(true)
    try {
      const { modalidad, fechaDesde, fechaHasta } = datosDeSuspension()
      const consulta = new URLSearchParams({ modalidad, fechaDesde, ...(fechaHasta && { fechaHasta }) })
      setVistaPrevia(await api.get(`/turnos-semanales/${aSuspender.idTurnoSemanal}/suspension/vista-previa?${consulta}`))
    } catch (err) {
      setErrorSuspension(comoError(err, 'No fue posible preparar el resumen. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  const confirmarSuspension = async () => {
    setEnviando(true)
    try {
      const respuesta = await api.post(`/turnos-semanales/${aSuspender.idTurnoSemanal}/suspension`, datosDeSuspension())
      setVistaPrevia(null)
      setASuspender(null)
      setAviso(
        respuesta.reservasAvisadas === 0
          ? 'Turno suspendido correctamente. No había reservas afectadas.'
          : `Turno suspendido correctamente. Se ${respuesta.reservasAnuladas === 1 ? 'anuló' : 'anularon'} ` +
            `${cantidad(respuesta.reservasAnuladas, 'reserva', 'reservas')} y se avisó a ` +
            `${cantidad(respuesta.reservasAvisadas, 'paciente', 'pacientes')}.`,
      )
      buscar(filtros)
    } catch (err) {
      // Con un error (por ejemplo, el turno cambió mientras tanto) se vuelve al formulario con el motivo.
      setVistaPrevia(null)
      setErrorSuspension(comoError(err, 'No fue posible suspender el turno. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  // ------------------------------------------------------------------ pantalla

  return (
    <section className="panel">
      <h1>Turnos semanales</h1>

      {aviso && <p className="mensaje-exito" role="status">{aviso}</p>}
      {error && <Mensaje error={error} />}

      <form className={estilos.filtros} onSubmit={enviarBusqueda}>
        <div className={estilos.camposDeFiltro}>
          <label>
            Médico
            <select name="idMedico" value={filtros.idMedico} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              {opciones.medicos.map((m) => <option key={m.idMedico} value={m.idMedico}>{nombreMedico(m)}</option>)}
            </select>
          </label>
          <label>
            Especialidad
            <select name="codigoEspecialidad" value={filtros.codigoEspecialidad} onChange={cambiarFiltro}>
              <option value="">Todas</option>
              {opciones.especialidades.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}
            </select>
          </label>
          <label>
            Día
            <select name="dia" value={filtros.dia} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              {DIAS.map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
            </select>
          </label>
          <label>
            Consultorio
            <select name="codigoConsultorio" value={filtros.codigoConsultorio} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              {opciones.consultorios.map((c) => <option key={c.codigo} value={c.codigo}>{c.nombre}</option>)}
            </select>
          </label>
          <label>
            Estado
            <select name="estado" value={filtros.estado} onChange={cambiarFiltro}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activo</option>
              <option value="SUSPENDIDO">Suspendido</option>
            </select>
          </label>
          <label>
            Ordenar por
            <select name="ordenarPor" value={filtros.ordenarPor} onChange={cambiarFiltro}>
              {ORDENES.map(([valor, etiqueta]) => <option key={valor} value={valor}>{etiqueta}</option>)}
            </select>
          </label>
        </div>
        <label className={estilos.inversa}>
          <input type="checkbox" name="descendente" checked={filtros.descendente} onChange={cambiarFiltro} />
          Orden descendente
        </label>
        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={buscando}>Buscar</button>
          <button type="button" onClick={limpiarFiltros} disabled={buscando}>Limpiar</button>
        </div>
      </form>

      {aSuspender && (
        <form className={estilos.suspension} onSubmit={verResumen}>
          <h2>Suspender turno</h2>
          <p className={estilos.turnoElegido}>
            {NOMBRE_DIA[aSuspender.dia]} {hora(aSuspender.horaInicio)}-{hora(aSuspender.horaFin)} · {aSuspender.nombreMedico} ·{' '}
            {aSuspender.nombreEspecialidad} · {aSuspender.nombreConsultorio}
          </p>
          {errorSuspension && <Mensaje error={errorSuspension} />}
          <fieldset className={estilos.opciones}>
            <legend>Tipo de suspensión *</legend>
            <label className={estilos.opcion}>
              <input type="radio" name="modalidad" value="TEMPORAL" checked={suspension.modalidad === 'TEMPORAL'} onChange={cambiarSuspension} />
              Temporal
            </label>
            <label className={estilos.opcion}>
              <input type="radio" name="modalidad" value="PERMANENTE" checked={suspension.modalidad === 'PERMANENTE'} onChange={cambiarSuspension} />
              Permanente
            </label>
          </fieldset>
          <div className={estilos.camposDeFiltro}>
            <label>
              Desde *
              <input type="date" name="fechaDesde" value={suspension.fechaDesde} min={hoy()} onChange={cambiarSuspension} required />
            </label>
            <label>
              Hasta{temporal ? ' *' : ''}
              <input
                type="date"
                name="fechaHasta"
                value={temporal ? suspension.fechaHasta : ''}
                min={suspension.fechaDesde}
                onChange={cambiarSuspension}
                disabled={!temporal}
                required={temporal}
              />
            </label>
          </div>
          <label>
            Motivo
            <textarea name="motivo" value={suspension.motivo} onChange={cambiarSuspension} maxLength={500} rows={2} />
          </label>
          <p className={estilos.ayuda}>
            Hasta la fecha «desde» el turno sigue funcionando igual. A partir de ese día no admite reservas y las reservas de
            esas fechas se anulan.
          </p>
          <div className={estilos.acciones}>
            <button type="submit" className="principal" disabled={enviando}>Continuar</button>
            <button type="button" onClick={cancelarSuspension} disabled={enviando}>Cancelar</button>
          </div>
        </form>
      )}

      {turnos && turnos.length === 0 && (
        <p className="pendiente" role="status">No se encontraron turnos con los criterios seleccionados.</p>
      )}

      {turnos && turnos.length > 0 && (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>Médico</th>
                <th>Especialidad</th>
                <th>Día</th>
                <th>Horario</th>
                <th>Consultorio</th>
                <th>Máx. pacientes</th>
                <th>Estado</th>
                <th><span className={estilos.soloLectores}>Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {turnos.map((t) => (
                <tr key={t.idTurnoSemanal}>
                  <td>{t.nombreMedico}</td>
                  <td>{t.nombreEspecialidad}</td>
                  <td>{NOMBRE_DIA[t.dia]}</td>
                  <td>{hora(t.horaInicio)}-{hora(t.horaFin)}</td>
                  <td>{t.nombreConsultorio}</td>
                  <td>{t.cupoMaximo}</td>
                  <td>
                    <span className={t.estado === 'ACTIVO' ? estilos.activo : estilos.suspendido}>
                      {t.estado === 'ACTIVO' ? 'Activo' : 'Suspendido'}
                    </span>
                    {t.suspension && <div className={estilos.detalleSuspension}>{describirSuspension(t.suspension)}</div>}
                  </td>
                  <td>
                    {t.estado === 'ACTIVO' && !t.suspension && (
                      <button type="button" onClick={() => abrirSuspension(t)}>Suspender</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {vistaPrevia && (
        <Confirmacion
          titulo="Confirmar suspensión"
          textoConfirmar="Suspender turno"
          ancho="amplio"
          ocupado={enviando}
          onConfirmar={confirmarSuspension}
          onCancelar={() => setVistaPrevia(null)}
        >
          <p>
            {vistaPrevia.modalidad === 'TEMPORAL'
              ? `Suspensión temporal del ${fecha(vistaPrevia.fechaDesde)} al ${fecha(vistaPrevia.fechaHasta)}.`
              : `Suspensión permanente desde el ${fecha(vistaPrevia.fechaDesde)}.`}{' '}
            Afecta a {cantidad(vistaPrevia.cantidadFechas, 'fecha', 'fechas')} y a{' '}
            {cantidad(vistaPrevia.cantidadReservasAfectadas, 'reserva', 'reservas')}.
          </p>
          {vistaPrevia.cantidadReservasAfectadas > 0 ? (
            <>
              <p>
                Se anularán las reservas vigentes y se avisará a todos los pacientes por correo y en la aplicación. Las reservas con
                orden emitida no se anulan, pero también se avisa a sus pacientes:
              </p>
              <div className="tabla-scroll">
                <table className="tabla">
                  <thead>
                    <tr><th>Fecha</th><th>Número</th><th>Paciente</th><th>Situación</th></tr>
                  </thead>
                  <tbody>
                    {vistaPrevia.reservas.map((r) => (
                      <tr key={r.idReserva}>
                        <td>{fecha(r.fecha)} {hora(r.horaInicio)}</td>
                        <td>{r.numero}</td>
                        <td>{r.nombrePaciente}</td>
                        <td>{r.conOrdenEmitida ? 'Orden emitida' : 'Se anula'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <p>No hay reservas que se vean afectadas.</p>
          )}
        </Confirmacion>
      )}
    </section>
  )
}
