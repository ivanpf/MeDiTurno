package com.mediturno.backend.notificaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.notificaciones.model.CanalNotificacion;
import com.mediturno.backend.notificaciones.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Las últimas 50 notificaciones de una persona en un canal, la más nueva primero. */
    List<Notificacion> findTop50ByUsuarioIdUsuarioAndCanalOrderByFechaDescIdNotificacionDesc(Long idUsuario, CanalNotificacion canal);
}
