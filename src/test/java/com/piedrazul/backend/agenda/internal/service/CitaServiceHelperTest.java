package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.repository.AgendaDiaLockRepository;
import com.piedrazul.backend.auth.api.AuthApi;
import com.piedrazul.backend.auth.api.dto.UsuarioInfoDto;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CitaServiceHelperTest {

    @Mock private AgendaDiaLockRepository agendaDiaLockRepository;
    @Mock private AuthApi authApi;

    @InjectMocks private CitaServiceHelper helper;

    @BeforeEach
    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── JWT resolution ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Usuario registrado en DB: devuelve UUID interno de la tabla usuarios")
    void obtenerUsuarioIdAutenticado_usuarioRegistrado_retornaUuidInterno() {
        String keycloakId = "aaaaaaaa-0000-0000-0000-000000000001";
        UUID uuidInterno = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

        configurarJwt(keycloakId);
        when(authApi.findByKeycloakId(keycloakId))
                .thenReturn(Optional.of(UsuarioInfoDto.builder().id(uuidInterno).build()));

        UUID resultado = helper.obtenerUsuarioIdAutenticado();

        assertThat(resultado).isEqualTo(uuidInterno);
    }

    @Test
    @DisplayName("Usuario bootstrap (no en DB): devuelve el UUID del subject de Keycloak")
    void obtenerUsuarioIdAutenticado_usuarioBootstrap_retornaUuidKeycloak() {
        String keycloakId = "cccccccc-0000-0000-0000-000000000003";

        configurarJwt(keycloakId);
        when(authApi.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        UUID resultado = helper.obtenerUsuarioIdAutenticado();

        assertThat(resultado).isEqualTo(UUID.fromString(keycloakId));
    }

    @Test
    @DisplayName("Sin autenticación en contexto: devuelve null")
    void obtenerUsuarioIdAutenticado_sinAutenticacion_retornaNull() {
        UUID resultado = helper.obtenerUsuarioIdAutenticado();
        assertThat(resultado).isNull();
    }

    // ── parseHora ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseHora con formato válido HH:mm:ss retorna LocalTime correcto")
    void parseHora_formatoValido_retornaLocalTime() {
        var hora = helper.parseHora("09:30:00");
        assertThat(hora.getHour()).isEqualTo(9);
        assertThat(hora.getMinute()).isEqualTo(30);
    }

    @Test
    @DisplayName("parseHora con formato inválido lanza BusinessRuleException")
    void parseHora_formatoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> helper.parseHora("9:30"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("formato");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void configurarJwt(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
