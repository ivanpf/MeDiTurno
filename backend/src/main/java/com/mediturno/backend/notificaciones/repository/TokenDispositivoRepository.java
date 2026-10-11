package com.mediturno.backend.notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.notificaciones.model.TokenDispositivo;

public interface TokenDispositivoRepository extends JpaRepository<TokenDispositivo, Long> {
}
