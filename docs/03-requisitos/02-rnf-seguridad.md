# Requisitos No Funcionales (Seguridad y Calidad)

## Seguridad de acceso

Implementacion base observada en `SecurityConfig`:

- Autenticacion stateless con JWT.
- Autorizacion por rol con `@PreAuthorize`.
- Endpoints publicos restringidos a login/registro y Swagger.
- Respuesta uniforme de no autorizado: `{"error":"No autorizado"}`.

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

- Hash de contrasenas con `BCryptPasswordEncoder(12)`.
- Token JWT firmado (libreria JJWT).
- Expiracion de token configurable (`app.jwt.expiration`).

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
- **Riesgo:** parametros sensibles JWT en perfil dev no aptos para produccion.
- **Recomendado:** externalizar secretos con variables de entorno en todos los entornos.
- **Recomendado:** endurecer politica de registro admin en entornos no locales.

