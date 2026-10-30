package com.mediturno.backend.notificaciones.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.notificaciones.model.CanalNotificacion;
import com.mediturno.backend.notificaciones.model.EstadoNotificacion;
import com.mediturno.backend.notificaciones.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Las últimas 50 notificaciones de una persona en un canal, la más nueva primero. */
    List<Notificacion> findTop50ByUsuarioIdUsuarioAndCanalOrderByFechaDescIdNotificacionDesc(Long idUsuario, CanalNotificacion canal);

    /** Deja registrado cómo salió un aviso (se usa desde un hilo aparte, sin transacción previa). */
    @Transactional
    @Modifying
    @Query("update Notificacion n set n.estado = :estado, n.fechaEnvio = :fechaEnvio where n.idNotificacion = :id")
    void actualizarEstado(@Param("id") Long id, @Param("estado") EstadoNotificacion estado, @Param("fechaEnvio") LocalDateTime fechaEnvio);
}
