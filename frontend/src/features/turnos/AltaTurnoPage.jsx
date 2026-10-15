import { useEffect, useState } from 'react'
import { api, ApiError } from '../../services/api'
import estilos from './AltaTurnoPage.module.css'

const DIAS = [
  ['LUNES', 'Lunes'],
  ['MARTES', 'Martes'],
  ['MIERCOLES', 'Miércoles'],
  ['JUEVES', 'Jueves'],
  ['VIERNES', 'Viernes'],
  ['SABADO', 'Sábado'],
  ['DOMINGO', 'Domingo'],
]

// Fecha de hoy en la zona horaria de quien usa la pantalla (toISOString devolvería la de UTC).
const hoy = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10)

// El formulario arranca con la vigencia en la fecha de hoy. Es una función para que, si la pantalla queda abierta de un
// día para el otro, al limpiarla vuelva a tomar la fecha de ese momento.
const formularioInicial = () => ({
  idMedico: '',
  codigoEspecialidad: '',
  codigoConsultorio: '',
  dias: [],
  horaInicio: '09:00',
  horaFin: '12:00',
  cupoMaximo: '10',
  vigenteDesde: hoy(),
})

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle

// Las listas: GET /api/medicos (médicos activos con sus especialidades activas) y GET /api/consultorios (todos; acá solo
// se ofrecen los activos).
const nombreDe = (medico) => `${medico.apellido}, ${medico.nombre}`

// CU-12 Alta de un turno semanal de atención. Se crea un turno por cada día elegido.
export default function AltaTurnoPage() {
  const [listas, setListas] = useState(null) // { medicos, consultorios }
  const [datos, setDatos] = useState(formularioInicial)
  const [error, setError] = useState(null)
  const [registrados, setRegistrados] = useState(0)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    Promise.all([api.get('/medicos'), api.get('/consultorios')])
      .then(([medicos, consultorios]) => setListas({ medicos, consultorios }))
      .catch((err) => setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible cargar los datos del formulario.')))
  }, [])

  const medicos = listas?.medicos ?? []
  const consultorios = listas?.consultorios.filter((c) => c.estado === 'ACTIVO') ?? []
  const medico = medicos.find((m) => String(m.idMedico) === datos.idMedico)

  // Del médico elegido solo se ofrecen sus especialidades. Los consultorios no dependen del médico ni de la especialidad.
  const especialidadesDelMedico = medico?.especialidades ?? []

  const cambiar = (e) => {
    setDatos({ ...datos, [e.target.name]: e.target.value })
    setRegistrados(0)
  }

  // Al elegir médico se vuelve a elegir la especialidad; si tiene una sola, queda elegida.
  const cambiarMedico = (e) => {
    const elegido = medicos.find((m) => String(m.idMedico) === e.target.value)
    const unica = elegido?.especialidades.length === 1 ? elegido.especialidades[0].codigo : ''
    setDatos({ ...datos, idMedico: e.target.value, codigoEspecialidad: unica })
    setRegistrados(0)
  }

  const alternarDia = (dia) => {
    const dias = datos.dias.includes(dia) ? datos.dias.filter((d) => d !== dia) : [...datos.dias, dia]
    setDatos({ ...datos, dias })
    setRegistrados(0)
  }

  const limpiar = () => {
    setDatos(formularioInicial())
    setError(null)
    setRegistrados(0)
  }

  const registrar = async (e) => {
    e.preventDefault()
    setError(null)
    setRegistrados(0)
    setEnviando(true)
    try {
      const turnos = await api.post('/turnos-semanales', {
        ...datos,
        idMedico: Number(datos.idMedico),
        cupoMaximo: Number(datos.cupoMaximo),
      })
      setDatos(formularioInicial())
      setRegistrados(turnos.length)
    } catch (err) {
      // Con un error del backend el formulario conserva los datos ingresados.
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible registrar el turno. Intente nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel">
      <h1>Alta de turno semanal</h1>

      <div className={estilos.mensajes}>
        {registrados > 0 && (
          <p className="mensaje-exito" role="status">
            {registrados === 1
              ? 'Turno registrado. Estará disponible para reservas desde la fecha de vigencia indicada.'
              : `${registrados} turnos registrados. Estarán disponibles para reservas desde la fecha de vigencia indicada.`}
          </p>
        )}
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

      <form className={estilos.formulario} onSubmit={registrar}>
        <div className={estilos.fila}>
          <label>
            Médico *
            <select name="idMedico" value={datos.idMedico} onChange={cambiarMedico} disabled={!listas} required>
              <option value="">Seleccione un médico</option>
              {medicos.map((m) => <option key={m.idMedico} value={m.idMedico}>{nombreDe(m)}</option>)}
            </select>
          </label>
          <label>
            Especialidad *
            <select name="codigoEspecialidad" value={datos.codigoEspecialidad} onChange={cambiar} disabled={!medico} required>
              <option value="">{medico ? 'Seleccione una especialidad' : 'Primero elija el médico'}</option>
              {especialidadesDelMedico.map((e) => <option key={e.codigo} value={e.codigo}>{e.nombre}</option>)}
            </select>
          </label>
          <label>
            Consultorio *
            <select name="codigoConsultorio" value={datos.codigoConsultorio} onChange={cambiar} disabled={!listas} required>
              <option value="">Seleccione un consultorio</option>
              {consultorios.map((c) => <option key={c.codigo} value={c.codigo}>{c.nombre}</option>)}
            </select>
          </label>
        </div>

        <fieldset className={estilos.dias}>
          <legend>Días de la semana *</legend>
          {DIAS.map(([valor, etiqueta]) => (
            <label key={valor} className={estilos.dia}>
              <input type="checkbox" checked={datos.dias.includes(valor)} onChange={() => alternarDia(valor)} />
              {etiqueta}
            </label>
          ))}
        </fieldset>

        <div className={estilos.fila}>
          <label>
            Hora de inicio *
            <input type="time" name="horaInicio" value={datos.horaInicio} onChange={cambiar} required />
          </label>
          <label>
            Hora de fin *
            <input type="time" name="horaFin" value={datos.horaFin} onChange={cambiar} required />
          </label>
          <label>
            Máximo de pacientes *
            <input type="number" name="cupoMaximo" value={datos.cupoMaximo} onChange={cambiar} min={1} required />
          </label>
          <label>
            Vigente desde *
            <input type="date" name="vigenteDesde" value={datos.vigenteDesde} onChange={cambiar} min={hoy()} required />
          </label>
        </div>

        <div className={estilos.acciones}>
          <button type="submit" className="principal" disabled={enviando || !listas}>Registrar turno</button>
          <button type="button" onClick={limpiar} disabled={enviando}>Cancelar</button>
        </div>
      </form>
    </section>
  )
}
