package com.piedrazul.backend.auth.internal.service;

import com.piedrazul.backend.shared.exception.BusinessRuleException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakAdminService {

    private final KeycloakAdminProperties props;
    private final Keycloak keycloak;
    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    public KeycloakAdminService(KeycloakAdminProperties props, Keycloak keycloak) {
        this.props = props;
        this.keycloak = keycloak;
    }

    public String crearUsuario(String username, String email, String password, String rol) {
        log.info("Creando usuario en Keycloak: {}", username);

        RealmResource realm = keycloak.realm(props.getRealm());
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);

        if (email != null && !email.isBlank()) {
            user.setEmail(email);
            user.setEmailVerified(true);
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setTemporary(false);
        credential.setValue(password);
        user.setCredentials(List.of(credential));

        try (Response response = realm.users().create(user)) {
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new BusinessRuleException("El usuario ya existe en Keycloak");
            }
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new BusinessRuleException("Keycloak devolvio error al crear usuario: " + response.getStatus());
            }

            String userId = CreatedResponseUtil.getCreatedId(response);
            if (userId == null || userId.isBlank()) {
                throw new BusinessRuleException("No fue posible obtener el id de usuario en Keycloak");
            }

            asignarRol(realm, userId, rol);
            return userId;
        } catch (WebApplicationException ex) {
            throw mapearErrorKeycloak(ex, "creando usuario");
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado creando usuario en Keycloak", ex);
            throw new BusinessRuleException("Error creando usuario en Keycloak");
        }
    }

    private void asignarRol(RealmResource realm, String userId, String rolNombre) {
        if (rolNombre == null || rolNombre.isBlank()) {
            return;
        }

        try {
            RoleRepresentation role = realm.roles().get(rolNombre).toRepresentation();
            realm.users().get(userId).roles().realmLevel().add(List.of(role));
        } catch (WebApplicationException ex) {
            throw mapearErrorKeycloak(ex, "asignando rol " + rolNombre);
        }
    }

    private BusinessRuleException mapearErrorKeycloak(WebApplicationException ex, String contexto) {
        int status = ex.getResponse() != null ? ex.getResponse().getStatus() : -1;
        if (status == Response.Status.CONFLICT.getStatusCode()) {
            return new BusinessRuleException("El usuario ya existe en Keycloak");
        }
        String detalle = ex.getMessage();
        if (detalle == null || detalle.isBlank()) {
            detalle = "estado " + status;
        }
        log.error("Error Keycloak {}: {}", contexto, detalle);
        return new BusinessRuleException("Error en Keycloak " + contexto + ": " + detalle);
    }
}