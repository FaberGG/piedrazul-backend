package com.piedrazul.backend.agenda.internal.repository;


import com.piedrazul.backend.agenda.internal.domain.DiaNoLaboral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DiaNoLaboralRepository extends JpaRepository<DiaNoLaboral, Long> {
    boolean existsByFecha(LocalDate fecha);
    List<DiaNoLaboral> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}
