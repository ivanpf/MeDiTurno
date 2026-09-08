package com.mediturno.backend.service;

import com.mediturno.backend.model.Reserva;
import com.mediturno.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

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
                reserva.setNombreCliente(reservaActualizada.getNombreCliente());
                reserva.setFecha(reservaActualizada.getFecha());
                reserva.setHora(reservaActualizada.getHora());
                reserva.setCantidadPersonas(reservaActualizada.getCantidadPersonas());
                reserva.setEstado(reservaActualizada.getEstado());
                return reservaRepository.save(reserva);
            })
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));
    }

    public void eliminar(Long id) {
        reservaRepository.deleteById(id);
    }
}
