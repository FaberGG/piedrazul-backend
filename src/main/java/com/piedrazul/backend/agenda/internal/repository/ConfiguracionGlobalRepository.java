package com.piedrazul.backend.agenda.internal.repository;

import com.piedrazul.backend.agenda.internal.domain.ConfiguracionGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionGlobalRepository extends JpaRepository<ConfiguracionGlobal, Long> {
    Optional<ConfiguracionGlobal> findByClave(String clave);
}