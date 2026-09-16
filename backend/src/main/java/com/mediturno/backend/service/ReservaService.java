package com.mediturno.backend.service;

import com.mediturno.backend.model.Reserva;
import com.mediturno.backend.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReservaService {

    private static final Logger logger = LoggerFactory.getLogger(ReservaService.class);

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private NotificationService notificationService;

    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    public Optional<Reserva> buscarPorId(Long id) {
        return reservaRepository.findById(id);
    }

    public Reserva guardar(Reserva reserva) {
        return reservaRepository.save(reserva);
    }

    public Reserva actualizar(Long id, Reserva reservaActualizada) {
        return reservaRepository.findById(id)
            .map(reserva -> {
                String estadoAnterior = reserva.getEstado();
                reserva.setNombreCliente(reservaActualizada.getNombreCliente());
                reserva.setFecha(reservaActualizada.getFecha());
                reserva.setHora(reservaActualizada.getHora());
                reserva.setCantidadPersonas(reservaActualizada.getCantidadPersonas());
                reserva.setEstado(reservaActualizada.getEstado());
                Reserva saved = reservaRepository.save(reserva);

                if (estadoAnterior != null && !estadoAnterior.equals(saved.getEstado())) {
                    enviarNotificacionCambioEstado(saved, estadoAnterior);
                }

                return saved;
            })
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));
    }

    public void eliminar(Long id) {
        reservaRepository.deleteById(id);
    }

    private void enviarNotificacionCambioEstado(Reserva reserva, String estadoAnterior) {
        try {
            String titulo;
            String mensaje;

            switch (reserva.getEstado()) {
                case "CONFIRMADA":
                    titulo = "Reserva Confirmada";
                    mensaje = "Tu reserva #" + reserva.getId() + " para " + reserva.getFecha()
                            + " a las " + reserva.getHora() + " fue confirmada.";
                    break;
                case "CANCELADA":
                    titulo = "Reserva Cancelada";
                    mensaje = "Tu reserva #" + reserva.getId() + " fue cancelada.";
                    break;
                case "PENDIENTE":
                    titulo = "Reserva en Pendiente";
                    mensaje = "Tu reserva #" + reserva.getId() + " está pendiente de confirmación.";
                    break;
                default:
                    titulo = "Reserva Actualizada";
                    mensaje = "Tu reserva #" + reserva.getId() + " cambió de estado: "
                            + estadoAnterior + " → " + reserva.getEstado();
            }

            Map<String, Object> data = Map.of(
                "reservaId", String.valueOf(reserva.getId()),
                "estado", reserva.getEstado(),
                "fecha", String.valueOf(reserva.getFecha()),
                "hora", String.valueOf(reserva.getHora())
            );

            notificationService.sendPushNotification(titulo, mensaje, data);
            logger.info("Notificación enviada para reserva #{}: {}", reserva.getId(), reserva.getEstado());
        } catch (Exception e) {
            logger.error("Error enviando notificación para reserva #{}", reserva.getId(), e);
        }
    }
}
