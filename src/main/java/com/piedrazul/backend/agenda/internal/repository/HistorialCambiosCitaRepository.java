package com.piedrazul.backend.agenda.internal.repository;

import com.piedrazul.backend.agenda.internal.domain.HistorialCambiosCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialCambiosCitaRepository extends JpaRepository<HistorialCambiosCita, Long> {

    /** Devuelve el historial de reagendamientos de una cita, del más reciente al más antiguo. */
    List<HistorialCambiosCita> findByCitaIdOrderByCreatedAtDesc(Long citaId);
}
