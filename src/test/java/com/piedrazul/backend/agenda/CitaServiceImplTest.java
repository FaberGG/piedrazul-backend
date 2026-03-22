package com.piedrazul.backend.agenda;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.agenda.internal.service.CitaServiceImpl;
import com.piedrazul.backend.agenda.internal.service.DisponibilidadService;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.domain.Medico;
import com.piedrazul.backend.medicos.repository.MedicosRepository;
import com.piedrazul.backend.shared.audit.AuditService;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para RF1 — Listar agenda de un médico por fecha.
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    @Mock private CitaRepository citaRepository;
    @Mock private DisponibilidadService disponibilidadService;
    @Mock private AuditService auditService;
    @Mock private MedicosApi medicosApi;
    @Mock private MedicosRepository medicosRepository;

    @InjectMocks
    private CitaServiceImpl citaService;

    private Medico medicoActivo;
    private HorarioAtencionDTO horarioEstandar;
    private LocalDate fechaLunes;

    @BeforeEach
    void setUp() {
        medicoActivo = Medico.builder()
                .id(1L)
                .nombres("Clara Inés")
                .apellidos("Córdoba")
                .especialidad("TERAPIA_NEURAL")
                .estado("ACTIVO")
                .build();

        fechaLunes = LocalDate.of(2026, 3, 23); // Lunes

        horarioEstandar = HorarioAtencionDTO.builder()
                .horaInicio(LocalTime.of(7, 0))
                .horaFin(LocalTime.of(12, 0))
                .intervaloMinutos(15)
                .diasAtencion(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("RF1 — Debe retornar agenda con citas y slots disponibles correctamente")
    void listarAgendaMedico_debeRetornarAgendaCompleta() {
        // ARRANGE
        Cita cita1 = Cita.builder()
                .id(101L)
                .pacienteId(10L)
                .medicoId(1L)
                .fecha(fechaLunes)
                .hora(LocalTime.of(7, 0))
                .estado("PROGRAMADA")
                .build();

        when(medicosRepository.findById(1L)).thenReturn(Optional.of(medicoActivo));
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(citaRepository.findByMedicoIdAndFecha(1L, fechaLunes)).thenReturn(List.of(cita1));
        when(disponibilidadService.calcularHorariosDisponibles(1L, fechaLunes))
                .thenReturn(List.of(LocalTime.of(7, 15), LocalTime.of(7, 30)));

        // ACT
        AgendaResponse respuesta = citaService.listarAgendaMedico(1L, fechaLunes);

        // ASSERT
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getMedicoId()).isEqualTo(1L);
        assertThat(respuesta.getMedicoNombre()).isEqualTo("Clara Inés Córdoba");
        assertThat(respuesta.getEspecialidad()).isEqualTo("TERAPIA_NEURAL");
        assertThat(respuesta.getFecha()).isEqualTo(fechaLunes);
        assertThat(respuesta.getCitas()).hasSize(1);
        assertThat(respuesta.getHorariosDisponibles()).hasSize(2);
        assertThat(respuesta.getTotalSlots()).isEqualTo(20); // 300 min / 15 = 20
        assertThat(respuesta.getSlotsOcupados()).isEqualTo(1);
        assertThat(respuesta.getPorcentajeOcupacion()).isEqualTo(5.0); // 1/20 * 100
    }

    @Test
    @DisplayName("RF1 — Debe lanzar excepción si el médico no existe")
    void listarAgendaMedico_debeLanzarExcepcionSiMedicoNoExiste() {
        // ARRANGE
        when(medicosRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> citaService.listarAgendaMedico(99L, fechaLunes))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RF1 — Debe retornar 0% ocupación si no hay citas en el día")
    void listarAgendaMedico_sinCitasDebeMostrarCeroOcupacion() {
        // ARRANGE
        when(medicosRepository.findById(1L)).thenReturn(Optional.of(medicoActivo));
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(citaRepository.findByMedicoIdAndFecha(1L, fechaLunes)).thenReturn(List.of());
        when(disponibilidadService.calcularHorariosDisponibles(1L, fechaLunes))
                .thenReturn(List.of(LocalTime.of(7, 0), LocalTime.of(7, 15)));

        // ACT
        AgendaResponse respuesta = citaService.listarAgendaMedico(1L, fechaLunes);

        // ASSERT
        assertThat(respuesta.getCitas()).isEmpty();
        assertThat(respuesta.getSlotsOcupados()).isZero();
        assertThat(respuesta.getPorcentajeOcupacion()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("RF1 — Las citas CANCELADAS no deben contar como ocupadas")
    void listarAgendaMedico_citasCanceladasNoContanComoOcupadas() {
        // ARRANGE
        Cita citaCancelada = Cita.builder()
                .id(200L)
                .pacienteId(20L)
                .medicoId(1L)
                .fecha(fechaLunes)
                .hora(LocalTime.of(7, 0))
                .estado("CANCELADA")
                .build();

        when(medicosRepository.findById(1L)).thenReturn(Optional.of(medicoActivo));
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(citaRepository.findByMedicoIdAndFecha(1L, fechaLunes)).thenReturn(List.of(citaCancelada));
        when(disponibilidadService.calcularHorariosDisponibles(1L, fechaLunes))
                .thenReturn(List.of(LocalTime.of(7, 0)));

        // ACT
        AgendaResponse respuesta = citaService.listarAgendaMedico(1L, fechaLunes);

        // ASSERT
        assertThat(respuesta.getSlotsOcupados()).isZero(); // La cancelada no cuenta
    }
}
