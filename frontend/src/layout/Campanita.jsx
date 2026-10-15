import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../services/api'
import estilos from './Campanita.module.css'

const CADA_MS = 60000 // cada cuánto se consultan las notificaciones nuevas

const formatearFecha = (iso) =>
  new Date(iso).toLocaleString('es-UY', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

// Campanita del encabezado: avisos de la aplicación (CU-6) con la cantidad sin leer. Se actualiza sola.
export default function Campanita() {
  const [lista, setLista] = useState([])
  const [abierta, setAbierta] = useState(false)
  const [error, setError] = useState(false)
  const contenedor = useRef(null)

  const cargar = useCallback(async () => {
    try {
      setLista(await api.get('/notificaciones'))
      setError(false)
    } catch {
      setError(true)
    }
  }, [])

  useEffect(() => {
    const inicial = setTimeout(cargar, 0)
    const intervalo = setInterval(cargar, CADA_MS)
    window.addEventListener('focus', cargar)
    return () => {
      clearTimeout(inicial)
      clearInterval(intervalo)
      window.removeEventListener('focus', cargar)
    }
  }, [cargar])

  // El panel se cierra al tocar fuera de él o con Escape.
  useEffect(() => {
    if (!abierta) return undefined
    const fuera = (e) => !contenedor.current?.contains(e.target) && setAbierta(false)
    const escape = (e) => e.key === 'Escape' && setAbierta(false)
    document.addEventListener('mousedown', fuera)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('mousedown', fuera)
      document.removeEventListener('keydown', escape)
    }
  }, [abierta])

  const sinLeer = lista.filter((n) => !n.leida).length

  const marcarLeida = async (n) => {
    if (n.leida) return
    setLista((actual) => actual.map((x) => (x.idNotificacion === n.idNotificacion ? { ...x, leida: true } : x)))
    try {
      await api.post(`/notificaciones/${n.idNotificacion}/lectura`)
    } catch {
      cargar() // si no se pudo marcar, se vuelve a mostrar como estaba
    }
  }

  return (
    <div className={estilos.campanita} ref={contenedor}>
      <button
        className={estilos.boton}
        aria-expanded={abierta}
        aria-controls="panel-notificaciones"
        aria-label={sinLeer > 0 ? `Notificaciones, ${sinLeer} sin leer` : 'Notificaciones'}
        onClick={() => setAbierta(!abierta)}
      >
        <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.7 21a2 2 0 0 1-3.4 0" />
        </svg>
        {sinLeer > 0 && <span className={estilos.contador}>{sinLeer > 9 ? '9+' : sinLeer}</span>}
      </button>
      {abierta && (
        <div id="panel-notificaciones" className={estilos.panel}>
          <h2>Notificaciones</h2>
          {error && <p className={estilos.vacio}>No se pudieron cargar las notificaciones.</p>}
          {!error && lista.length === 0 && <p className={estilos.vacio}>No tiene notificaciones.</p>}
          <ul className={estilos.lista}>
            {lista.map((n) => (
              <li key={n.idNotificacion}>
                <button
                  className={n.leida ? estilos.item : `${estilos.item} ${estilos.nueva}`}
                  onClick={() => marcarLeida(n)}
                >
                  <span className={estilos.mensaje}>{n.mensaje}</span>
                  <span className={estilos.fecha}>{formatearFecha(n.fecha)}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
