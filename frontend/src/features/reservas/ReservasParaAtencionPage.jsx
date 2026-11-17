import ListadoReservas from './ListadoReservas'

export default function ReservasParaAtencionPage() {
  return <section className="panel"><h1>Reservas para atención</h1><p>Consulte las reservas por fecha o turno para organizar la atención de ventanilla. La búsqueda inicial muestra las de hoy.</p><ListadoReservas funcionario /></section>
}
