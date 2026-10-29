import CargaMasivaCsv from '../../components/CargaMasivaCsv'

const EJEMPLO = {
  archivo: 'pacientes-ejemplo.csv',
  contenido: 'cédula,nombre,apellido,correo,fecha de nacimiento,teléfono,domicilio\n1.234.567-8,Ana,López,ana.lopez@ejemplo.com,1990-05-17,099 123 456,Av. Italia 1234\n2.345.678-9,Luis,Díaz,luis.diaz@ejemplo.com,27/03/1985,,\n',
}

// CU-32 Alta masiva de pacientes por CSV. Incorpora a las personas al padrón; no crea cuentas de acceso ni contraseñas.
export default function AltaMasivaPacientesPage() {
  return (
    <CargaMasivaCsv titulo="Alta masiva de pacientes" ruta="/pacientes" singular="paciente" plural="pacientes" ejemplo={EJEMPLO}>
      Cargue un archivo CSV en UTF-8 con las columnas <strong>cédula</strong>, <strong>nombre</strong>, <strong>apellido</strong>,{' '}
      <strong>correo</strong> y <strong>fecha de nacimiento</strong> (AAAA-MM-DD o DD/MM/AAAA); <strong>teléfono</strong> y{' '}
      <strong>domicilio</strong> son opcionales. Los pacientes quedan en el padrón y podrán registrarse como usuarios: no se crean cuentas de acceso.
    </CargaMasivaCsv>
  )
}
