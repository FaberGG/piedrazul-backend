package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.TipoCita;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ValidadorCitaTest {

    @Mock private ReglaCita regla1;
    @Mock private ReglaCita regla2;

    @Test
    @DisplayName("Debe invocar todas las reglas registradas en orden")
    void validar_debeInvocarTodasLasReglas() {
        ValidadorCita validador = new ValidadorCita(List.of(regla1, regla2));
        ContextoValidacionCita ctx = new ContextoValidacionCita(10L, TipoCita.CONSULTA_GENERAL, 1L);

        assertThatCode(() -> validador.validar(ctx)).doesNotThrowAnyException();

        verify(regla1).validar(ctx);
        verify(regla2).validar(ctx);
    }

    @Test
    @DisplayName("Si una regla lanza BusinessRuleException debe propagarse sin atraparla")
    void validar_siReglaLanzaExcepcion_debePropagar() {
        ContextoValidacionCita ctx = new ContextoValidacionCita(10L, TipoCita.TERAPIA_NEURAL, 1L);
        doThrow(new BusinessRuleException("Regla violada"))
                .when(regla1).validar(ctx);

        ValidadorCita validador = new ValidadorCita(List.of(regla1, regla2));

        assertThatThrownBy(() -> validador.validar(ctx))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Regla violada");
    }

    @Test
    @DisplayName("Con lista vacía de reglas no debe lanzar excepción")
    void validar_sinReglas_noLanzaExcepcion() {
        ValidadorCita validador = new ValidadorCita(List.of());
        ContextoValidacionCita ctx = new ContextoValidacionCita(5L, TipoCita.CONSULTA_GENERAL, 2L);

        assertThatCode(() -> validador.validar(ctx)).doesNotThrowAnyException();
    }
}
