package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.HistorialCambiosCita;
import com.piedrazul.backend.agenda.internal.dto.HistorialCambiosCitaResponse;
import com.piedrazul.backend.agenda.internal.dto.ReagendarCitaRequest;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.agenda.internal.repository.HistorialCambiosCitaRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReagendarServiceImplTest {

    @Mock private CitaRepository citaRepository;
    @Mock private HistorialCambiosCitaRepository historialRepository;
    @Mock private DisponibilidadService disponibilidadService;
    @Mock private PacientesApi pacientesApi;
    @Mock private MedicosApi medicosApi;
    @Mock private CitaAuditoriaAdapter auditoriaAdapter;
    @Mock private CitaServiceHelper helper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ReagendarServiceImpl reagendarService;

    private Cita citaAtendida;
    private ReagendarCitaRequest request;
    private final LocalDate nuevaFecha = LocalDate.of(2026, 7, 1);
    private final LocalTime nuevaHora = LocalTime.of(10, 0);

    @BeforeEach
    void setUp() {
        citaAtendida = Cita.builder()
                .id(1L)
                .medicoId(5L)
                .pacienteId(20L)
                .fecha(LocalDate.of(2026, 6, 10))
                .hora(LocalTime.of(9, 0))
                .estado(EstadoCita.ATENDIDA)
                .build();

        request = new ReagendarCitaRequest();
        request.setNuevaFecha(nuevaFecha);
        request.setNuevaHora("10:00:00");
        request.setMotivo("Reagendamiento de prueba");
    }

    @Test
    @DisplayName("RF8 — Solo citas ATENDIDAS pueden reagendarse; PROGRAMADA debe lanzar excepción")
    void reagendar_citaEnEstadoProgramada_lanzaExcepcion() {
        Cita citaProgramada = Cita.builder()
                .id(2L)
                .medicoId(5L)
                .pacienteId(20L)
                .fecha(LocalDate.of(2026, 6, 15))
                .hora(LocalTime.of(8, 0))
                .estado(EstadoCita.PROGRAMADA)
                .build();

        when(citaRepository.findById(2L)).thenReturn(Optional.of(citaProgramada));

        assertThatThrownBy(() -> reagendarService.reagendar(2L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("atendidas");
    }

    @Test
    @DisplayName("RF8 — El historial se persiste ANTES de mutar la cita (garantía de rollback)")
    void reagendar_debeGuardarHistorialAntesDeModificarCita() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(citaAtendida));
        when(helper.parseHora("10:00:00")).thenReturn(nuevaHora);
        when(disponibilidadService.estaDisponible(5L, nuevaFecha, nuevaHora)).thenReturn(true);
        when(helper.obtenerUsuarioIdAutenticado())
                .thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(citaRepository.save(any(Cita.class))).thenReturn(citaAtendida);
        when(pacientesApi.obtenerResumenPorId(20L))
                .thenReturn(PacienteResumenDTO.builder().id(20L).nombres("Juan").apellidos("Lopez").documento("123").build());
        when(medicosApi.obtenerResumenMedico(5L))
                .thenReturn(MedicoResumenDTO.builder().id(5L).nombresCompletos("Dr. Gomez").especialidad("FISIOTERAPIA").build());

        InOrder orden = inOrder(historialRepository, citaRepository);

        reagendarService.reagendar(1L, request);

        orden.verify(historialRepository).save(any(HistorialCambiosCita.class));
        orden.verify(citaRepository).save(any(Cita.class));
    }

    @Test
    @DisplayName("RF8 — Cita reagendada queda en estado PROGRAMADA")
    void reagendar_citaAtendida_quedaEnEstadoProgramada() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(citaAtendida));
        when(helper.parseHora("10:00:00")).thenReturn(nuevaHora);
        when(disponibilidadService.estaDisponible(5L, nuevaFecha, nuevaHora)).thenReturn(true);
        when(helper.obtenerUsuarioIdAutenticado()).thenReturn(null);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pacientesApi.obtenerResumenPorId(20L))
                .thenReturn(PacienteResumenDTO.builder().id(20L).nombres("Ana").apellidos("Perez").documento("456").build());
        when(medicosApi.obtenerResumenMedico(5L))
                .thenReturn(MedicoResumenDTO.builder().id(5L).nombresCompletos("Dr. Rios").especialidad("GENERAL").build());

        reagendarService.reagendar(1L, request);

        assertThat(citaAtendida.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(citaAtendida.getFecha()).isEqualTo(nuevaFecha);
        assertThat(citaAtendida.getHora()).isEqualTo(nuevaHora);
    }

    @Test
    @DisplayName("obtenerHistorial — cita inexistente lanza ResourceNotFoundException")
    void obtenerHistorial_citaInexistente_lanzaExcepcion() {
        when(citaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reagendarService.obtenerHistorial(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerHistorial — devuelve lista ordenada por createdAt DESC")
    void obtenerHistorial_retornaListaOrdenada() {
        when(citaRepository.existsById(1L)).thenReturn(true);

        HistorialCambiosCita entrada1 = HistorialCambiosCita.builder()
                .id(1L)
                .cita(citaAtendida)
                .fechaAnterior(LocalDate.of(2026, 6, 1))
                .horaAnterior(LocalTime.of(8, 0))
                .fechaNueva(LocalDate.of(2026, 6, 10))
                .horaNueva(LocalTime.of(9, 0))
                .medicoAnteriorId(5L)
                .medicoNuevoId(5L)
                .createdAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();

        HistorialCambiosCita entrada2 = HistorialCambiosCita.builder()
                .id(2L)
                .cita(citaAtendida)
                .fechaAnterior(LocalDate.of(2026, 6, 10))
                .horaAnterior(LocalTime.of(9, 0))
                .fechaNueva(nuevaFecha)
                .horaNueva(nuevaHora)
                .medicoAnteriorId(5L)
                .medicoNuevoId(5L)
                .createdAt(LocalDateTime.of(2026, 6, 15, 12, 0))
                .build();

        when(historialRepository.findByCitaIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(entrada2, entrada1)); // más reciente primero

        List<HistorialCambiosCitaResponse> resultado = reagendarService.obtenerHistorial(1L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getId()).isEqualTo(2L);
        assertThat(resultado.get(1).getId()).isEqualTo(1L);
    }
}
