import { useState } from 'react'
import { NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { ETIQUETA_ROL } from '../auth/roles'
import Campanita from './Campanita'
import Confirmacion from '../components/Confirmacion'
import Logo from '../components/Logo'
import { RUTAS } from '../routes'
import estilos from './Layout.module.css'

// NavLink agrega su propia clase `active`; con CSS Modules se usa la función de className para aplicar la local.
const claseItem = ({ isActive }) => (isActive ? `${estilos.item} ${estilos.activo}` : estilos.item)

const iniciales = (nombre) =>
  nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('')

// Estructura común de las pantallas autenticadas: encabezado, menú lateral según el rol y contenido.
export default function Layout() {
  const { rol, nombre, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [abiertos, setAbiertos] = useState({}) // grupos que el usuario abrió o cerró a mano
  const [menuAbierto, setMenuAbierto] = useState(false) // solo se usa en pantallas chicas
  const [confirmandoSalida, setConfirmandoSalida] = useState(false) // CU-2: ventana «¿Cerrar sesión?»
  const [saliendo, setSaliendo] = useState(false)

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

  const cerrarMenu = () => setMenuAbierto(false)

  // CU-2: «Cerrar sesión» pide confirmar; recién al confirmar se cierra la sesión.
  const pedirConfirmacion = () => {
    cerrarMenu()
    setConfirmandoSalida(true)
  }

  const salir = async () => {
    setSaliendo(true)
    await cerrarSesion()
    navigate('/login')
  }

  return (
    <div>
      <header className={estilos.encabezado}>
        <Logo variante="claro" />
        <div className={estilos.derecha}>
          {rol === 'paciente' && <Campanita />}
          {/* En pantallas grandes: usuario, rol y cerrar sesión en el encabezado */}
          <div className={estilos.usuario}>
            {nombre && (
              <span className={estilos.persona}>
                <span className={estilos.avatar}>{iniciales(nombre)}</span>
                {nombre}
              </span>
            )}
            <span className={estilos.rol}>{ETIQUETA_ROL[rol]}</span>
            <button onClick={pedirConfirmacion}>Cerrar sesión</button>
          </div>
          {/* En pantallas chicas: un botón que abre y cierra el menú */}
          <button
            className={estilos.botonMenu}
            aria-expanded={menuAbierto}
            aria-controls="menu-principal"
            onClick={() => setMenuAbierto(!menuAbierto)}
          >
            {menuAbierto ? 'Cerrar menú' : 'Menú'}
          </button>
        </div>
      </header>
      <div className={estilos.cuerpo}>
        <nav id="menu-principal" className={menuAbierto ? `${estilos.menu} ${estilos.abierto}` : estilos.menu}>
          {/* Solo en pantallas chicas: usuario, rol y cerrar sesión dentro del menú */}
          <div className={estilos.usuarioMovil}>
            {nombre && (
              <span className={estilos.persona}>
                <span className={estilos.avatar}>{iniciales(nombre)}</span>
                {nombre}
              </span>
            )}
            <span className={estilos.rol}>{ETIQUETA_ROL[rol]}</span>
            <button onClick={pedirConfirmacion}>Cerrar sesión</button>
          </div>
          {secciones.map((sec) =>
            sec.grupo ? (
              <div key={sec.grupo}>
                <button className={estilos.grupo} aria-expanded={estaAbierto(sec.grupo)} onClick={() => alternar(sec.grupo)}>
                  {sec.grupo}
                </button>
                {estaAbierto(sec.grupo) && (
                  <div className={estilos.subitems}>
                    {sec.items.map((r) => (
                      <NavLink key={r.path} to={r.path} end className={claseItem} onClick={cerrarMenu}>
                        {r.etiqueta}
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <NavLink key={sec.items[0].path} to={sec.items[0].path} end className={claseItem} onClick={cerrarMenu}>
                {sec.items[0].etiqueta}
              </NavLink>
            ),
          )}
        </nav>
        <main className={estilos.contenido}>
          <Outlet />
        </main>
      </div>
      {confirmandoSalida && (
        <Confirmacion
          titulo="Cerrar sesión"
          textoConfirmar="Cerrar sesión"
          ocupado={saliendo}
          onConfirmar={salir}
          onCancelar={() => setConfirmandoSalida(false)}
        >
          ¿Desea cerrar su sesión?
        </Confirmacion>
      )}
    </div>
  )
}
