export const API_BASE_URL = 'http://10.0.2.2:8080';

export const listarReservas = async () => {
  const response = await fetch(`${API_BASE_URL}/reservas`);
  if (!response.ok) throw new Error('Error al listar reservas');
  return response.json();
};

export const buscarReserva = async (id) => {
  const response = await fetch(`${API_BASE_URL}/reservas/${id}`);
  if (!response.ok) throw new Error('Reserva no encontrada');
  return response.json();
};

export const crearReserva = async (reserva) => {
  const response = await fetch(`${API_BASE_URL}/reservas`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(reserva),
  });
  if (!response.ok) throw new Error('Error al crear reserva');
  return response.json();
};

export const actualizarReserva = async (id, reserva) => {
  const response = await fetch(`${API_BASE_URL}/reservas/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(reserva),
  });
  if (!response.ok) throw new Error('Error al actualizar reserva');
  return response.json();
};

export const eliminarReserva = async (id) => {
  const response = await fetch(`${API_BASE_URL}/reservas/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Error al eliminar reserva');
};
