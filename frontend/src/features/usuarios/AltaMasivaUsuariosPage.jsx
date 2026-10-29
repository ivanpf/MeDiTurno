import CargaMasivaCsv from '../../components/CargaMasivaCsv'

const EJEMPLO = {
  archivo: 'usuarios-internos-ejemplo.csv',
  contenido: 'cédula,nombre,apellido,correo,fecha de nacimiento,rol,matrícula,especialidades\n'
    + '1.234.567-8,Ana,López,ana.lopez@ejemplo.com,1985-05-17,administrador,,\n'
    + '2.345.678-9,Luis,Díaz,luis.diaz@ejemplo.com,27/03/1990,funcionario,,\n'
    + '3.456.789-0,María,Fernández,maria.fernandez@ejemplo.com,1978-11-02,médico,12345,CARD|CLIN\n',
}

// CU-35 Alta masiva de usuarios internos por CSV. Cada cuenta queda activa, con una contraseña temporal que se envía al correo.
export default function AltaMasivaUsuariosPage() {
  return (
    <CargaMasivaCsv titulo="Alta masiva de usuarios internos" ruta="/usuarios-internos" singular="usuario" plural="usuarios" ejemplo={EJEMPLO}>
      Cargue un archivo CSV en UTF-8 (hasta 100 filas) con las columnas <strong>cédula</strong>, <strong>nombre</strong>,{' '}
      <strong>apellido</strong>, <strong>correo</strong>, <strong>fecha de nacimiento</strong> (AAAA-MM-DD o DD/MM/AAAA) y{' '}
      <strong>rol</strong> (administrador, funcionario o médico). Para los médicos agregue también la <strong>matrícula</strong> y
      las <strong>especialidades</strong>: los códigos de las especialidades activas, separados por «|». Al importar se crean las
      cuentas, activas, y se envía a cada correo una contraseña temporal.
    </CargaMasivaCsv>
  )
}
