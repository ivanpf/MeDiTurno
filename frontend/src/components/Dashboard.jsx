import { useState, useEffect } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  PieChart, Pie, Cell
} from 'recharts'

const COLORES = ['#007bff', '#28a745', '#dc3545', '#ffc107', '#17a2b8', '#6f42c1']

function Dashboard() {
  const [reservas, setReservas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch('/api/reservas')
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(datos => {
        setReservas(datos)
        setCargando(false)
      })
      .catch(err => {
        setError(err.message)
        setCargando(false)
      })
  }, [])

  if (cargando) return <p>Cargando datos...</p>
  if (error) return <p style={{ color: 'red' }}>Error: {error}</p>
  if (reservas.length === 0) return <p>No hay reservas para graficar.</p>

  // Reservas por persona (barras)
  const porPersona = {}
  reservas.forEach(r => {
    porPersona[r.nombreCliente] = (porPersona[r.nombreCliente] || 0) + 1
  })
  const datosBarras = Object.entries(porPersona).map(([nombre, cantidad]) => ({
    nombre,
    cantidad
  }))

  // Reservas por estado (torta)
  const porEstado = {}
  reservas.forEach(r => {
    porEstado[r.estado] = (porEstado[r.estado] || 0) + 1
  })
  const datosTorta = Object.entries(porEstado).map(([estado, cantidad]) => ({
    name: estado,
    value: cantidad
  }))

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h2>Dashboard de Reservas</h2>
      <p>Total de reservas: <strong>{reservas.length}</strong></p>

      <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', marginTop: '2rem' }}>
        {/* Grafico de barras */}
        <div style={{ flex: 1, minWidth: 300 }}>
          <h3>Reservas por Persona</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={datosBarras}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="nombre" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="cantidad" fill="#007bff" name="Reservas" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Grafico de torta */}
        <div style={{ flex: 1, minWidth: 300 }}>
          <h3>Reservas por Estado</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={datosTorta}
                cx="50%"
                cy="50%"
                outerRadius={100}
                dataKey="value"
                label={({ name, value }) => `${name}: ${value}`}
              >
                {datosTorta.map((_, index) => (
                  <Cell key={index} fill={COLORES[index % COLORES.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
