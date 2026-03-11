package com.piedrazul.backend.agenda.repository;

import com.piedrazul.backend.agenda.domain.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio JPA para la entidad Cita.
 */
@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /**
     * Lista citas de un médico en una fecha determinada.
     */
    List<Cita> findByMedicoIdAndFecha(Long medicoId, LocalDate fecha);

    /**
     * Cuenta citas futuras activas de un paciente.
     */
    long countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(
            Long pacienteId, String estado, LocalDate fecha);
}

