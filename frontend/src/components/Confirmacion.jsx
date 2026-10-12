import { useEffect, useRef } from 'react'

// Ventana de confirmación (modal). Se muestra mientras está en pantalla, así que se usa con un estado:
//   {abierta && <Confirmacion titulo="..." textoConfirmar="Aceptar" onConfirmar={...} onCancelar={...}>Texto</Confirmacion>}
// Usa <dialog>: Esc la cierra, el foco queda dentro y el resto de la pantalla no se puede usar.
export default function Confirmacion({ titulo, textoConfirmar = 'Confirmar', ocupado = false, onConfirmar, onCancelar, children }) {
  const dialogo = useRef(null)

  useEffect(() => {
    if (!dialogo.current.open) dialogo.current.showModal()
  }, [])

  const cancelarConEsc = (e) => {
    e.preventDefault() // el cierre lo decide el componente que la usa
    if (!ocupado) onCancelar()
  }

  return (
    <dialog ref={dialogo} className="modal" onCancel={cancelarConEsc}>
      <h2>{titulo}</h2>
      <p>{children}</p>
      <div className="modal-acciones">
        <button type="button" onClick={onCancelar} disabled={ocupado}>Cancelar</button>
        <button type="button" className="principal" onClick={onConfirmar} disabled={ocupado}>{textoConfirmar}</button>
      </div>
    </dialog>
  )
}
