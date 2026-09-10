import { useEffect, useState } from 'react'

const API_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'

function App() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch(`${API_URL}/api/health`)
      .then((res) => res.json())
      .then(setData)
      .catch((err) => setError(String(err)))
  }, [])

  return (
    <main>
      <h1>MeDiTurno — POC de deploy</h1>
      {error && <p className="error">Error: {error}</p>}
      {data ? (
        <pre>{JSON.stringify(data, null, 2)}</pre>
      ) : (
        !error && <p>Cargando desde {API_URL}...</p>
      )}
    </main>
  )
}

export default App