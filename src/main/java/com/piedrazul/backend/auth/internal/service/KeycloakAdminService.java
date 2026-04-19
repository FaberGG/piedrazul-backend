package com.piedrazul.backend.auth.internal.service;

import com.piedrazul.backend.shared.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
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

    String tokenUrl = props.getServerUrl()
            + "/realms/" + props.getRealm()
            + "/protocol/openid-connect/token";

    log.info("Obteniendo token de: {}", tokenUrl);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "client_credentials");
    body.add("client_id", props.getClientId());
    body.add("client_secret", props.getClientSecret());
    log.info("Secret que se usa: '{}'", props.getClientSecret());

    try {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (response.getBody() == null || response.getBody().get("access_token") == null) {
            throw new BusinessRuleException("Keycloak no devolvió access_token");
        }

        String token = (String) response.getBody().get("access_token");

        log.info("Token obtenido correctamente");
        return token;

    } catch (Exception e) {
        log.error("Error obteniendo token de Keycloak: {}", e.getMessage());
        throw new BusinessRuleException("Error conectando a Keycloak: " + e.getMessage());
    }
}

    // 👤 CREAR USUARIO
    public void crearUsuario(String username, String email, String password, String rol, String nombres, String apellidos) {

    log.info("Creando usuario en Keycloak: {}", username);

    String token = obtenerToken();

    String usersUrl = props.getServerUrl()
            + "/admin/realms/" + props.getRealm() + "/users";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    Map<String, Object> userBody = Map.of(
            "username", username != null ? username : "",
            "email", email != null ? email : "",
            "firstName", nombres != null ? nombres : "",
            "lastName", apellidos != null ? apellidos : "",
            "enabled", true,
            "emailVerified", true,
            "credentials", List.of(
                    Map.of(
                            "type", "password",
                            "value", password,
                            "temporary", false
                    )
            )
    );

    try {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                usersUrl,
                new HttpEntity<>(userBody, headers),
                Void.class
        );
        log.info("Usuario creado en Keycloak. Status: {}", response.getStatusCode());
    } catch (HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.CONFLICT) {
            throw new BusinessRuleException("El usuario ya existe en Keycloak");
        }
        log.error("Error creando usuario en Keycloak: {}", e.getResponseBodyAsString());
        throw new BusinessRuleException("Error creando usuario: " + e.getMessage());
    }
}
}