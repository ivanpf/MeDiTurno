// Pantalla temporal de un caso de uso que todavía no se implementó.
// Al desarrollar el caso de uso, reemplazar el contenido del componente que la usa.
export default function PaginaPendiente({ cu, titulo, archivo }) {
  return (
    <section className="panel">
      <h1>{titulo}</h1>
      <p className="pendiente">
        <strong>{cu}</strong> — pendiente de implementar.
        {archivo && <> Se desarrolla en <code>{archivo}</code>.</>}
      </p>
    </section>
  )
}
