# Flujos de Interaccion por Rol

## 1) Paciente

Flujo disponible hoy:

1. `POST /auth/register/paciente` (si no tiene cuenta).
2. Login en Keycloak (obtencion de `access_token`).
3. `GET /medicos` para explorar profesionales.
4. `GET /citas/disponibilidad/primera/global` para recomendacion inicial.

Flujo objetivo (RF3, pendiente de cierre):

5. `POST /citas/autonomo` para confirmar cita propia.

## 2) Agendador de citas

Flujo operativo principal:

1. Login en Keycloak (obtencion de `access_token`).
2. `GET /medicos` para seleccionar profesional.
3. `GET /citas/agenda` para consultar agenda diaria.
4. `GET /pacientes/buscar?documento=...` para sugerencias por documento.
5. `POST /citas/manual` para registrar cita.
6. `GET /citas/agenda-dinamica` para panel de slots y operacion prioritaria.
7. `POST /citas/prioridad` cuando aplica sobrecupo de 5 minutos.
8. `GET /reportes/citas` para consolidado de actividad.

## 3) Medico / Terapista

Flujo tipico:

1. Login en Keycloak (obtencion de `access_token`).
2. `GET /citas/agenda` para ver agenda por fecha.
3. `GET /citas/agenda-dinamica` para visualizacion operativa avanzada.
4. `GET /pacientes/{id}` para detalle de paciente asociado.
5. `POST /citas/manual` si requiere crear cita asistida.

## 4) Administrador

Flujo de configuracion y gobierno:

1. Login en Keycloak (obtencion de `access_token` con rol `ADMIN`).
2. `POST /auth/register/medico` para alta de profesional.
3. `PUT /medicos/{medicoId}/configuracion` para configurar agenda por medico.
4. `GET /medicos/{medicoId}/configuracion` para verificar capacidad configurada.
5. `GET /reportes/citas` para supervision.

## 5) Flujos internos entre modulos (sin HTTP)

### Agenda al crear cita manual

1. Valida medico y horario con `MedicosApi`.
2. Resuelve paciente con `PacientesApi`.
3. Persiste cita en `CitaRepository`.
4. Registra auditoria con `AuditService`.

### Agenda al listar agenda por medico y fecha

1. Obtiene resumen/configuracion del medico (`MedicosApi`).
2. Consulta citas del dia en agenda.
3. Enriquece datos de paciente (`PacientesApi`).
4. Calcula disponibilidad y ocupacion.

