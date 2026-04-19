package com.piedrazul.backend.auth.internal.service;

import com.piedrazul.backend.shared.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminService {

    private final KeycloakAdminProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    public KeycloakAdminService(KeycloakAdminProperties props) {
        this.props = props;
    }

    private String obtenerToken() {
    String tokenUrl = props.getServerUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/token";
    log.info("Obteniendo token de: {}", tokenUrl);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "client_credentials");
    body.add("client_id", props.getClientId());
    body.add("client_secret", props.getClientSecret());
    log.info("Usando clientId: '{}' secret: '{}'", props.getClientId(), props.getClientSecret());

    try {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            tokenUrl, new HttpEntity<>(body, headers), Map.class);
        String token = (String) response.getBody().get("access_token");
        log.info("Token obtenido: {}", token != null ? "OK" : "NULL");
        return token;
    } catch (Exception e) {
        log.error("Error obteniendo token: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        throw new BusinessRuleException("Error conectando a Keycloak: " + e.getMessage());
    }
}

    public void crearUsuario(String username, String email, String password, String rol) {
        log.info("Creando usuario en Keycloak: {}", username);

        String token = obtenerToken();

        String usersUrl = props.getServerUrl() + "/admin/realms/" + props.getRealm() + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> userBody = Map.of(
            "username", username != null ? username : "",
            "email", email != null ? email : "",
            "enabled", true,
            "credentials", List.of(Map.of(
                "type", "password",
                "value", password,
                "temporary", false
            ))
        );

        ResponseEntity<Void> response = restTemplate.postForEntity(
            usersUrl, new HttpEntity<>(userBody, headers), Void.class);

        log.info("Respuesta Keycloak: {}", response.getStatusCode());

        if (response.getStatusCode() == HttpStatus.CONFLICT) {
            throw new BusinessRuleException("El usuario ya existe en Keycloak");
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BusinessRuleException("Error al crear usuario en Keycloak: " + response.getStatusCode());
        }
    }
}