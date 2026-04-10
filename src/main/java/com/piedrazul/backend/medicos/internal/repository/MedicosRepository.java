package com.piedrazul.backend.medicos.internal.repository;

import com.piedrazul.backend.medicos.internal.domain.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicosRepository extends JpaRepository<Medico, Long> {
    boolean existsByUsuarioId(Long usuarioId);

    List<Medico> findByEstadoIgnoreCase(String estado);

    List<Medico> findByEstadoIgnoreCaseAndEspecialidadIgnoreCase(String estado, String especialidad);
}
