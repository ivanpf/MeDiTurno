package com.mediturno.backend.notificaciones.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.notificaciones.model.TokenDispositivo;

public interface TokenDispositivoRepository extends JpaRepository<TokenDispositivo, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO token_dispositivo (id_usuario, token, fecha_registro)
        VALUES (:idUsuario, :token, :fechaRegistro)
        ON CONFLICT (token) DO UPDATE
        SET id_usuario = EXCLUDED.id_usuario, fecha_registro = EXCLUDED.fecha_registro
        """, nativeQuery = true)
    void registrarOActualizar(@Param("idUsuario") Long idUsuario, @Param("token") String token,
            @Param("fechaRegistro") LocalDateTime fechaRegistro);

    @Query("select t.token from TokenDispositivo t where t.usuario.idUsuario = :idUsuario")
    List<String> buscarTokensPorUsuario(@Param("idUsuario") Long idUsuario);

    @Modifying
    @Transactional
    @Query("delete from TokenDispositivo t where t.usuario.idUsuario = :idUsuario")
    int deleteByUsuarioIdUsuario(@Param("idUsuario") Long idUsuario);

    @Modifying
    @Transactional
    @Query("delete from TokenDispositivo t where t.token in :tokens")
    int deleteByTokenIn(@Param("tokens") List<String> tokens);
}
