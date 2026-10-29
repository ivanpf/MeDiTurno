import { useEffect, useState } from 'react'
import Confirmacion from '../../components/Confirmacion'
import { api, ApiError } from '../../services/api'
import estilos from './EliminarAnuncioPage.module.css'

const SITUACION = {
  ELIMINADO: { texto: 'Eliminado', clase: 'eliminado' },
  VIGENTE: { texto: 'Vigente', clase: 'vigente' },
  PROGRAMADO: { texto: 'Programado', clase: 'programado' },
  VENCIDO: { texto: 'Vencido', clase: 'vencido' },
}

// Las fechas sin hora (aaaa-mm-dd) se arman a mano: new Date() las leería como UTC y mostraría el día anterior.
const formatearDia = (dia) => (dia ? dia.split('-').reverse().join('/') : 'Sin fecha')
const formatearFecha = (fecha) => new Date(fecha).toLocaleDateString('es-UY')

// CU-18 Eliminar anuncios.
export default function EliminarAnuncioPage() {
  const [anuncios, setAnuncios] = useState(null)
  const [error, setError] = useState(null)
  const [eliminado, setEliminado] = useState(null) // título del último anuncio eliminado
  const [porEliminar, setPorEliminar] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const [version, setVersion] = useState(0) // al cambiar, se vuelve a pedir la lista

  useEffect(() => {
    let cancelado = false
    api.get('/anuncios/gestion')
      .then((datos) => !cancelado && setAnuncios(datos))
      .catch((err) => !cancelado && setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible cargar los anuncios. Intente nuevamente.')))
    return () => {
      cancelado = true
    }
  }, [version])

  const eliminar = async () => {
    setEnviando(true)
    setError(null)
    try {
      await api.delete(`/anuncios/${porEliminar.idAnuncio}`)
      setEliminado(porEliminar.titulo)
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'No fue posible eliminar el anuncio. Intente nuevamente.'))
    } finally {
      setPorEliminar(null)
      setEnviando(false)
      setVersion((v) => v + 1) // también si falló: otro funcionario pudo haberlo eliminado antes
    }
  }

  return (
    <section className="panel">
      <h1>Eliminar anuncios</h1>

      <div className={estilos.mensajes}>
        {eliminado && <p className="mensaje-exito" role="status">El anuncio «{eliminado}» fue eliminado y ya no aparece en la cartelera.</p>}
        {error && <p className="mensaje-error" role="alert">{error.mensaje}</p>}
      </div>

      {anuncios === null && !error && <p role="status">Cargando…</p>}
      {anuncios?.length === 0 && <p className="pendiente">Todavía no hay anuncios.</p>}

      {anuncios?.length > 0 && (
        <div className="tabla-scroll">
          <table className={`tabla ${estilos.tablaAnuncios}`}>
            <thead>
              <tr>
                <th>Título</th>
                <th>Situación</th>
                <th>Visible desde</th>
                <th>Visible hasta</th>
                <th>Publicado</th>
                <th><span className={estilos.oculto}>Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {anuncios.map((anuncio) => {
                const eliminado = anuncio.estado === 'ELIMINADO'
                const situacion = SITUACION[eliminado ? 'ELIMINADO' : anuncio.vigencia]
                return (
                  <tr key={anuncio.idAnuncio}>
                    <td className={estilos.titulo}>{anuncio.titulo}</td>
                    <td><span className={`${estilos.etiqueta} ${estilos[situacion.clase]}`}>{situacion.texto}</span></td>
                    <td>{formatearDia(anuncio.fechaInicio)}</td>
                    <td>{anuncio.fechaFin ? formatearDia(anuncio.fechaFin) : 'Sin vencimiento'}</td>
                    <td>{formatearFecha(anuncio.fechaPublicacion)}</td>
                    <td>
                      {!eliminado && <button type="button" onClick={() => { setEliminado(null); setPorEliminar(anuncio) }}>Eliminar</button>}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {porEliminar && (
        <Confirmacion
          titulo="Eliminar anuncio"
          textoConfirmar={enviando ? 'Eliminando…' : 'Eliminar'}
          ocupado={enviando}
          onConfirmar={eliminar}
          onCancelar={() => setPorEliminar(null)}
        >
          <p>Se eliminará el anuncio «{porEliminar.titulo}» y dejará de aparecer en la cartelera.</p>
          <p>Esta acción no se puede deshacer desde la aplicación.</p>
        </Confirmacion>
      )}
    </section>
  )
}
