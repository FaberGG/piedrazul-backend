package com.piedrazul.backend.agenda.internal.repository;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repositorio JPA para la entidad Cita.
 */
@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /** Lista citas de un médico en una fecha determinada (RF1). */
    List<Cita> findByMedicoIdAndFecha(Long medicoId, LocalDate fecha);

    /** Cuenta citas futuras activas de un paciente (RF3 — límite de 3 citas). */
    long countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(
            Long pacienteId, String estado, LocalDate fecha);

    /** Valida si el medico ya tiene una cita activa en un slot puntual. */
    boolean existsByMedicoIdAndFechaAndHoraAndEstadoNot(
            Long medicoId, LocalDate fecha, LocalTime hora, String estado);

    /** Lista todas las citas en un rango de fechas (usado por AgendaFacade para reportes). */
    List<Cita> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /**
     * Cuenta citas en un rango de fechas agrupadas por estado.
     * Usado por AgendaFacade.obtenerResumenCitas() → módulo reportes.
     */
    @Query("SELECT c.estado, COUNT(c) FROM Cita c " +
           "WHERE c.fecha BETWEEN :desde AND :hasta " +
           "GROUP BY c.estado")
    List<Object[]> countByEstadoBetweenFechas(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}

