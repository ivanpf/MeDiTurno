import Logo from './Logo'

// Estructura de las pantallas sin sesión (login, registro, recuperar acceso):
// panel de marca a la izquierda y el formulario a la derecha.
export default function PantallaAcceso({ children }) {
  return (
    <div className="acceso">
      <aside className="acceso-marca">
        <Logo variante="claro" />
        <h2>Sus turnos médicos, en un solo lugar</h2>
        <p>Reserve, consulte y gestione sus consultas de forma simple y segura.</p>
      </aside>
      <main className="acceso-formulario">{children}</main>
    </div>
  )
}
