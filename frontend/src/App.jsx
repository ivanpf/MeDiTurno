import { Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import SoloRol from './auth/SoloRol'
import LoginPage from './features/auth/LoginPage'
import RecuperarAccesoPage from './features/auth/RecuperarAccesoPage'
import RegistroPage from './features/auth/RegistroPage'
import Layout from './layout/Layout'
import { RUTAS } from './routes'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegistroPage />} />
      <Route path="/recuperar-acceso" element={<RecuperarAccesoPage />} />

      <Route element={<Layout />}>
        {RUTAS.map(({ path, roles, Componente }) => (
          <Route
            key={path}
            path={path}
            element={
              <SoloRol roles={roles}>
                <Suspense fallback={<p role="status">Cargando...</p>}>
                  <Componente />
                </Suspense>
              </SoloRol>
            }
          />
        ))}
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
