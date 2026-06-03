package com.piedrazul.backend.pacientes.internal.repository;



import com.piedrazul.backend.pacientes.internal.domain.Paciente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacientesRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    Optional<Paciente> findByUsuarioId(UUID usuarioId);

    Optional<Paciente> findByKeycloakId(String keycloakId);

    @Modifying
    @Query(value = """
            UPDATE pacientes p
            SET keycloak_id = u.keycloak_id
            FROM usuarios u
            WHERE p.usuario_id = u.id
              AND p.keycloak_id IS NULL
              AND u.keycloak_id IS NOT NULL
            """, nativeQuery = true)
    int backfillKeycloakIds();

    List<Paciente> findByDocumentoStartingWithOrderByDocumentoAsc(String documento, Pageable pageable);
}