import Logo from './Logo'
import estilos from './PantallaAcceso.module.css'

// Estructura de las pantallas sin sesión (login, registro, recuperar acceso):
// panel de marca a la izquierda y el formulario a la derecha.
export default function PantallaAcceso({ children }) {
  return (
    <div className={estilos.acceso}>
      <aside className={estilos.marca}>
        <Logo variante="claro" grande />
        <h2>Sus turnos médicos, en un solo lugar</h2>
        <p>Reserve, consulte y gestione sus consultas de forma simple y segura.</p>
      </aside>
      <main className={estilos.formulario}>{children}</main>
    </div>
  )
}
