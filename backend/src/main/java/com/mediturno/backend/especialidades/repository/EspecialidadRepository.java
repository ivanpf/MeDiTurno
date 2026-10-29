package com.mediturno.backend.especialidades.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.especialidades.model.Especialidad;

public interface EspecialidadRepository extends JpaRepository<Especialidad, String>, JpaSpecificationExecutor<Especialidad> {

    boolean existsByNombreIgnoreCase(String nombre);

    /** Hay otra especialidad (de código distinto) con ese nombre. */
    boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, String codigo);

    /**
     * Busca la especialidad y la bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): dos modificaciones
     * simultáneas de la misma especialidad no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Especialidad e where e.codigo = :codigo")
    Optional<Especialidad> buscarConLock(@Param("codigo") String codigo);
}
