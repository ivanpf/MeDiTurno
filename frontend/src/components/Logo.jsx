import logo from '../assets/logo.png'

// Logotipo de MeDiTurno. `variante="claro"` es para fondos azules: el logo va dentro de un círculo blanco.
export default function Logo({ variante = 'oscuro' }) {
  return (
    <span className={`logo ${variante}`}>
      <span className="logo-marca">
        <img src={logo} alt="" />
      </span>
      MeDiTurno
    </span>
  )
}
