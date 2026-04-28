# Keycloak - Autenticacion e Integracion

Esta guia centraliza la configuracion y el flujo operativo de autenticacion para el proyecto.

## Alcance

- Keycloak gestiona login y emision de tokens.
- El backend Spring no expone endpoint de login.
- El backend valida JWT como `OAuth2 Resource Server`.
- El backend usa `keycloak-admin-client` para crear usuarios y asignar roles.

## URL base en desarrollo

- Keycloak: `http://localhost:8180`
- Realm: `piedrazul`
- Issuer esperado por Spring: `http://localhost:8180/realms/piedrazul`

## Variables de entorno relevantes

Fuente local recomendada: `.env.dev`.

```dotenv
OAUTH2_ISSUER_URI=http://localhost:8180/realms/piedrazul
KEYCLOAK_SERVER_URL=http://localhost:8180
KEYCLOAK_REALM=piedrazul
KEYCLOAK_CLIENT_ID=piedrazul-backend
KEYCLOAK_CLIENT_SECRET=<client-secret>
```

## Configuracion del cliente `piedrazul-backend`

1. Crear/usar el cliente `piedrazul-backend` en el realm `piedrazul`.
2. Habilitar autenticacion de cliente (client secret).
3. Habilitar service account para el cliente.
4. Asignar permisos minimos al service account:
   - `realm-management -> manage-users`
5. Copiar el client secret y cargarlo en `KEYCLOAK_CLIENT_SECRET`.

## Login (obtener token)

El login se realiza contra Keycloak, no contra Spring.

```http
POST http://localhost:8180/realms/piedrazul/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

client_id=piedrazul-frontend&grant_type=password&username=<usuario>&password=<password>
```

Respuesta esperada: `access_token`, `refresh_token`, `expires_in`.

## Uso del token en el backend

Enviar el token en cada endpoint protegido:

```http
Authorization: Bearer <access_token>
```

El backend convierte roles desde estos claims del JWT:

- `roles`
- `realm_access.roles`
- `resource_access.*.roles`

## Endpoints de registro gestionados por Spring

- `POST /api/v1/auth/register/paciente` -> publico
- `POST /api/v1/auth/register/medico` -> requiere `ROLE_ADMIN`
- `POST /api/v1/auth/register/admin` -> requiere `ROLE_ADMIN`

Nota: registrar paciente usa `documento` como username interno y no requiere campo `username` en el body.

## Flujo de alta de usuario

1. Spring valida reglas de negocio internas (password, duplicidad de username interno, etc.).
2. `KeycloakAdminService` crea usuario en Keycloak y asigna rol de realm.
3. Spring persiste `Usuario` local con:
   - `id` (UUID interno)
   - `keycloakId`
   - `username`
   - `rol`
4. Para paciente/medico, Spring persiste tambien datos de negocio en su modulo.

## Errores esperados

- `401 Unauthorized`: token ausente/invalido/expirado.
- `403 Forbidden`: token valido sin rol suficiente (por ejemplo, sin `ADMIN`).
- `422 Unprocessable Entity`: regla de negocio o error funcional en alta de usuario (incluye conflictos de Keycloak mapeados a negocio).
- `500 Internal Server Error`: error inesperado no controlado.

## Troubleshooting rapido

- Si `@PreAuthorize("hasRole('ADMIN')")` falla con token valido:
  - Verificar que el JWT incluya `ADMIN` en alguno de los claims soportados.
- Si falla el arranque por `JwtDecoder`:
  - Confirmar `OAUTH2_ISSUER_URI` correcto y Keycloak disponible.
- Si falla alta de usuario en Keycloak:
  - Confirmar `KEYCLOAK_CLIENT_SECRET` y permisos de service account (`manage-users`).

