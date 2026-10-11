package com.mediturno.backend.notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.notificaciones.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
