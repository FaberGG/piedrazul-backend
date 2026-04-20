package com.piedrazul.backend.agenda.internal.repository;

import com.piedrazul.backend.agenda.internal.domain.AgendaDiaLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AgendaDiaLockRepository extends JpaRepository<AgendaDiaLock, Long> {

    Optional<AgendaDiaLock> findByMedicoIdAndFecha(Long medicoId, LocalDate fecha);
}

