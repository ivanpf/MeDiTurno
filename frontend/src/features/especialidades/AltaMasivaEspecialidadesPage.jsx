import CargaMasivaCsv from '../../components/CargaMasivaCsv'

const EJEMPLO = {
  archivo: 'especialidades-ejemplo.csv',
  contenido: 'código,nombre,descripción,requiere pase,estado\nCARD,Cardiología,Enfermedades del corazón,no,activa\nDERM,Dermatología,Enfermedades de la piel,sí,activa\n',
}

// CU-29 Alta masiva de especialidades médicas por CSV.
export default function AltaMasivaEspecialidadesPage() {
  return (
    <CargaMasivaCsv titulo="Alta masiva de especialidades" ruta="/especialidades" singular="especialidad" plural="especialidades" ejemplo={EJEMPLO}>
      Cargue un archivo CSV en UTF-8 con las columnas <strong>código</strong>, <strong>nombre</strong>, <strong>descripción</strong> y{' '}
      <strong>requiere pase</strong> (sí o no). Opcionalmente puede incluir <strong>estado</strong> (activa o inactiva; sin ese dato
      quedan activas).
    </CargaMasivaCsv>
  )
}
