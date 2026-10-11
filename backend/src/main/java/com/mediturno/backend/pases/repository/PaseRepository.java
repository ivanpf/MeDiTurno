package com.mediturno.backend.pases.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.pases.model.Pase;

public interface PaseRepository extends JpaRepository<Pase, Long> {
}
