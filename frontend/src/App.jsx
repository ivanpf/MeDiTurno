import { useState } from 'react'

function App() {
  const [estado, setEstado] = useState('en espera')
  const [respuesta, setRespuesta] = useState('')
  const [nombreCliente, setNombreCliente] = useState('')
  const [fecha, setFecha] = useState('')
  const [hora, setHora] = useState('')
  const [cantidadPersonas, setCantidadPersonas] = useState(1)

  const listarReservas = async () => {
    setEstado('cargando')
    try {
      const res = await fetch('/api/reservas')
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const datos = await res.json()
      setRespuesta(JSON.stringify(datos))
      setEstado('exito')
    } catch (err) {
      setRespuesta(err.message)
      setEstado('error')
    }
  }

  const crearReserva = async () => {
    setEstado('cargando')
    try {
      const res = await fetch('/api/reservas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          nombreCliente,
          fecha,
          hora,
          cantidadPersonas: Number(cantidadPersonas),
        }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const datos = await res.json()
      setRespuesta(JSON.stringify(datos))
      setEstado('exito')
    } catch (err) {
      setRespuesta(err.message)
      setEstado('error')
    }
  }

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>MeDiTurno</h1>
      <h2>Comunicación Backend - Frontend</h2>
      <button type="button" onClick={listarReservas}>
        Listar reservas
      </button>
      <div>
        <input
          type="text"
          placeholder="Nombre cliente"
          value={nombreCliente}
          onChange={(e) => setNombreCliente(e.target.value)}
        />
        <input
          type="date"
          value={fecha}
          onChange={(e) => setFecha(e.target.value)}
        />
        <input
          type="time"
          value={hora}
          onChange={(e) => setHora(e.target.value)}
        />
        <input
          type="number"
          min="1"
          value={cantidadPersonas}
          onChange={(e) => setCantidadPersonas(e.target.value)}
        />
        <button type="button" onClick={crearReserva}>
          Crear reserva
        </button>
      </div>
      {estado !== 'en espera' && (
        <p>
          Estado: <strong>{estado}</strong>
          <br />
          Respuesta: {respuesta}
        </p>
      )}
    </div>
  )
}

export default App
