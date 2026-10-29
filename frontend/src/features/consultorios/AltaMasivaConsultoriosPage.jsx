import CargaMasivaCsv from '../../components/CargaMasivaCsv'

const EJEMPLO = {
  archivo: 'consultorios-ejemplo.csv',
  contenido: 'código,nombre,ubicación,descripción,estado\nC1,Consultorio 1,Planta baja,Consultorio con camilla,activo\nC2,Consultorio 2,Piso 2 ala norte,,inactivo\n',
}

// CU-38 Alta masiva de consultorios por CSV.
export default function AltaMasivaConsultoriosPage() {
  return (
    <CargaMasivaCsv titulo="Alta masiva de consultorios" ruta="/consultorios" singular="consultorio" plural="consultorios" ejemplo={EJEMPLO}>
      Cargue un archivo CSV en UTF-8 con las columnas <strong>código</strong>, <strong>nombre</strong>, <strong>ubicación</strong>,{' '}
      <strong>descripción</strong> (puede quedar vacía) y <strong>estado</strong> (activo o inactivo).
    </CargaMasivaCsv>
  )
}
