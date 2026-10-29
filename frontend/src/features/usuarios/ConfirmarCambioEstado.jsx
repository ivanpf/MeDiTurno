import { useState } from 'react'
import Confirmacion from '../../components/Confirmacion'
import { api } from '../../services/api'
import { comoError } from '../../utils/errores'

// CU-46: ventana de confirmación para activar o inactivar la cuenta de un usuario interno (el listado y la pantalla de
// activación e inactivación la comparten). Las reglas las aplica el backend: si no corresponde, lo informa acá y no cambia nada.
//   <ConfirmarCambioEstado usuario={u} onTerminar={() => ...} onCancelar={() => ...} />
export default function ConfirmarCambioEstado({ usuario, onTerminar, onCancelar }) {
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const inactivar = usuario.estado === 'ACTIVO'

  const confirmar = async () => {
    setEnviando(true)
    setError(null)
    try {
      await api.post(`/usuarios-internos/${usuario.idUsuario}/${inactivar ? 'inactivacion' : 'activacion'}`)
      onTerminar(inactivar ? 'inactivada' : 'activada')
    } catch (err) {
      setError(comoError(err, `No fue posible ${inactivar ? 'inactivar' : 'activar'} la cuenta. Intente nuevamente.`))
      setEnviando(false)
    }
  }

  return (
    <Confirmacion
      titulo={inactivar ? 'Inactivar cuenta' : 'Activar cuenta'}
      textoConfirmar={inactivar ? 'Inactivar' : 'Activar'}
      ocupado={enviando}
      onConfirmar={confirmar}
      onCancelar={onCancelar}
    >
      {inactivar ? (
        <p>
          ¿Inactivar la cuenta de <strong>{usuario.apellido}, {usuario.nombre}</strong>? No podrá iniciar sesión mientras esté inactiva.
          Sus reservas, órdenes y registros clínicos se conservan.
        </p>
      ) : (
        <p>¿Activar la cuenta de <strong>{usuario.apellido}, {usuario.nombre}</strong>? Podrá iniciar sesión nuevamente.</p>
      )}
      {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
    </Confirmacion>
  )
}
