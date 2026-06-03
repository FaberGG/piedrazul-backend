package com.piedrazul.backend.agenda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.auth.api.AuthApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.medicos.internal.service.MedicosFacade;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de SISTEMA — RF2 Creación manual de cita.
 *
 * Carga el contexto completo con H2, ejecuta el flujo completo:
 * HTTP request → filtro de seguridad → controlador → servicio → repositorio → respuesta HTTP.
 *
 * Las APIs inter-módulo (Pacientes, Médicos, Auth) se simulan para aislar
 * el módulo de agenda sin necesitar datos de módulos vecinos.
 */
@Tag("system")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AgendarCitaManualSistemaTest {

    @MockitoBean JwtDecoder    jwtDecoder;
    @MockitoBean PacientesApi  pacientesApi;
    @MockitoBean MedicosFacade medicosApi;
    @MockitoBean AuthApi       authApi;

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

        MedicoResumenDTO medico = MedicoResumenDTO.builder()
                .id(1L)
                .nombresCompletos("Dra. Maria Gomez")
                .especialidad("GENERAL")
                .activo(true)
                .build();

        HorarioAtencionDTO horario = HorarioAtencionDTO.builder()
                .horaInicio(LocalTime.of(7, 0))
                .horaFin(LocalTime.of(17, 0))
                .intervaloMinutos(30)
                .diasAtencion(List.of(DayOfWeek.values()))
                .activo(true)
                .build();

        PacienteResumenDTO paciente = PacienteResumenDTO.builder()
                .id(1L)
                .documento("1234567890")
                .nombres("Carlos")
                .apellidos("Ruiz")
                .build();

        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medico);
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horario);
        when(pacientesApi.obtenerOCrearPorDocumento(any())).thenReturn(paciente);
        when(pacientesApi.obtenerResumenPorId(1L)).thenReturn(paciente);
        when(authApi.findByKeycloakId(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Sistema RF2 — flujo completo: POST /citas/manual crea cita con estado PROGRAMADA")
    void crearCitaManual_flujoCompleto_retorna201ConCitaProgramada() throws Exception {
        LocalDate fechaFutura = LocalDate.now().plusDays(3);

        CrearCitaManualRequest request = new CrearCitaManualRequest(
                "1234567890", "Carlos", "Ruiz", "3001234567",
                "MASCULINO", LocalDate.of(1985, 5, 10), "carlos@test.com",
                1L, "08:00:00", fechaFutura, "Control de rutina");

        mockMvc.perform(post("/api/v1/citas/manual")
                        .with(jwt()
                                .jwt(j -> j.subject("aaaaaaaa-0000-0000-0000-000000000001")
                                        .claim("roles", List.of("AGENDADOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_AGENDADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.medicoNombre").value("Dra. Maria Gomez"));
    }

    @Test
    @DisplayName("Sistema RF1 — flujo completo: GET /citas/agenda retorna estructura de agenda")
    void listarAgenda_flujoCompleto_retornaEstructuraJson() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(1);

        mockMvc.perform(get("/api/v1/citas/agenda")
                        .with(jwt()
                                .jwt(j -> j.subject("aaaaaaaa-0000-0000-0000-000000000001")
                                        .claim("roles", List.of("AGENDADOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_AGENDADOR")))
                        .param("medicoId", "1")
                        .param("fecha", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicoId").value(1))
                .andExpect(jsonPath("$.medicoNombre").value("Dra. Maria Gomez"));
    }

    @Test
    @DisplayName("Sistema — petición sin token JWT debe retornar 401")
    void crearCitaManual_sinToken_retorna401() throws Exception {
        CrearCitaManualRequest request = new CrearCitaManualRequest(
                "9999999999", "Anon", "User", "3009999999",
                "MASCULINO", null, null, 1L, "09:00:00",
                LocalDate.now().plusDays(2), null);

        mockMvc.perform(post("/api/v1/citas/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
