import { useState } from 'react'
import { NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { ETIQUETA_ROL } from '../auth/roles'
import Logo from '../components/Logo'
import { RUTAS } from '../routes'

const iniciales = (nombre) =>
  nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('')

// Estructura común de las pantallas autenticadas: encabezado, menú lateral según el rol y contenido.
export default function Layout() {
  const { rol, nombre, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [abiertos, setAbiertos] = useState({}) // grupos que el usuario abrió o cerró a mano

  if (!rol) return <Navigate to="/login" replace />

  const menu = RUTAS.filter((r) => r.roles.includes(rol))
  const grupoActual = menu.find((r) => r.path === pathname)?.grupo

  // Agrupa los ítems consecutivos con el mismo grupo; los ítems sin grupo quedan sueltos.
  const secciones = []
  menu.forEach((r) => {
    const ultima = secciones[secciones.length - 1]
    if (r.grupo && ultima?.grupo === r.grupo) ultima.items.push(r)
    else secciones.push({ grupo: r.grupo, items: [r] })
  })

  // El grupo de la pantalla actual está abierto salvo que el usuario lo haya cerrado.
  const estaAbierto = (grupo) => abiertos[grupo] ?? grupo === grupoActual
  const alternar = (grupo) => setAbiertos({ ...abiertos, [grupo]: !estaAbierto(grupo) })

  const salir = () => {
    cerrarSesion() // TODO(CU-2): confirmar y pedir al backend que invalide la sesión
    navigate('/login')
  }

  return (
    <div className="layout">
      <header className="encabezado">
        <Logo variante="claro" />
        <div className="usuario">
          {nombre && (
            <span className="persona">
              <span className="avatar">{iniciales(nombre)}</span>
              {nombre}
            </span>
          )}
          <span className="rol">{ETIQUETA_ROL[rol]}</span>
          <button onClick={salir}>Cerrar sesión</button>
        </div>
      </header>
      <div className="cuerpo">
        <nav className="menu">
          {secciones.map((sec) =>
            sec.grupo ? (
              <div key={sec.grupo}>
                <button className="grupo" aria-expanded={estaAbierto(sec.grupo)} onClick={() => alternar(sec.grupo)}>
                  {sec.grupo}
                </button>
                {estaAbierto(sec.grupo) && (
                  <div className="subitems">
                    {sec.items.map((r) => (
                      <NavLink key={r.path} to={r.path} end>
                        {r.etiqueta}
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <NavLink key={sec.items[0].path} to={sec.items[0].path} end>
                {sec.items[0].etiqueta}
              </NavLink>
            ),
          )}
        </nav>
        <main className="contenido">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
