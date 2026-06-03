package com.piedrazul.backend.agenda.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.realtime.AgendaDinamicaSseHub;
import com.piedrazul.backend.agenda.internal.service.CitaService;
import com.piedrazul.backend.agenda.internal.service.DisponibilidadService;
import com.piedrazul.backend.auth.api.AuthApi;
import com.piedrazul.backend.medicos.internal.service.MedicosFacade;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración — capa MVC.
 * Verifica routing HTTP, serialización JSON y control de acceso por rol.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class CitaControllerIntegrationTest {

    // Infraestructura externa simulada
    @MockitoBean JwtDecoder    jwtDecoder;
    @MockitoBean PacientesApi  pacientesApi;
    @MockitoBean MedicosFacade medicosApi;
    @MockitoBean AuthApi       authApi;

    // Dependencias del controlador simuladas para aislar la capa MVC
    @MockitoBean CitaService            citaService;
    @MockitoBean DisponibilidadService  disponibilidadService;
    @MockitoBean AgendaDinamicaSseHub   agendaDinamicaSseHub;

    @Autowired WebApplicationContext wac;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /agenda con rol AGENDADOR — responde 200 con JSON de agenda")
    @WithMockUser(roles = "AGENDADOR")
    void listarAgenda_rolAgendador_retorna200() throws Exception {
        AgendaResponse respuesta = AgendaResponse.builder()
                .medicoId(1L)
                .medicoNombre("Dr. Test")
                .fecha(LocalDate.of(2026, 7, 1))
                .build();

        when(citaService.listarAgendaMedico(eq(1L), any(LocalDate.class)))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/citas/agenda")
                        .param("medicoId", "1")
                        .param("fecha", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.medicoId").value(1));
    }

    @Test
    @DisplayName("POST /manual con rol AGENDADOR — responde 201 con cita creada")
    @WithMockUser(roles = "AGENDADOR")
    void crearCitaManual_rolAgendador_retorna201() throws Exception {
        CrearCitaManualRequest request = new CrearCitaManualRequest(
                "1234567890", "Ana", "Lopez", "3001234567",
                "FEMENINO", LocalDate.of(1990, 1, 1), "ana@test.com",
                1L, "09:00:00", LocalDate.of(2026, 7, 10), null);

        CitaResponse citaCreada = CitaResponse.builder()
                .id(100L)
                .estado("PROGRAMADA")
                .medicoNombre("Dr. Test")
                .hora(LocalTime.of(9, 0))
                .fecha(LocalDate.of(2026, 7, 10))
                .build();

        when(citaService.crearCitaManual(any())).thenReturn(citaCreada);

        mockMvc.perform(post("/api/v1/citas/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    @Test
    @DisplayName("POST /manual con rol PACIENTE — debe rechazar con 403")
    @WithMockUser(roles = "PACIENTE")
    void crearCitaManual_rolPaciente_retorna403() throws Exception {
        CrearCitaManualRequest request = new CrearCitaManualRequest(
                "1234567890", "Ana", "Lopez", "3001234567",
                "FEMENINO", LocalDate.of(1990, 1, 1), "ana@test.com",
                1L, "09:00:00", LocalDate.of(2026, 7, 10), null);

        mockMvc.perform(post("/api/v1/citas/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Endpoint sin autenticación — debe rechazar con 401")
    void listarAgenda_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/citas/agenda")
                        .param("medicoId", "1")
                        .param("fecha", "2026-07-01"))
                .andExpect(status().isUnauthorized());
    }
}
