package com.piedrazul.backend.agenda.internal.repository;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.TipoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByMedicoIdAndFecha(Long medicoId, LocalDate fecha);

    long countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(
            Long pacienteId, EstadoCita estado, LocalDate fecha);

    boolean existsByPacienteIdAndEstadoInAndFechaGreaterThanEqual(
            Long pacienteId, List<EstadoCita> estados, LocalDate fecha);

    boolean existsByMedicoIdAndFechaAndHoraAndEstadoNot(
            Long medicoId, LocalDate fecha, LocalTime hora, EstadoCita estado);

    List<Cita> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Cita> findByFechaOrderByMedicoIdAscHoraAsc(LocalDate fecha);

    @Query("SELECT c.estado, COUNT(c) FROM Cita c " +
           "WHERE c.fecha BETWEEN :desde AND :hasta " +
           "GROUP BY c.estado")
    List<Object[]> countByEstadoBetweenFechas(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    boolean existsByPacienteIdAndIdLessThan(Long pacienteId, Long citaId);

    boolean existsByPacienteIdAndTipoCitaAndEstado(Long pacienteId, TipoCita tipoCita, EstadoCita estado);

    List<Cita> findByPacienteIdOrderByFechaDesc(Long pacienteId);
}
