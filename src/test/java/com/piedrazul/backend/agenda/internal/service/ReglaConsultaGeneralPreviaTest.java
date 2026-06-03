package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.TipoCita;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReglaConsultaGeneralPreviaTest {

    @Mock private CitaRepository citaRepository;

    @InjectMocks private ReglaConsultaGeneralPrevia regla;

    private static final Long PACIENTE_ID = 10L;
    private static final Long MEDICO_ID = 1L;

    @Test
    @DisplayName("Paciente con Consulta General atendida puede agendar especialidad")
    void validar_pacienteConConsultaGeneral_noLanzaExcepcion() {
        when(citaRepository.existsByPacienteIdAndTipoCitaAndEstado(
                PACIENTE_ID, TipoCita.CONSULTA_GENERAL, EstadoCita.ATENDIDA))
                .thenReturn(true);

        ContextoValidacionCita ctx = new ContextoValidacionCita(PACIENTE_ID, TipoCita.TERAPIA_NEURAL, MEDICO_ID);

        assertThatCode(() -> regla.validar(ctx)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Paciente sin Consulta General atendida no puede agendar especialidad")
    void validar_pacienteSinConsultaGeneral_lanzaExcepcion() {
        when(citaRepository.existsByPacienteIdAndTipoCitaAndEstado(
                PACIENTE_ID, TipoCita.CONSULTA_GENERAL, EstadoCita.ATENDIDA))
                .thenReturn(false);

        ContextoValidacionCita ctx = new ContextoValidacionCita(PACIENTE_ID, TipoCita.QUIROPRAXIA, MEDICO_ID);

        assertThatThrownBy(() -> regla.validar(ctx))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Consulta General");
    }

    @Test
    @DisplayName("Agendar CONSULTA_GENERAL no requiere consulta previa — regla es no-op")
    void validar_consultaGeneral_noConsultaRepositorio() {
        ContextoValidacionCita ctx = new ContextoValidacionCita(PACIENTE_ID, TipoCita.CONSULTA_GENERAL, MEDICO_ID);

        assertThatCode(() -> regla.validar(ctx)).doesNotThrowAnyException();

        verify(citaRepository, never())
                .existsByPacienteIdAndTipoCitaAndEstado(any(), any(), any());
    }

    @Test
    @DisplayName("Agendar ESTANDAR no requiere consulta previa — regla es no-op")
    void validar_estandar_noConsultaRepositorio() {
        ContextoValidacionCita ctx = new ContextoValidacionCita(PACIENTE_ID, TipoCita.ESTANDAR, MEDICO_ID);

        assertThatCode(() -> regla.validar(ctx)).doesNotThrowAnyException();

        verify(citaRepository, never())
                .existsByPacienteIdAndTipoCitaAndEstado(eq(PACIENTE_ID), any(), any());
    }
}
