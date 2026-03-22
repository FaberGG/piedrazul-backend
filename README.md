# Piedrazul Backend

Backend del sistema de agendamiento medico de Piedrazul, implementado como monolito modular con Spring Boot.

Este README refleja el estado actual del codigo, incluyendo la arquitectura con los modulos `pacientes` y `medicos`, los endpoints disponibles y el estado funcional de los requisitos RF1, RF2 y RF3.

## Checklist de esta guia

- [x] Arquitectura modular actualizada
- [x] Requisitos funcionales y estado real
- [x] Configuracion de entorno y ejecucion
- [x] Seguridad JWT y roles
- [x] Endpoints HTTP con URL, headers, roles, request/response JSON
- [x] Endpoints definidos pero no implementados (marcados)

## 1) Stack tecnologico

- Java 17 (proyecto), Maven Wrapper
- Spring Boot (starter parent actual en `pom.xml`)
- Spring Security + JWT
- Spring Data JPA + Hibernate
- PostgreSQL (dev)
- H2 (tests)
- Spring Modulith
- Swagger/OpenAPI

## 2) Arquitectura modular actual

El sistema esta organizado por modulos funcionales y contratos publicos.

### Modulos

- `shared`: seguridad, auditoria, excepciones y utilidades transversales
- `auth`: autenticacion, registro y usuarios
- `agenda`: gestion de citas
- `pacientes`: datos de pacientes y API de consulta/creacion para otros modulos
- `medicos`: datos de medicos y API de disponibilidad/resumen para otros modulos
- `reportes`: reportes usando `AgendaApi`

### Contratos de comunicacion entre modulos

- `auth`: `AuthApi`
- `agenda`: `AgendaApi`
- `pacientes`: `PacientesApi` (`pacientes::api`, DTOs en `pacientes::api-dto`)
- `medicos`: `MedicosApi` (`medicos::api`, DTOs en `medicos::api-dto`)

`agenda` consume internamente `PacientesApi` y `MedicosApi` para RF2 (cita manual), evitando dependencia directa a repositorios de otros modulos.

## 3) Estado de requisitos funcionales

- RF1 - Listar agenda por medico y fecha: **implementado**
- RF2 - Crear cita manual: **implementado**
- RF3 - Agendamiento autonomo: **endpoint definido, implementacion pendiente** (`UnsupportedOperationException`)

## 4) Configuracion de entorno

### Desarrollo (`dev`, por defecto)

`application.yml` activa perfil `dev` y usa PostgreSQL local via `application-dev.yml`.

- DB: `piedrazul_dev`
- User: `piedrazul_user`
- Pass: `piedrazul_pass`
- Puerto: `5432`

### Pruebas (`test`)

Se usa H2 en memoria (`src/test/resources/application-test.yml`).

## 5) Levantar el proyecto

### 5.1 Levantar infraestructura local

```bash
docker compose up -d
docker compose ps
```

### 5.2 Ejecutar aplicacion

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

### 5.3 Ejecutar tests

```bash
# Suite completa
./mvnw test

# Solo test de contexto de agenda
./mvnw -Dtest=CitaServiceTest test
```

### 5.4 Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## 6) Seguridad, autenticacion y headers

## Acceso publico (sin token)

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register/paciente`
- `POST /api/v1/auth/register/admin`
- `/swagger-ui/**`
- `/v3/api-docs/**`

## Acceso autenticado

Todos los demas endpoints requieren:

```http
Authorization: Bearer <jwt>
Content-Type: application/json
```

Si falta token o es invalido, la API responde:

```json
{
  "error": "No autorizado"
}
```

## Roles usados actualmente en codigo

- `PACIENTE`
- `AGENDADOR`
- `MEDICO`
- `MEDICO_TERAPISTA`
- `ADMIN`
- `ADMINISTRADOR`

Nota: hay coexistencia de nombres de rol en distintos controladores/documentacion historica; validar el rol exacto segun endpoint.

## 7) Endpoints implementados

Base URL: `http://localhost:8080/api/v1`

### 7.1 Auth

## `POST /auth/login`

- Auth requerida: No
- Body:

```json
{
  "username": "maria.gonzalez",
  "password": "Password123"
}
```

- Response 200:

```json
{
  "token": "eyJ...",
  "userId": 42,
  "username": "maria.gonzalez",
  "rol": "AGENDADOR",
  "expiresIn": 86400
}
```

## `POST /auth/register/paciente`

- Auth requerida: No
- Body:

```json
{
  "username": "paciente.demo",
  "password": "Password123",
  "documento": "1234567890",
  "nombres": "Juan Carlos",
  "apellidos": "Perez Gomez",
  "celular": "3001234567",
  "genero": "MASCULINO",
  "fechaNacimiento": "1985-03-15",
  "correo": "juan@email.com"
}
```

- Response 201:

```json
{
  "token": "eyJ...",
  "userId": 100,
  "username": "paciente.demo",
  "rol": "PACIENTE",
  "expiresIn": 86400
}
```

## `POST /auth/register/admin`

- Auth requerida: No (estado actual de seguridad)
- Body:

```json
{
  "username": "admin.demo",
  "password": "Password123"
}
```

- Response 201:

```json
{
  "token": "eyJ...",
  "userId": 1,
  "username": "admin.demo",
  "rol": "ADMIN",
  "expiresIn": 86400
}
```

## `POST /auth/register/medico`

- Auth requerida: Si
- Rol requerido: `ADMIN`
- Body:

```json
{
  "username": "medico.demo",
  "password": "Password123",
  "nombres": "Clara Ines",
  "apellidos": "Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "tipo": "MEDICO"
}
```

- Response 201:

```json
{
  "token": "eyJ...",
  "userId": 200,
  "username": "medico.demo",
  "rol": "MEDICO",
  "expiresIn": 86400
}
```

### 7.2 Agenda

## `POST /citas/manual` (RF2)

- Auth requerida: Si
- Roles requeridos: `AGENDADOR` o `MEDICO_TERAPISTA`
- Body:

```json
{
  "documento": "1234567890",
  "nombres": "Juan Carlos",
  "apellidos": "Perez Gomez",
  "celular": "3001234567",
  "genero": "MASCULINO",
  "fechaNacimiento": "1985-03-15",
  "correo": "juan@email.com",
  "medicoId": 1,
  "hora": "08:00:00",
  "fecha": "2026-03-20",
  "observaciones": "Dolor lumbar cronico"
}
```

- Response 201:

```json
{
  "id": 101,
  "pacienteNombre": "Juan Carlos Perez Gomez",
  "pacienteDocumento": "1234567890",
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-03-20",
  "hora": "08:00:00",
  "estado": "PROGRAMADA",
  "observaciones": "Dolor lumbar cronico"
}
```

- Validaciones de negocio implementadas:
  - fecha futura
  - medico existente y activo
  - horario disponible segun agenda
  - parseo de hora (`HH:mm:ss`)
  - paciente por documento: reutiliza o crea

## `GET /citas/agenda` (RF1)

- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `ADMIN`
- Query params:

```json
{
  "medicoId": 1,
  "fecha": "2026-03-20"
}
```

- Response 200:

```json
{
  "medicoId": 1,
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-03-20",
  "citas": [
    {
      "id": 101,
      "pacienteNombre": "Ana Perez",
      "pacienteDocumento": "122321",
      "medicoNombre": "Clara Ines Cordoba",
      "especialidad": "TERAPIA_NEURAL",
      "fecha": "2026-03-20",
      "hora": "07:00:00",
      "estado": "PROGRAMADA",
      "observaciones": "Control"
    }
  ],
  "horariosDisponibles": ["07:15:00", "07:30:00"],
  "totalSlots": 20,
  "slotsOcupados": 1,
  "porcentajeOcupacion": 5.0
}
```

- Reglas implementadas en servicio:
  - valida medico existente y activo (`MedicosApi`)
  - valida configuracion horaria activa (`MedicosApi`)
  - filtra citas con estado `CANCELADA` para calculos de ocupacion
  - enriquece respuesta con datos de paciente (`PacientesApi`)
  - calcula `totalSlots` segun `horaInicio`, `horaFin`, `intervaloMinutos` y `diasAtencion` del medico
  - `porcentajeOcupacion = (slotsOcupados / totalSlots) * 100` (si `totalSlots` es 0, retorna 0)

## `POST /citas/autonomo` (RF3)

- Auth requerida: Si
- Rol requerido: `PACIENTE`
- Body:

```json
{
  "medicoId": 1,
  "fecha": "2026-03-20",
  "hora": "09:00:00",
  "observaciones": "Control"
}
```

- Estado actual: endpoint expuesto, **pendiente de implementacion en servicio**.

## `GET /citas/agenda-dinamica`

- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `ADMIN`
- Query params:

```json
{
  "medicoId": 1,
  "fecha": "2026-06-10"
}
```

- Response 200 (estructura para UI declarativa):

```json
{
  "fecha": "2026-06-10",
  "medico": "Dra. Maria Cordoba",
  "primerSlotDisponible": "2026-06-10T09:25:00",
  "bloques": [
    {
      "rango": "9:00 AM - 10:00 AM",
      "estaExpandido": true,
      "slots": [
        {
          "hora": "9:00 AM",
          "estado": "OCUPADO",
          "citaId": 120,
          "pacienteDocumento": "1234567890",
          "pacienteNombres": "Juan Jose",
          "pacienteApellidos": "Perez",
          "pacienteCelular": "3001234567",
          "permiteAbrirPrioridadPosterior": true
        }
      ]
    }
  ]
}
```

- Regla clave: `permiteAbrirPrioridadPosterior` solo se marca en el inicio de una cita ocupada donde el backend valida flexibilidad real para insertar 5 minutos.

## `POST /citas/prioridad`

- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `ADMIN`
- Body:

```json
{
  "documento": "1234567890",
  "nombres": "Paciente",
  "apellidos": "Prioritario",
  "celular": "3001234567",
  "genero": "MASCULINO",
  "fechaNacimiento": "1990-01-01",
  "correo": "prioridad@email.com",
  "medicoId": 1,
  "fecha": "2026-06-10",
  "horaReferencia": "09:00:00",
  "observaciones": "Sobrecupo autorizado"
}
```

- Resultado: crea una cita de tipo `PRIORIDAD` de 5 minutos inmediatamente posterior a la cita de referencia y recorta la cita vecina al minimo permitido cuando aplica.

### 7.3 Pacientes

## `GET /pacientes`

- Auth requerida: Si
- Roles requeridos: `ADMIN` o `MEDICO`
- Response 200:

```json
[
  {
    "id": 1,
    "documento": "1234567890",
    "nombres": "Juan Carlos",
    "apellidos": "Perez Gomez",
    "celular": "3001234567",
    "correo": "juan@email.com",
    "fechaNacimiento": "1985-03-15",
    "genero": "MASCULINO"
  }
]
```

## `GET /pacientes/{id}`

- Auth requerida: Si
- Roles requeridos: `ADMIN` o `MEDICO` o `PACIENTE`
- Response 200:

```json
{
  "id": 1,
  "documento": "1234567890",
  "nombres": "Juan Carlos",
  "apellidos": "Perez Gomez",
  "celular": "3001234567",
  "correo": "juan@email.com",
  "fechaNacimiento": "1985-03-15",
  "genero": "MASCULINO"
}
```

## `GET /pacientes/buscar` (search-as-you-type)

- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `ADMIN`
- Query params:

```json
{
  "documento": "123",
  "limit": 5
}
```

- Response 200:

```json
[
  {
    "id": 10,
    "documento": "1234567890",
    "nombresCompletos": "Juan Carlos Perez Gomez"
  },
  {
    "id": 18,
    "documento": "1234987654",
    "nombresCompletos": "Juana Perez Soto"
  }
]
```

- Reglas implementadas:
  - búsqueda por prefijo (`documento` empieza con el valor ingresado)
  - mínimo 2 caracteres para devolver sugerencias
  - `limit` por defecto `5`, máximo `10`
  - respuesta liviana para autocompletado (sin datos clínicos)

### 7.4 Medicos

## `GET /medicos`

- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `PACIENTE`, `ADMIN`
- Query params opcionales:

```json
{
  "especialidad": "TERAPIA_NEURAL"
}
```

- Response 200:

```json
[
  {
    "id": 1,
    "nombresCompletos": "Clara Ines Cordoba",
    "especialidad": "TERAPIA_NEURAL",
    "tipo": "MEDICO",
    "activo": true,
    "intervaloMinutos": 15
  }
]
```

## `GET /medicos/{medicoId}/configuracion`

- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `PACIENTE`, `ADMIN`
- Response 200:

```json
{
  "medicoId": 1,
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "activo": true,
  "diasAtencion": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "horaInicio": "07:00:00",
  "horaFin": "12:00:00",
  "intervaloMinutos": 15,
  "capacidadDiaria": 20
}
```

## `PUT /medicos/{medicoId}/configuracion`

- Auth requerida: Si
- Rol requerido: `ADMIN`
- Body:

```json
{
  "diasAtencion": ["MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"],
  "horaInicio": "07:00:00",
  "horaFin": "12:00:00",
  "intervaloMinutos": 15
}
```

- Response 200:

```json
{
  "medicoId": 1,
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "activo": true,
  "diasAtencion": ["MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"],
  "horaInicio": "07:00:00",
  "horaFin": "12:00:00",
  "intervaloMinutos": 15,
  "capacidadDiaria": 20
}
```

- Validaciones de negocio implementadas:
  - `horaFin` debe ser mayor a `horaInicio`
  - jornada entre 2 y 8 horas
  - `intervaloMinutos` en: `5, 10, 15, 20, 30, 45, 60`
  - `diasAtencion` no puede ser vacio

### 7.5 Reportes

## `GET /reportes/citas`

- Auth requerida: Si
- Roles requeridos: `AGENDADOR` o `ADMINISTRADOR`
- Query params:

```json
{
  "desde": "2026-03-01",
  "hasta": "2026-03-31"
}
```

- Response 200:

```json
{
  "desde": "2026-03-01",
  "hasta": "2026-03-31",
  "totalCitas": 25,
  "citasAtendidas": 10,
  "citasCanceladas": 3,
  "citasProgramadas": 12,
  "porcentajeOcupacion": 0.0
}
```

## 8) Endpoints de requisitos definidos pero no completos

- RF3: `POST /api/v1/citas/autonomo` (servicio pendiente)

## 9) Flujo recomendado de uso API

1. Crear cuenta admin (`/auth/register/admin`) o usar un admin existente.
2. Login (`/auth/login`) y guardar JWT.
3. Registrar medico (`/auth/register/medico`) con token admin.
4. Crear cita manual (`/citas/manual`) con rol `AGENDADOR` o `MEDICO_TERAPISTA`.
5. Consultar pacientes y reportes segun permisos.

## 10) Notas importantes del estado actual

- El modulo `medicos` expone endpoints REST para listado y configuracion de agenda por medico (`/api/v1/medicos`).
- `PacientesApi` y `MedicosApi` ya se usan en `agenda` para RF1 y RF2.
- Cada medico se crea con configuracion base (07:00-12:00, intervalo 15 min, lunes-viernes) y puede ser ajustado por `ADMIN` en `PUT /api/v1/medicos/{medicoId}/configuracion`.
- Si ejecutas tests sin perfil `test`, el contexto puede intentar usar PostgreSQL dev.

## 11) Comandos utiles de operacion

```bash
# Compilar
./mvnw -DskipTests compile

# Ejecutar app
./mvnw spring-boot:run

# Test de contexto de agenda
./mvnw -Dtest=CitaServiceTest test

# Detener infraestructura
docker compose down
```

