package com.piedrazul.backend.agenda.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.shared.audit.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CitaAuditoriaAdapterTest {

    @Mock private AuditService auditService;

    @InjectMocks private CitaAuditoriaAdapter adapter;

    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID actor = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("registrarCreacion debe persistir medicoId y pacienteId en el JSON de auditoría")
    void registrarCreacion_debePersistirCamposEnJson() throws Exception {
        Cita cita = Cita.builder()
                .id(42L)
                .medicoId(5L)
                .pacienteId(99L)
                .estado(EstadoCita.PROGRAMADA)
                .build();

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        adapter.registrarCreacion(actor, "CREAR", cita);

        verify(auditService).registrar(
                eq(actor), eq("CREAR"), eq("CITA"), eq(42L), jsonCaptor.capture());

        JsonNode nodo = mapper.readTree(jsonCaptor.getValue());
        assertThat(nodo.get("medicoId").asLong()).isEqualTo(5L);
        assertThat(nodo.get("pacienteId").asLong()).isEqualTo(99L);
    }

    @Test
    @DisplayName("registrarActualizacion con cambio de observaciones debe incluir valores anterior y nuevo")
    void registrarActualizacion_conCambioDeObservaciones_incluyeAmbosValores() throws Exception {
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        adapter.registrarActualizacion(actor, 7L, "ATENDIDA", "obs vieja", "obs nueva");

        verify(auditService).registrar(
                eq(actor), eq("ACTUALIZAR"), eq("CITA"), eq(7L), jsonCaptor.capture());

        JsonNode nodo = mapper.readTree(jsonCaptor.getValue());
        assertThat(nodo.get("nuevoEstado").asText()).isEqualTo("ATENDIDA");
        assertThat(nodo.get("observacionesAnterior").asText()).isEqualTo("obs vieja");
        assertThat(nodo.get("observacionesNueva").asText()).isEqualTo("obs nueva");
    }

    @Test
    @DisplayName("registrarActualizacion sin cambio de observaciones solo incluye nuevoEstado")
    void registrarActualizacion_sinCambioObservaciones_soloEstado() throws Exception {
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        adapter.registrarActualizacion(actor, 7L, "CANCELADA", "misma obs", "misma obs");

        verify(auditService).registrar(
                eq(actor), eq("ACTUALIZAR"), eq("CITA"), eq(7L), jsonCaptor.capture());

        JsonNode nodo = mapper.readTree(jsonCaptor.getValue());
        assertThat(nodo.get("nuevoEstado").asText()).isEqualTo("CANCELADA");
        assertThat(nodo.has("observacionesNueva")).isFalse();
    }

    @Test
    @DisplayName("registrarReagendamiento debe incluir fechaAnterior y horaNueva")
    void registrarReagendamiento_debePersistirFechaYHora() throws Exception {
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        LocalDate fechaAnterior = LocalDate.of(2026, 6, 1);
        LocalTime nuevaHora = LocalTime.of(9, 30);

        adapter.registrarReagendamiento(actor, 15L, fechaAnterior, nuevaHora, "Viaje médico");

        verify(auditService).registrar(
                eq(actor), eq("REPROGRAMAR"), eq("CITA"), eq(15L), jsonCaptor.capture());

        JsonNode nodo = mapper.readTree(jsonCaptor.getValue());
        assertThat(nodo.get("fechaAnterior").asText()).isEqualTo("2026-06-01");
        assertThat(nodo.get("horaNueva").asText()).isEqualTo("09:30");
        assertThat(nodo.get("motivo").asText()).isEqualTo("Viaje médico");
    }
}
