import { useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../../services/api'
import estilos from './HistoriaClinicaAtencionPage.module.css'

const fechaHora = (valor) => `${valor.slice(0, 10).split('-').reverse().join('/')} ${valor.slice(11, 16)}`
const comoError = (err) => err instanceof ApiError ? err : new ApiError(0, 'No fue posible consultar la historia clínica. Intente nuevamente.')

export default function HistoriaClinicaAtencionPage() {
  const [parametros] = useSearchParams()
  const { state } = useLocation()
  const idPaciente = parametros.get('idPaciente') ?? ''
  if (!/^[1-9]\d*$/.test(idPaciente)) {
    return (
      <section className="panel">
        <h1>Historia clínica durante la atención</h1>
        <p className="pendiente">Seleccione primero un paciente de sus turnos para consultar su historia clínica.</p>
        <Link className="enlace-volver" to="/medico/pacientes">Ir a Mis pacientes</Link>
      </section>
    )
  }
  return <HistoriaDePaciente key={idPaciente} idPaciente={idPaciente} nombreSeleccionado={String(state?.idPaciente) === idPaciente ? state.nombrePaciente : null} />
}

function HistoriaDePaciente({ idPaciente, nombreSeleccionado }) {
  const [historial, setHistorial] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [intento, setIntento] = useState(0)
  const [idConsulta, setIdConsulta] = useState(null)
  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)
  const [errorDetalle, setErrorDetalle] = useState(null)
  const [intentoDetalle, setIntentoDetalle] = useState(0)
  const consulta = new URLSearchParams({ idPaciente }).toString()

  useEffect(() => {
    let vigente = true
    api.get(`/consultas/historia-clinica?${consulta}`)
      .then((lista) => { if (vigente) setHistorial(lista) })
      .catch((err) => { if (vigente) setError(comoError(err)) })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [consulta, intento])

  useEffect(() => {
    let vigente = true
    if (!idConsulta) return () => { vigente = false }
    api.get(`/consultas/historia-clinica/${idConsulta}?${consulta}`)
      .then((respuesta) => { if (vigente) setDetalle(respuesta) })
      .catch((err) => {
        if (!vigente) return
        const fallo = comoError(err)
        if (fallo.estado === 403 || fallo.estado === 401) {
          setHistorial([])
          setDetalle(null)
          setIdConsulta(null)
          setError(fallo)
        } else { setErrorDetalle(fallo) }
      })
      .finally(() => { if (vigente) setCargandoDetalle(false) })
    return () => { vigente = false }
  }, [idConsulta, consulta, intentoDetalle])

  const actualizar = () => {
    setCargando(true)
    setError(null)
    setHistorial([])
    setIdConsulta(null)
    setDetalle(null)
    setErrorDetalle(null)
    setCargandoDetalle(false)
    setIntento((valor) => valor + 1)
  }
  const verDetalle = (id) => {
    setIdConsulta(id)
    setDetalle(null)
    setErrorDetalle(null)
    setCargandoDetalle(true)
    setIntentoDetalle((valor) => valor + 1)
  }
  const nombre = historial[0]?.nombrePaciente ?? nombreSeleccionado

  return (
    <section className={`panel ${estilos.pagina}`}>
      <h1>Historia clínica durante la atención</h1>
      <div className={estilos.acciones}>
        <Link to="/medico/pacientes">Volver a Mis pacientes</Link>
        <button type="button" onClick={actualizar} disabled={cargando || cargandoDetalle}>Actualizar historia</button>
      </div>
      {cargando && <p role="status">Verificando la atención y cargando las consultas anteriores…</p>}
      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
      {!cargando && !error && (
        <>
          {nombre && <h2>{nombre}</h2>}
          <p>Consultas anteriores, ordenadas de la más reciente a la más antigua. Seleccione una para ver su detalle.</p>
          {!historial.length && <p className="pendiente" role="status">El paciente no posee antecedentes de consultas disponibles para visualizar.</p>}
          {historial.length > 0 && (
            <div className="tabla-scroll">
              <table className="tabla">
                <caption className={estilos.leyenda}>Consultas anteriores del paciente</caption>
                <thead><tr><th scope="col">Fecha y hora</th><th scope="col">Especialidad</th><th scope="col">Médico</th><th scope="col">Detalle</th></tr></thead>
                <tbody>{historial.map((item) => (
                  <tr key={item.idConsulta}>
                    <td>{fechaHora(item.fecha)}</td><td>{item.especialidad}</td><td>{item.nombreMedico}</td>
                    <td><button type="button" aria-pressed={idConsulta === item.idConsulta} onClick={() => verDetalle(item.idConsulta)}>Ver detalle</button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
          {cargandoDetalle && <p role="status">Cargando detalle de la consulta…</p>}
          {errorDetalle && <div className="mensaje-error" role="alert">{errorDetalle.mensaje}<button type="button" onClick={() => verDetalle(idConsulta)}>Reintentar detalle</button></div>}
          {detalle && !cargandoDetalle && !errorDetalle && (
            <article className={estilos.detalle} aria-label="Detalle de la consulta">
              <h2>Consulta del {fechaHora(detalle.fecha)}</h2>
              <p>{detalle.especialidad} · {detalle.nombreMedico}</p>
              <h3>Motivo de la consulta</h3><p className={estilos.textoClinico}>{detalle.motivo}</p>
              <h3>Observaciones clínicas</h3><p className={estilos.textoClinico}>{detalle.observaciones || 'Sin observaciones registradas.'}</p>
              <h3>Indicaciones</h3><p className={estilos.textoClinico}>{detalle.indicaciones || 'Sin indicaciones registradas.'}</p>
            </article>
          )}
        </>
      )}
    </section>
  )
}
