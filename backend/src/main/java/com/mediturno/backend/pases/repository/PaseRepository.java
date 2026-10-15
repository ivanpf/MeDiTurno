package com.mediturno.backend.pases.repository;

import com.mediturno.backend.pases.model.EstadoPase;
import com.mediturno.backend.pases.model.Pase;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaseRepository extends JpaRepository<Pase, Long> {
    /** Pases del paciente para esa especialidad, en ese estado y emitidos entre las dos fechas, del más antiguo al más nuevo. */
    @Query("""
        select p from Pase p
        where p.paciente.idUsuario = :idPaciente
          and p.especialidadDestino.codigo = :codigoEspecialidad
          and p.paseEstado = :estado
          and p.fechaEmision between :desde and :hasta
        order by p.fechaEmision
        """)
    List<Pase> buscarPorPacienteEspecialidadYEstado(
            @Param("idPaciente") Long idPaciente, @Param("codigoEspecialidad") String codigoEspecialidad,
            @Param("estado") EstadoPase estado, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Busca el pase y lo bloquea hasta el fin de la transacción (SELECT ... FOR UPDATE): un pase no se vincula a dos reservas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pase p where p.idPase = :id")
    Optional<Pase> buscarConLock(@Param("id") Long id);
}
