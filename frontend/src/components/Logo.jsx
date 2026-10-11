import logo from '../assets/logo.png'
import estilos from './Logo.module.css'

// Logotipo de MeDiTurno. `variante="claro"` es para fondos azules: el logo va dentro de un círculo blanco.
// `grande` agranda el logo (panel de marca del login).
export default function Logo({ variante = 'oscuro', grande = false }) {
  return (
    <span className={[estilos.logo, estilos[variante], grande && estilos.grande].filter(Boolean).join(' ')}>
      <span className={estilos.marca}>
        <img src={logo} alt="" />
      </span>
      MeDiTurno
    </span>
  )
}
