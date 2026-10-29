package com.mediturno.backend.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.usuarios.model.Administrador;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    /** Cuántos administradores están en ese estado (para no dejar el sistema sin ninguno activo). */
    long countByEstado(EstadoActivacion estado);
}
