package com.piedrazul.backend.pacientes.repository;



import com.piedrazul.backend.pacientes.domain.Paciente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacientesRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    List<Paciente> findByDocumentoStartingWithOrderByDocumentoAsc(String documento, Pageable pageable);
}