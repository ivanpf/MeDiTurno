package com.mediturno.backend.usuarios.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.usuarios.model.Medico;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    /**
     * Busca al médico y lo bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): quien lo pida después espera.
     * Así dos altas de turno del mismo médico no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.idUsuario = :id")
    Optional<Medico> buscarConLock(@Param("id") Long id);

    /** Los médicos en ese estado con sus especialidades (en una sola consulta), ordenados por apellido y nombre. */
    @Query("select distinct m from Medico m left join fetch m.especialidades where m.estado = :estado order by m.apellido, m.nombre")
    List<Medico> listarConEspecialidades(@Param("estado") EstadoActivacion estado);
}
