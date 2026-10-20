package com.mediturno.backend.ordenes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.ordenes.model.Orden;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    @Query("select o from Orden o join fetch o.reserva r where r.ocurrenciaTurno.idOcurrenciaTurno = :idOcurrencia")
    List<Orden> buscarPorOcurrencia(@Param("idOcurrencia") Long idOcurrencia);
}
