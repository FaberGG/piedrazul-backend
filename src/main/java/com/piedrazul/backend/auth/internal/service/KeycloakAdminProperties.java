package com.piedrazul.backend.auth.internal.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {
    // getters y setters
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
}