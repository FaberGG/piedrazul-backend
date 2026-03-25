package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.dto.PrimerHorarioDisponibleResponse;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.shared.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplPrimerHorarioTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private DisponibilidadService disponibilidadService;
    @Mock
    private PacientesApi pacientesApi;
    @Mock
    private MedicosApi medicosApi;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private CitaServiceImpl citaService;

    @Test
    void obtienePrimerHorarioDisponiblePorMedico() {
        Long medicoId = 10L;
        LocalDate desde = LocalDate.now().plusDays(1);

        MedicoResumenDTO medico = MedicoResumenDTO.builder()
                .id(medicoId)
                .nombresCompletos("Medico Uno")
                .especialidad("FISIOTERAPIA")
                .activo(true)
                .build();

        when(medicosApi.obtenerResumenMedico(medicoId)).thenReturn(medico);
        when(medicosApi.obtenerHorarioAtencion(medicoId)).thenReturn(
                HorarioAtencionDTO.builder().intervaloMinutos(20).build()
        );
        when(disponibilidadService.calcularHorariosDisponibles(medicoId, desde))
                .thenReturn(List.of(LocalTime.of(10, 0), LocalTime.of(10, 20)));

        PrimerHorarioDisponibleResponse response = citaService.obtenerPrimerHorarioDisponibleMedico(medicoId, desde);

        assertEquals(medicoId, response.getMedicoId());
        assertEquals(desde, response.getFecha());
        assertEquals(LocalTime.of(10, 0), response.getHora());
        assertEquals(20, response.getIntervaloMinutos());
    }

    @Test
    void obtienePrimerHorarioDisponibleGlobal() {
        LocalDate desde = LocalDate.now().plusDays(1);
        LocalDate diaSiguiente = desde.plusDays(1);

        MedicoResumenDTO medico1 = MedicoResumenDTO.builder()
                .id(1L)
                .nombresCompletos("Medico Uno")
                .especialidad("QUIROPRAXIA")
                .activo(true)
                .build();
        MedicoResumenDTO medico2 = MedicoResumenDTO.builder()
                .id(2L)
                .nombresCompletos("Medico Dos")
                .especialidad("TERAPIA_NEURAL")
                .activo(true)
                .build();

        when(medicosApi.listarMedicosActivos()).thenReturn(List.of(medico1, medico2));
        when(medicosApi.obtenerHorarioAtencion(any(Long.class)))
                .thenReturn(HorarioAtencionDTO.builder().intervaloMinutos(15).build());

        when(disponibilidadService.calcularHorariosDisponibles(1L, desde)).thenReturn(List.of());
        when(disponibilidadService.calcularHorariosDisponibles(1L, diaSiguiente))
                .thenReturn(List.of(LocalTime.of(9, 0)));

        when(disponibilidadService.calcularHorariosDisponibles(2L, desde))
                .thenReturn(List.of(LocalTime.of(11, 0)));

        PrimerHorarioDisponibleResponse response = citaService.obtenerPrimerHorarioDisponibleGlobal(desde);

        assertEquals(2L, response.getMedicoId());
        assertEquals(desde, response.getFecha());
        assertEquals(LocalTime.of(11, 0), response.getHora());
        assertEquals("Medico Dos", response.getMedicoNombre());
    }

    @Test
    void obtenerPrimerHorarioMedico_conDesdeNulo_empiezaDesdeManana() {
        Long medicoId = 10L;
        LocalDate manana = LocalDate.now().plusDays(1);

        MedicoResumenDTO medico = MedicoResumenDTO.builder()
                .id(medicoId)
                .nombresCompletos("Medico Uno")
                .especialidad("FISIOTERAPIA")
                .activo(true)
                .build();

        when(medicosApi.obtenerResumenMedico(medicoId)).thenReturn(medico);
        when(medicosApi.obtenerHorarioAtencion(medicoId))
                .thenReturn(HorarioAtencionDTO.builder().intervaloMinutos(20).build());
        when(disponibilidadService.calcularHorariosDisponibles(medicoId, manana))
                .thenReturn(List.of(LocalTime.of(10, 0)));

        PrimerHorarioDisponibleResponse response = citaService.obtenerPrimerHorarioDisponibleMedico(medicoId, null);

        assertEquals(manana, response.getFecha());
        assertEquals(LocalTime.of(10, 0), response.getHora());
        verify(disponibilidadService, never()).calcularHorariosDisponibles(eq(medicoId), eq(LocalDate.now()));
    }

    @Test
    void obtenerPrimerHorarioGlobal_conDesdeHoy_empiezaDesdeManana() {
        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);

        MedicoResumenDTO medico = MedicoResumenDTO.builder()
                .id(1L)
                .nombresCompletos("Medico Uno")
                .especialidad("QUIROPRAXIA")
                .activo(true)
                .build();

        when(medicosApi.listarMedicosActivos()).thenReturn(List.of(medico));
        when(medicosApi.obtenerHorarioAtencion(1L))
                .thenReturn(HorarioAtencionDTO.builder().intervaloMinutos(15).build());
        when(disponibilidadService.calcularHorariosDisponibles(1L, manana))
                .thenReturn(List.of(LocalTime.of(9, 0)));

        PrimerHorarioDisponibleResponse response = citaService.obtenerPrimerHorarioDisponibleGlobal(hoy);

        assertEquals(manana, response.getFecha());
        verify(disponibilidadService, never()).calcularHorariosDisponibles(eq(1L), eq(hoy));
    }

    @Test
    void obtenerPrimerHorarioGlobal_conDesdePasado_empiezaDesdeManana() {
        LocalDate pasado = LocalDate.now().minusDays(3);
        LocalDate manana = LocalDate.now().plusDays(1);

        MedicoResumenDTO medico = MedicoResumenDTO.builder()
                .id(1L)
                .nombresCompletos("Medico Uno")
                .especialidad("QUIROPRAXIA")
                .activo(true)
                .build();

        when(medicosApi.listarMedicosActivos()).thenReturn(List.of(medico));
        when(medicosApi.obtenerHorarioAtencion(1L))
                .thenReturn(HorarioAtencionDTO.builder().intervaloMinutos(15).build());
        when(disponibilidadService.calcularHorariosDisponibles(1L, manana))
                .thenReturn(List.of(LocalTime.of(9, 30)));

        PrimerHorarioDisponibleResponse response = citaService.obtenerPrimerHorarioDisponibleGlobal(pasado);

        assertEquals(manana, response.getFecha());
        verify(disponibilidadService, never()).calcularHorariosDisponibles(eq(1L), eq(LocalDate.now()));
    }
}

