package com.piedrazul.backend.agenda.internal.repository;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.TipoCita;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración — capa JPA.
 * Verifica las consultas derivadas del dominio directamente contra H2.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class CitaRepositoryIntegrationTest {

    // Previene que Spring Boot intente conectarse a Keycloak para las JWK keys
    @MockitoBean JwtDecoder jwtDecoder;

    @Autowired CitaRepository citaRepository;

    private static final Long MEDICO_ID   = 1L;
    private static final Long PACIENTE_ID = 10L;
    private static final LocalDate FECHA  = LocalDate.of(2026, 7, 15);

    @BeforeEach
    void setUp() {
        // Cita PROGRAMADA (ocupa el slot 09:00)
        guardar(MEDICO_ID, PACIENTE_ID, FECHA, LocalTime.of(9, 0),
                EstadoCita.PROGRAMADA, TipoCita.CONSULTA_GENERAL);
        // Segunda cita PROGRAMADA del mismo paciente en otro día
        guardar(MEDICO_ID, PACIENTE_ID, FECHA.plusDays(1), LocalTime.of(10, 0),
                EstadoCita.PROGRAMADA, TipoCita.CONSULTA_GENERAL);
        // Consulta General ya ATENDIDA (habilita acceso a especialidades)
        guardar(MEDICO_ID, PACIENTE_ID, FECHA.minusDays(10), LocalTime.of(8, 0),
                EstadoCita.ATENDIDA, TipoCita.CONSULTA_GENERAL);
        // Cita CANCELADA (no debe bloquear slot ni contar como cita activa)
        guardar(MEDICO_ID, 99L, FECHA, LocalTime.of(11, 0),
                EstadoCita.CANCELADA, TipoCita.CONSULTA_GENERAL);
    }

    @Test
    @DisplayName("Guarda doble cita — detecta colisión en (medico, fecha, hora) no cancelada")
    void existsByMedicoFechaHoraNotCancelada_slotOcupado_retornaTrue() {
        boolean ocupado = citaRepository.existsByMedicoIdAndFechaAndHoraAndEstadoNot(
                MEDICO_ID, FECHA, LocalTime.of(9, 0), EstadoCita.CANCELADA);
        assertThat(ocupado).isTrue();
    }

    @Test
    @DisplayName("Slot con cita CANCELADA no bloquea el horario")
    void existsByMedicoFechaHoraNotCancelada_citaCancelada_retornaFalse() {
        boolean ocupado = citaRepository.existsByMedicoIdAndFechaAndHoraAndEstadoNot(
                MEDICO_ID, FECHA, LocalTime.of(11, 0), EstadoCita.CANCELADA);
        assertThat(ocupado).isFalse();
    }

    @Test
    @DisplayName("Regla consulta previa — paciente con CONSULTA_GENERAL ATENDIDA retorna true")
    void existsByPacienteIdTipoCitaEstado_consultaAtendida_retornaTrue() {
        boolean tiene = citaRepository.existsByPacienteIdAndTipoCitaAndEstado(
                PACIENTE_ID, TipoCita.CONSULTA_GENERAL, EstadoCita.ATENDIDA);
        assertThat(tiene).isTrue();
    }

    @Test
    @DisplayName("Regla consulta previa — paciente sin consulta atendida retorna false")
    void existsByPacienteIdTipoCitaEstado_sinConsulta_retornaFalse() {
        boolean tiene = citaRepository.existsByPacienteIdAndTipoCitaAndEstado(
                99L, TipoCita.CONSULTA_GENERAL, EstadoCita.ATENDIDA);
        assertThat(tiene).isFalse();
    }

    @Test
    @DisplayName("Límite de 3 citas — cuenta las citas activas (no canceladas) futuras o actuales")
    void countActivasFuturas_contaCitasNoCanceladas() {
        long total = citaRepository.countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(
                PACIENTE_ID, EstadoCita.CANCELADA, LocalDate.now());
        // PROGRAMADA en FECHA y FECHA+1 son ≥ hoy; ATENDIDA en FECHA-10 puede variar
        assertThat(total).isGreaterThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("'Primera cita' por ID — paciente con citas previas retorna true")
    void existsByPacienteIdAndIdLessThan_hayAnteriores_retornaTrue() {
        List<Cita> citas = citaRepository.findByPacienteIdOrderByFechaDesc(PACIENTE_ID);
        assertThat(citas).hasSizeGreaterThan(1);

        Long idMaximo = citas.stream().mapToLong(Cita::getId).max().orElseThrow();
        boolean tieneAnteriores = citaRepository.existsByPacienteIdAndIdLessThan(PACIENTE_ID, idMaximo);
        assertThat(tieneAnteriores).isTrue();
    }

    @Test
    @DisplayName("findByMedicoIdAndFecha retorna todas las citas de ese médico en esa fecha")
    void findByMedicoIdAndFecha_retornaCitasDelDia() {
        List<Cita> resultado = citaRepository.findByMedicoIdAndFecha(MEDICO_ID, FECHA);
        // PROGRAMADA(9:00) y CANCELADA(11:00) son del mismo médico y fecha
        assertThat(resultado).hasSize(2);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void guardar(Long medicoId, Long pacienteId, LocalDate fecha,
                         LocalTime hora, EstadoCita estado, TipoCita tipo) {
        citaRepository.saveAndFlush(Cita.builder()
                .medicoId(medicoId)
                .pacienteId(pacienteId)
                .fecha(fecha)
                .hora(hora)
                .estado(estado)
                .tipoCita(tipo)
                .build());
    }
}
