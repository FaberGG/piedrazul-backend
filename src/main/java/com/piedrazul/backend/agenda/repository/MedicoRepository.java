package com.piedrazul.backend.agenda.repository;

import com.piedrazul.backend.agenda.domain.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Medico.
 */
@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    List<Medico> findByEstado(String estado);

    List<Medico> findByEspecialidad(String especialidad);
}

