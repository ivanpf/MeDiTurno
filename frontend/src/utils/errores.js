import { ApiError } from '../services/api'

// Lo que atrapa un catch puede ser un error del backend (ApiError) o un fallo de red: en ese caso se muestra el mensaje de respaldo.
export const comoError = (err, mensaje) => (err instanceof ApiError ? err : new ApiError(0, mensaje))

// El backend informa los datos inválidos como «campo: mensaje»; a la persona solo le interesa el mensaje.
export const soloMensaje = (detalle) => detalle.split(': ').slice(1).join(': ') || detalle
