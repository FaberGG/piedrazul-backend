package com.piedrazul.backend.pacientes.internal.repository;



import com.piedrazul.backend.pacientes.internal.domain.Paciente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacientesRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    Optional<Paciente> findByUsuarioId(UUID usuarioId);

    List<Paciente> findByDocumentoStartingWithOrderByDocumentoAsc(String documento, Pageable pageable);
}