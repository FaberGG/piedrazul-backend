package com.piedrazul.backend.agenda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.TipoCita;
import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
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
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de ACEPTACIÓN — RF3 Agendamiento Autónomo del Paciente.
 *
 * Cada método corresponde a un Criterio de Aceptación expresado
 * en lenguaje de negocio con formato Dado/Cuando/Entonces.
 */
@Tag("acceptance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AgendarCitaAutonomoAceptacionTest {

    @MockitoBean JwtDecoder    jwtDecoder;
    @MockitoBean PacientesApi  pacientesApi;
    @MockitoBean MedicosFacade medicosApi;
    @MockitoBean AuthApi       authApi;

    @Autowired WebApplicationContext wac;
    @Autowired CitaRepository citaRepository;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    private static final UUID PACIENTE_UUID  = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final Long PACIENTE_ID    = 50L;
    private static final Long MEDICO_ID      = 1L;
    private static final LocalDate FECHA_FUTURA = LocalDate.now().plusDays(5);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        when(authApi.findByKeycloakId(PACIENTE_UUID.toString())).thenReturn(Optional.empty());

        when(pacientesApi.buscarPorUsuarioId(PACIENTE_UUID)).thenReturn(
                PacienteResumenDTO.builder()
                        .id(PACIENTE_ID)
                        .documento("5555555555")
                        .nombres("Lucía")
                        .apellidos("Castro")
                        .build());

        when(pacientesApi.obtenerResumenPorId(PACIENTE_ID)).thenReturn(
                PacienteResumenDTO.builder()
                        .id(PACIENTE_ID)
                        .documento("5555555555")
                        .nombres("Lucía")
                        .apellidos("Castro")
                        .build());

        when(medicosApi.obtenerResumenMedico(MEDICO_ID)).thenReturn(
                MedicoResumenDTO.builder()
                        .id(MEDICO_ID)
                        .nombresCompletos("Dr. Perez")
                        .especialidad("GENERAL")
                        .activo(true)
                        .build());

        when(medicosApi.obtenerHorarioAtencion(MEDICO_ID)).thenReturn(
                HorarioAtencionDTO.builder()
                        .horaInicio(LocalTime.of(7, 0))
                        .horaFin(LocalTime.of(17, 0))
                        .intervaloMinutos(30)
                        .diasAtencion(List.of(DayOfWeek.values()))
                        .activo(true)
                        .build());
    }

    /**
     * CA-03.1 — Dado un paciente activo sin citas pendientes,
     * cuando agenda autónomamente con datos válidos,
     * entonces la cita queda en estado PROGRAMADA.
     */
    @Test
    @DisplayName("CA-03.1 — Paciente activo sin citas previas agenda con éxito; estado = PROGRAMADA")
    void CA0301_pacienteActivoSinCitas_agendaCorrectamente() throws Exception {
        mockMvc.perform(post("/api/v1/citas/autonomo")
                        .with(jwt()
                                .jwt(j -> j.subject(PACIENTE_UUID.toString())
                                        .claim("roles", List.of("PACIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgendarAutonomoRequest(MEDICO_ID, FECHA_FUTURA, LocalTime.of(8, 0), "Primera visita"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    /**
     * CA-03.3 — Dado un paciente con cita ya PROGRAMADA,
     * cuando intenta agendar una segunda cita activa,
     * entonces el sistema rechaza con error de negocio (4xx).
     */
    @Test
    @DisplayName("CA-03.3 — Paciente con cita activa no puede agendar otra sin cancelar la actual")
    void CA0303_pacienteConCitaActiva_noPuedeAgendarOtra() throws Exception {
        citaRepository.saveAndFlush(Cita.builder()
                .pacienteId(PACIENTE_ID)
                .medicoId(MEDICO_ID)
                .fecha(FECHA_FUTURA)
                .hora(LocalTime.of(9, 0))
                .estado(EstadoCita.PROGRAMADA)
                .tipoCita(TipoCita.CONSULTA_GENERAL)
                .build());

        mockMvc.perform(post("/api/v1/citas/autonomo")
                        .with(jwt()
                                .jwt(j -> j.subject(PACIENTE_UUID.toString())
                                        .claim("roles", List.of("PACIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgendarAutonomoRequest(MEDICO_ID, FECHA_FUTURA.plusDays(1), LocalTime.of(10, 0), null))))
                .andExpect(status().is4xxClientError());
    }

    /**
     * CA-03.5 — Dado un paciente sin Consulta General atendida,
     * cuando intenta agendar con un médico de especialidad (TERAPIA_NEURAL),
     * entonces la regla de negocio lo rechaza.
     */
    @Test
    @DisplayName("CA-03.5 — Paciente sin consulta general previa no puede agendar especialidad")
    void CA0305_pacienteSinConsultaGeneral_noPuedeAgendarEspecialidad() throws Exception {
        when(medicosApi.obtenerResumenMedico(MEDICO_ID)).thenReturn(
                MedicoResumenDTO.builder()
                        .id(MEDICO_ID)
                        .nombresCompletos("Dr. Especialista")
                        .especialidad("TERAPIA_NEURAL")
                        .activo(true)
                        .build());

        mockMvc.perform(post("/api/v1/citas/autonomo")
                        .with(jwt()
                                .jwt(j -> j.subject(PACIENTE_UUID.toString())
                                        .claim("roles", List.of("PACIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgendarAutonomoRequest(MEDICO_ID, FECHA_FUTURA, LocalTime.of(8, 30), null))))
                .andExpect(status().is4xxClientError());
    }

    /**
     * CA-03.4 — Dado un slot ya ocupado por otro paciente,
     * cuando el paciente intenta agendar en el mismo horario,
     * entonces el sistema rechaza indicando que el horario no está disponible.
     */
    @Test
    @DisplayName("CA-03.4 — Slot ya ocupado no puede ser reservado por un segundo paciente")
    void CA0304_slotYaOcupado_rechazaSegundaReserva() throws Exception {
        citaRepository.saveAndFlush(Cita.builder()
                .pacienteId(999L)
                .medicoId(MEDICO_ID)
                .fecha(FECHA_FUTURA)
                .hora(LocalTime.of(8, 0))
                .estado(EstadoCita.PROGRAMADA)
                .tipoCita(TipoCita.CONSULTA_GENERAL)
                .build());

        mockMvc.perform(post("/api/v1/citas/autonomo")
                        .with(jwt()
                                .jwt(j -> j.subject(PACIENTE_UUID.toString())
                                        .claim("roles", List.of("PACIENTE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgendarAutonomoRequest(MEDICO_ID, FECHA_FUTURA, LocalTime.of(8, 0), null))))
                .andExpect(status().is4xxClientError());
    }
}
