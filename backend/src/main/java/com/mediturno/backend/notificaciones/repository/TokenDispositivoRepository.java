package com.mediturno.backend.notificaciones.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.notificaciones.model.TokenDispositivo;

public interface TokenDispositivoRepository extends JpaRepository<TokenDispositivo, Long> {

    Optional<TokenDispositivo> findByToken(String token);

    List<TokenDispositivo> findByUsuarioIdUsuario(Long idUsuario);

    boolean existsByUsuarioIdUsuario(Long idUsuario);

    @Transactional
    void deleteByUsuarioIdUsuario(Long idUsuario);

    @Transactional
    void deleteByToken(String token);
}
