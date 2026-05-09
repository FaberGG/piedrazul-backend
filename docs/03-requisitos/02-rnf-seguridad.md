# Requisitos No Funcionales (Seguridad y Calidad)

## Seguridad de acceso

Implementacion base observada en `SecurityConfig`:

- Backend como `OAuth2 Resource Server` validando JWT por `issuer-uri`.
- Roles mapeados desde claims del token (`roles`, `realm_access.roles` y `resource_access.*.roles`).
- Autorizacion por rol con `@PreAuthorize`.
- Endpoints publicos restringidos a `POST /api/v1/auth/register/paciente` y Swagger.
- Respuesta uniforme de no autorizado: `{"error":"No autorizado"}`.

## Rate limiting

Se implemento limitacion de tasa a nivel de filtro de seguridad para endpoints publicos y aquellos donde `PACIENTE` tiene acceso. Los limites se configuran en `src/main/resources/application.yml` bajo `app.rate-limiting`.

Politica aplicada:

- Endpoint publico `POST /api/v1/auth/register/paciente`: 5 solicitudes por minuto por IP.
- Endpoints con rol `PACIENTE` (clave por `sub` del JWT, con fallback a IP):
  - `GET /api/v1/medicos`: 60 solicitudes por minuto.
  - `GET /api/v1/medicos/*/configuracion`: 30 solicitudes por minuto.
  - `GET /api/v1/pacientes/*`: 30 solicitudes por minuto.
  - `POST /api/v1/citas/autonomo`: 10 solicitudes por minuto.
  - `GET /api/v1/citas/disponibilidad/primera`: 60 solicitudes por minuto.
  - `GET /api/v1/citas/disponibilidad/primera/global`: 30 solicitudes por minuto.
  - `GET /api/v1/citas/disponibilidad/franjas`: 60 solicitudes por minuto.

Respuesta cuando se excede el limite:

- HTTP `429 Too Many Requests`
- Header `Retry-After` con segundos aproximados para reintentar.
- Body: `{"error":"Rate limit excedido"}`

Nota: si el request es autenticado pero no tiene rol `PACIENTE`, no aplica esta politica.

## Control por roles (RBAC)

Roles usados en el proyecto:

- `PACIENTE`
- `AGENDADOR`
- `MEDICO`
- `MEDICO_TERAPISTA`
- `ADMIN`
- `ADMINISTRADOR` (presente en documentos/legado; revisar estandarizacion)

Recomendacion de gobierno:

- Definir una convencion unica de rol administrativo (`ADMIN` o `ADMINISTRADOR`) y aplicar en controladores y documentacion.

## Seguridad de credenciales

- El login y la emision del token los gestiona Keycloak (no Spring).
- El backend usa `keycloak-admin-client` para altas de usuarios y asignacion de roles.
- Secretos (DB, Keycloak admin client, issuer) externalizados por variables de entorno (`.env.dev` en local).

## Integridad de negocio

Controles funcionales relevantes ya implementados:

- No crear citas en fecha pasada.
- Validar medico activo y horario configurado.
- Evitar doble agendamiento en mismo slot por medico.
- Respetar intervalo por medico para disponibilidad y duracion estandar.

## Trazabilidad y auditoria

- Uso de `AuditService` para registrar operaciones criticas (ej. creacion de cita).
- Base para seguimiento operativo y control administrativo.

## Calidad tecnica y mantenibilidad

- Monolito modular con fronteras definidas.
- Contratos internos entre modulos (`*Api`) para bajo acoplamiento.
- Test de conformidad modular (`ModularityTest`).

## Riesgos y recomendaciones

- **Riesgo:** coexistencia de roles `ADMIN` y `ADMINISTRADOR`.
- **Riesgo:** desalineacion entre claims emitidos por Keycloak y converter de Spring Security.
- **Recomendado:** externalizar secretos con variables de entorno en todos los entornos.
- **Recomendado:** endurecer politica de registro admin en entornos no locales.
- **Recomendado:** mantener control de permisos minimos para el service account de Keycloak (`manage-users`) y rotar `client-secret`.
