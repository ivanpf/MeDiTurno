package com.mediturno.backend.consultorios.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.consultorios.model.Consultorio;

public interface ConsultorioRepository extends JpaRepository<Consultorio, String>, JpaSpecificationExecutor<Consultorio> {

    /**
     * Busca el consultorio y lo bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): quien lo pida después espera.
     * Así dos altas de turno en el mismo consultorio no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consultorio c where c.codigo = :codigo")
    Optional<Consultorio> buscarConLock(@Param("codigo") String codigo);
}
