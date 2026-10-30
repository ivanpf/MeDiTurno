package com.mediturno.backend.pacientes.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.pacientes.model.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long>, JpaSpecificationExecutor<Paciente> {

    Optional<Paciente> findByCedula(String cedula);

    /**
     * Busca el paciente y lo bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): dos administradores que lo
     * modifican a la vez no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paciente p where p.idUsuario = :id")
    Optional<Paciente> buscarConLock(@Param("id") Long id);
}
