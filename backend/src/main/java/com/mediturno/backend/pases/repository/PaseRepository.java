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
    List<Pase> findByPacienteIdUsuarioAndEspecialidadDestinoCodigoAndPaseEstadoAndFechaEmisionBetweenOrderByFechaEmisionAsc(
            Long paciente, String especialidad, EstadoPase estado,
            LocalDateTime desde, LocalDateTime hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pase p where p.idPase = :id")
    Optional<Pase> bloquearPorId(@Param("id") Long id);
}
