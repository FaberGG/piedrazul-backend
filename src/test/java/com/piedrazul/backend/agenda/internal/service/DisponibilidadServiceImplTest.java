package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.TipoCita;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceImplTest {

    @Mock
    private MedicosApi medicosApi;

    @Mock
    private CitaRepository citaRepository;

    @InjectMocks
    private DisponibilidadServiceImpl disponibilidadService;

    private Long medicoId;
    private LocalDate fecha;
    private HorarioAtencionDTO horario30;

    @BeforeEach
    void setUp() {
        medicoId = 1L;
        fecha = LocalDate.of(2026, 3, 23); // Monday

        horario30 = HorarioAtencionDTO.builder()
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(11, 0))
                .intervaloMinutos(30)
                .diasAtencion(List.of(DayOfWeek.MONDAY))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("No debe listar 9:30 como disponible cuando no cabe cita estandar completa")
    void calcularHorariosDisponibles_noDebeListarHuecoInutilizableTrasPrioridad() {
        List<Cita> citas = List.of(
                Cita.builder().medicoId(medicoId).fecha(fecha).hora(LocalTime.of(9, 0)).duracionMinutos(15).estado(EstadoCita.PROGRAMADA).tipoCita(TipoCita.ESTANDAR).build(),
                Cita.builder().medicoId(medicoId).fecha(fecha).hora(LocalTime.of(9, 15)).duracionMinutos(5).estado(EstadoCita.PROGRAMADA).tipoCita(TipoCita.PRIORIDAD).build(),
                Cita.builder().medicoId(medicoId).fecha(fecha).hora(LocalTime.of(9, 45)).duracionMinutos(30).estado(EstadoCita.PROGRAMADA).tipoCita(TipoCita.ESTANDAR).build()
        );

        when(medicosApi.obtenerHorarioAtencion(medicoId)).thenReturn(horario30);
        when(citaRepository.findByMedicoIdAndFecha(medicoId, fecha)).thenReturn(citas);

        List<LocalTime> disponibles = disponibilidadService.calcularHorariosDisponibles(medicoId, fecha);

        assertThat(disponibles).containsExactly(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("estaDisponible debe retornar false para 9:30 si se solapa por duracion")
    void estaDisponible_debeRetornarFalseEnSlotQueSeSolapa() {
        List<Cita> citas = List.of(
                Cita.builder().medicoId(medicoId).fecha(fecha).hora(LocalTime.of(9, 45)).duracionMinutos(30).estado(EstadoCita.PROGRAMADA).tipoCita(TipoCita.ESTANDAR).build()
        );

        when(medicosApi.obtenerHorarioAtencion(medicoId)).thenReturn(horario30);
        when(citaRepository.findByMedicoIdAndFecha(medicoId, fecha)).thenReturn(citas);

        boolean disponible = disponibilidadService.estaDisponible(medicoId, fecha, LocalTime.of(9, 30));

        assertThat(disponible).isFalse();
    }

    @Test
    @DisplayName("estaDisponible debe retornar true cuando el rango completo no se solapa")
    void estaDisponible_debeRetornarTrueEnSlotValido() {
        List<Cita> citas = List.of(
                Cita.builder().medicoId(medicoId).fecha(fecha).hora(LocalTime.of(9, 45)).duracionMinutos(30).estado(EstadoCita.PROGRAMADA).tipoCita(TipoCita.ESTANDAR).build()
        );

        when(medicosApi.obtenerHorarioAtencion(medicoId)).thenReturn(horario30);
        when(citaRepository.findByMedicoIdAndFecha(medicoId, fecha)).thenReturn(citas);

        boolean disponible = disponibilidadService.estaDisponible(medicoId, fecha, LocalTime.of(10, 30));

        assertThat(disponible).isTrue();
    }
}

