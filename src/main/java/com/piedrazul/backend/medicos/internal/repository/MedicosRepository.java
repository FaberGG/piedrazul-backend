package com.piedrazul.backend.medicos.internal.repository;

import com.piedrazul.backend.medicos.internal.domain.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicosRepository extends JpaRepository<Medico, Long> {
    boolean existsByUsuarioId(UUID usuarioId);

    Optional<Medico> findByUsuarioId(UUID usuarioId);

    List<Medico> findByEstadoIgnoreCase(String estado);

    List<Medico> findByEstadoIgnoreCaseAndEspecialidadIgnoreCase(String estado, String especialidad);
}
