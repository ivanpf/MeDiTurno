import { useEffect, useState } from 'react'
import { api, ApiError } from '../../services/api'
import { comoError } from '../../utils/errores'

// Fecha de hoy en Montevideo, como «2031-03-17» (la misma zona que usa el backend).
const hoy = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Montevideo' }).format(new Date())

// Período con el que se abren las pantallas: los últimos 90 días, hasta hoy.
export function periodoPorDefecto() {
  const desde = new Date(`${hoy()}T12:00:00Z`)
  desde.setUTCDate(desde.getUTCDate() - 90)
  return { fechaDesde: desde.toISOString().slice(0, 10), fechaHasta: hoy() }
}

// Consulta de una estadística: guarda los filtros, pide `ruta` al backend con ellos y devuelve la lista. Se consulta sola al abrir
// la pantalla, con los filtros iniciales, y de nuevo cada vez que se llama a buscar (o a limpiar, que vuelve a los iniciales).
export function useEstadistica(ruta, filtrosIniciales) {
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [resultado, setResultado] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState(null)

  const consultar = async (criterios) => {
    if (criterios.fechaDesde && criterios.fechaHasta && criterios.fechaDesde > criterios.fechaHasta) {
      setError(new ApiError(400, 'La fecha desde no puede ser posterior a la fecha hasta.'))
      return
    }
    setBuscando(true)
    setError(null)
    try {
      const consulta = new URLSearchParams()
      Object.entries(criterios).forEach(([clave, valor]) => {
        if (valor !== '') consulta.set(clave, valor)
      })
      setResultado(await api.get(`${ruta}?${consulta}`))
    } catch (err) {
      setResultado(null)
      setError(comoError(err, 'No fue posible consultar la estadística. Intente nuevamente.'))
    } finally {
      setBuscando(false)
    }
  }

  useEffect(() => {
    consultar(filtrosIniciales)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo al abrir la pantalla
  }, [])

  const cambiarFiltro = (e) => setFiltros({ ...filtros, [e.target.name]: e.target.value })

  const buscar = (e) => {
    e.preventDefault()
    consultar(filtros)
  }

  const limpiar = () => {
    setFiltros(filtrosIniciales)
    consultar(filtrosIniciales)
  }

  return { filtros, setFiltros, cambiarFiltro, resultado, buscando, error, buscar, limpiar }
}

// Las listas de los filtros (médicos, especialidades, consultorios): se piden las que se nombren, de una vez.
export function useListas(...nombres) {
  const [listas, setListas] = useState({ medicos: [], especialidades: [], consultorios: [] })
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all(nombres.map((nombre) => api.get(`/${nombre}`)))
      .then((respuestas) => setListas((actuales) => ({ ...actuales, ...Object.fromEntries(nombres.map((n, i) => [n, respuestas[i]])) })))
      .catch(() => setError(new ApiError(0, 'No fue posible cargar las listas de los filtros.')))
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo al abrir la pantalla
  }, [])

  return { listas, errorListas: error }
}

// Con una especialidad elegida, solo se ofrecen los médicos que la atienden. Cambiar la especialidad quita el médico elegido si
// ya no la atiende.
export function filtroMedicos(listas, filtros, setFiltros) {
  const atiende = (m, codigo) => m.especialidades.some((e) => e.codigo === codigo)
  const medicos = filtros.codigoEspecialidad ? listas.medicos.filter((m) => atiende(m, filtros.codigoEspecialidad)) : listas.medicos
  const cambiarEspecialidad = (e) => {
    const codigo = e.target.value
    const medicoSigue = !codigo || listas.medicos.some((m) => String(m.idMedico) === filtros.idMedico && atiende(m, codigo))
    setFiltros({ ...filtros, codigoEspecialidad: codigo, idMedico: medicoSigue ? filtros.idMedico : '' })
  }
  return { medicos, cambiarEspecialidad }
}
