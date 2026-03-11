# Documentacion Backend - Proyecto

Estado: In progress
Revisado: No
Semestre: Septimo

# Arquitectura Backend — Sistema de Agendamiento Piedrazul

**Versión:** 1.0 | **Fecha:** Marzo 2026 | **Equipo:** Ingeniería de Software III — Universidad del Cauca

---

## 1. Introducción

### Alcance

Documentación del backend del Sistema de Agendamiento de Citas Médicas para la Red de Servicios Médicos Piedrazul, implementado como **monolito modular con Spring Boot**.

Cubre: estructura modular, modelo de datos, API REST, seguridad/autenticación, flujos de usuario.

### Stack Tecnológico

| Componente | Tecnología | Versión |
| --- | --- | --- |
| Framework | Spring Boot | 3.2.x |
| Lenguaje | Java | 17 LTS |
| Base de Datos | PostgreSQL | 15-aphine |
| ORM | Spring Data JPA (Hibernate) | — |
| Seguridad | Spring Security + JWT | 6.x |
| Documentación API | SpringDoc OpenAPI | 2.x |
| Mapeo DTOs | MapStruct | 1.5.x |
| Validación | Jakarta Bean Validation | 3.x |
| Build | Maven | 3.9.x |

---

## 2. Contexto del Proyecto

### Situación Actual

- Sistema de escritorio para agendamiento + solicitudes por WhatsApp/teléfono (solo 2–5 PM)
- Especialidades: Terapia Neural, Quiropraxia, Fisioterapia
- Horario de atención: lunes a viernes en las mañanas

### Problemática

- Dependencia del personal para asignar citas manualmente
- Sin canal de autogestión para pacientes
- Restricción de horario para solicitar citas

### Objetivos

- Liberar al personal médico del agendamiento manual
- Permitir a pacientes agendar citas de forma autónoma 24/7
- Mantener control administrativo y trazabilidad total

### Roles de Usuario

| Rol | Permisos Principales |
| --- | --- |
| **Paciente** | Crear cuenta, agendar/cancelar sus propias citas |
| **Agendador de Citas** | Crear citas manuales, consultar agendas, reagendar |
| **Médico/Terapista** | Consultar su agenda, crear citas ocasionales, registrar historias |
| **Administrador** | Gestión de usuarios, configuración del sistema, auditoría |

---

## 3. Requisitos Funcionales

### RF1 — Listar Citas de Médico por Fecha *(Sprint 1 · Alta)*

> **Yo como** agendador de citas **necesito** listar las citas médicas de un determinado médico/terapista en una fecha determinada **para** visualizar el listado y la cantidad de citas. *Contexto*: Se sugiere diseñar un sistema de búsqueda con resultados en una tabla.
> 

Visualizar todas las citas de un médico en una fecha determinada, con datos del paciente, estado de cada cita, horarios disponibles/ocupados y porcentaje de ocupación.

### RF2 — Crear Cita Manual *(Sprint 1 · Alta)*

> **Yo como** agendador de citas **necesito** crear una nueva cita de un paciente que me ha contactado por WhatsApp **para** hacer efectiva esa cita. *Contexto*: los datos que se deben capturar del paciente son: Número de documento de identidad, nombres y apellidos, celular, género (Hombre, Mujer, Otro), fecha de nacimiento (opcional) y correo electrónico (opcional); los datos de la cita son: Médico/terapista, hora. Tener en cuenta el intervalo de tiempo de cada médico/terapista.
> 

Registrar cita para paciente que contactó por WhatsApp/teléfono. Incluye búsqueda o creación de paciente, validación de disponibilidad y respeto al intervalo del médico.

**Datos del paciente requeridos:** documento (obligatorio), nombres/apellidos (obligatorio), celular 10 dígitos (obligatorio), género M/F/Otro (obligatorio), fecha de nacimiento (opcional), correo (opcional).

### RF3 — Agendamiento Autónomo *(Sprint 2 · Alta)*

> **Yo como** paciente **necesito** agendar una cita mediante la web **para** tener una cita de manera sencilla y rápida sin tener que usar WhatsApp. *Contexto*: El paciente debe tener un registro de usuario para poder agendar una cita. El sistema debe brindar un mecanismo para hacer la cita de manera segura, usable y eficiente, mostrando las franjas disponibles para cada médico.
> 

El paciente agenda su cita vía web seleccionando especialidad, médico, fecha y horario. Requiere cuenta verificada por email. Límite: máximo 3 citas futuras. Incluye validación de concurrencia y confirmación por correo.

### RF4 — Configuración de Parámetros *(Sprint 2 · Media)*

> **Yo como** administrador **necesito** configurar los parámetros del sistema  **para** que el agendamiento de citas autónomo funcione acorde a la disponibilidad de los médicos y terapistas de Piedrazul. *Contexto*: Se debe configurar la ventana de tiempo que se habilitarán las citas (en semanas), los días de la semana que cada médico/terapista atiende, la franja horaria de cada médico/terapista, el intervalo de tiempo (minutos) que cada médico/terapista tiene entre cita y cita.
> 

El administrador configura: ventana de agendamiento (1–12 semanas), horarios y días de atención por médico, intervalo entre citas (5/10/15/20/30/45/60 min) y días festivos/no laborables.

### Requisitos No Funcionales Clave

| RNF | Descripción |
| --- | --- |
| **Seguridad** | JWT stateless, BCrypt, RBAC, cierre automático por inactividad |
| **Trazabilidad** | Auditoría de todas las operaciones críticas (usuario, acción, IP, timestamp) |
| **Concurrencia** | Prevención de doble agendamiento con transacciones ACID |
| **Usabilidad API** | Errores descriptivos, códigos HTTP semánticos, Swagger/OpenAPI |

---

## 4. Arquitectura del Sistema

### Patrón: Monolito Modular

**Justificación:** simplicidad de despliegue (un solo JAR), ideal para equipos pequeños, transacciones ACID sencillas, bajo costo operacional y suficiente para el volumen esperado de Piedrazul.

Los módulos tienen límites bien definidos y bajo acoplamiento, lo que permite extraerlos a microservicios en el futuro si es necesario.

### Arquitectura en Capas

![image.png](image.png)

### Diagrama de Contexto

![image.png](image%201.png)

### Diagrama de contenedores

![image.png](image%202.png)

### Diagrama de componentes BAKCEND

![image.png](image%203.png)

### Módulos y Dependencias

El monolito se organiza en **4 módulos funcionales**. La granularidad fue definida según cohesión de dominio: las entidades que solo tienen sentido juntas (Paciente, Médico, Cita) viven en el mismo módulo, evitando acoplamiento distribuido innecesario.

```
SHARED (transversal)
  └── JWT · Auditoría · Excepciones · Utilidades
  └── ningún módulo funcional como dependencia

AUTH
  └── depende de: SHARED
  └── gestiona: Usuario, Login, Registro, Roles

AGENDA  ←  módulo principal
  └── depende de: SHARED, AUTH (contexto de seguridad)
  └── gestiona: Paciente, Médico, ConfiguracionMedico,
               Cita, HistorialCambiosCita,
               HistoriaClinica, ConfiguracionGlobal

REPORTES
  └── depende de: SHARED, AGENDA (solo lectura)
  └── gestiona: consultas analíticas, exportaciones estadísticas
```

**Reglas de dependencia:**

- SHARED no depende de ningún módulo funcional.
- AUTH no depende de AGENDA ni de REPORTES.
- AGENDA accede al contexto de seguridad de AUTH pero no a su lógica interna.
- REPORTES solo lee datos de AGENDA, nunca modifica estado del sistema.
- Ningún módulo depende de REPORTES.

### Flujo de un Request HTTP

```
Cliente → JWT Filter → Controller (valida DTO) → Service (reglas negocio)
       → Repository → PostgreSQL → Audit → Response
```

---

## 5. Modelo de Datos

### Diagrama de Entidades

```
Usuario (1) ──── (0..1) Paciente
Usuario (1) ──── (0..1) Medico
Medico  (1) ──── (1)    ConfiguracionMedico
Paciente (1) ─── (N)    Cita
Medico   (1) ─── (N)    Cita
Cita     (1) ─── (N)    HistorialCambiosCita
Cita     (1) ─── (0..1) HistoriaClinica
Usuario  (1) ─── (N)    Auditoria
```

### Entidades Principales

### Usuario

| Campo | Tipo | Notas |
| --- | --- | --- |
| id | BIGINT PK | — |
| username | VARCHAR(50) | UNIQUE |
| password | VARCHAR(255) | BCrypt |
| rol | VARCHAR(20) | PACIENTE / AGENDADOR / MEDICO_TERAPISTA / ADMINISTRADOR |
| estado | VARCHAR(20) | ACTIVO / INACTIVO |
| created_at / updated_at | TIMESTAMP | — |

### Paciente

| Campo | Tipo | Notas |
| --- | --- | --- |
| id | BIGINT PK | — |
| usuario_id | BIGINT FK | Nullable (solo si tiene cuenta web) |
| documento | VARCHAR(15) | UNIQUE |
| nombres / apellidos | VARCHAR(100) | Obligatorios |
| celular | VARCHAR(10) | 10 dígitos |
| genero | VARCHAR(20) | MASCULINO / FEMENINO / OTRO |
| fecha_nacimiento | DATE | Opcional |
| correo | VARCHAR(100) | Opcional |

### Medico

| Campo | Tipo | Notas |
| --- | --- | --- |
| id | BIGINT PK | — |
| usuario_id | BIGINT FK | — |
| nombres / apellidos | VARCHAR(100) | — |
| tipo | VARCHAR(20) | MEDICO / TERAPISTA |
| especialidad | VARCHAR(50) | TERAPIA_NEURAL / QUIROPRAXIA / FISIOTERAPIA |
| estado | VARCHAR(20) | Solo ACTIVO puede recibir citas |

### ConfiguracionMedico

| Campo | Tipo | Notas |
| --- | --- | --- |
| medico_id | BIGINT FK UNIQUE | Relación 1:1 |
| dias_atencion | JSON | Ej: `["LUNES","MARTES","JUEVES","VIERNES"]` |
| hora_inicio / hora_fin | TIME | Franja de atención |
| intervalo_minutos | INTEGER | Minutos entre citas |

> **Fórmula capacidad diaria:** `(hora_fin - hora_inicio en min) / intervalo`
> 
> 
> Ej: (12:00 - 07:00) = 300 min / 15 = **20 citas/día**
> 

### Cita

| Campo | Tipo | Notas |
| --- | --- | --- |
| paciente_id / medico_id | BIGINT FK | — |
| fecha / hora | DATE / TIME | — |
| estado | VARCHAR(20) | PROGRAMADA / CONFIRMADA / ATENDIDA / CANCELADA |
| observaciones | TEXT | Opcional |
| creado_por | BIGINT FK | Auditoría |

**Constraint único:** `(medico_id, fecha, hora)` donde `estado != CANCELADA`

**Índice compuesto:** `(medico_id, fecha)` para consultas de agenda

### HistorialCambiosCita

Registra cada **reagendamiento** de una cita (RF5): fecha/hora/médico anterior y nuevo, motivo (obligatorio) y usuario que modificó. **No es historia clínica** — es exclusivamente el log de cambios de programación.

### HistoriaClinica

Registra el **control médico** realizado durante una cita atendida (RF7). Solo accesible para rol MEDICO_TERAPISTA.

| Campo | Tipo | Notas |
| --- | --- | --- |
| id | BIGINT PK | — |
| cita_id | BIGINT FK UNIQUE | Relación 1:1 con Cita |
| medico_id | BIGINT FK | Profesional que realizó el control |
| fecha_atencion | TIMESTAMP | Fecha y hora del control |
| descripcion | TEXT | Descripción del procedimiento (obligatorio) |
| created_at | TIMESTAMP | — |

**Índice:** `cita_id` UNIQUE (una sola historia clínica por cita).

**Restricción de acceso:** solo MEDICO_TERAPISTA puede crear o consultar registros clínicos.

### ConfiguracionGlobal

Tabla clave-valor para parámetros del sistema:

- `VENTANA_AGENDAMIENTO_SEMANAS` → `4`
- `LIMITE_CITAS_FUTURAS_PACIENTE` → `3`
- `HORARIO_MIN_ANTICIPACION_HORAS` → `3`

### Auditoria

Registra: `usuario_id`, `accion`, `entidad`, `entidad_id`, `detalles (JSON)`, `ip_address`, `timestamp`.

---

## 6. Seguridad y Autenticación

### JWT (Stateless)

**Algoritmo:** HMAC-SHA512

**Expiración:** 24 horas

**Secret:** Variable de entorno (mínimo 512 bits)

**Payload del token:**

json

```json
{
  "sub": "maria.gonzalez",
  "userId": 42,
  "rol": "AGENDADOR",
  "iat": 1709827200,
  "exp": 1709913600
}
```

### Matriz de Permisos (RBAC)

| Endpoint | Paciente | Agendador | Médico | Admin |
| --- | --- | --- | --- | --- |
| `POST /auth/login` | ✅ | ✅ | ✅ | ✅ |
| `POST /auth/register/paciente` | ✅ | ❌ | ❌ | ❌ |
| `GET /citas/agenda` | ❌ | ✅ | ✅ | ✅ |
| `POST /citas/manual` | ❌ | ✅ | ✅ | ❌ |
| `PUT /citas/{id}/reagendar` | ❌ | ✅ | ✅ | ❌ |
| `GET /citas/exportar` | ❌ | ✅ | ✅ | ✅ |
| `POST /citas/autonomo` | ✅ | ❌ | ❌ | ❌ |
| `GET /citas/mis-citas` | ✅ | ❌ | ❌ | ❌ |
| `GET /medicos` | ✅ | ✅ | ✅ | ✅ |
| `POST /medicos` | ❌ | ❌ | ❌ | ✅ |
| `PUT /medicos/{id}/configuracion` | ❌ | ❌ | ❌ | ✅ |
| `POST /citas/{id}/historia-clinica` | ❌ | ❌ | ✅ | ❌ |
| `GET /citas/{id}/historia-clinica` | ❌ | ❌ | ✅ | ❌ |
| `GET /reportes/**` | ❌ | ✅ | ❌ | ✅ |
| `GET/POST /usuarios` | ❌ | ❌ | ❌ | ✅ |

**Implementación:** `@PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA')")` en controllers.

### Contraseñas

- Algoritmo: BCrypt, cost factor 12
- Requisitos: mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número

### Endpoints Públicos

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register/paciente`
- `GET /swagger-ui/**` y `/v3/api-docs/**`

---

## 7. API REST — Especificación de Endpoints

**Base URL:** `http://localhost:8080/api/v1`

**Header requerido:** `Authorization: Bearer <jwt_token>`

**Formato:** JSON

### Códigos HTTP

| Código | Uso |
| --- | --- |
| 200 OK | Operación exitosa |
| 201 Created | Recurso creado |
| 400 Bad Request | Datos inválidos |
| 401 Unauthorized | Token inválido/ausente |
| 403 Forbidden | Sin permisos |
| 404 Not Found | Recurso no encontrado |
| 409 Conflict | Conflicto de concurrencia |
| 422 Unprocessable Entity | Regla de negocio violada |

**Estructura de error estándar:**

json

```json
{
  "status": 400,
  "message": "Descripción del error",
  "timestamp": "2026-03-10T10:30:00",
  "errors": { "campo": "mensaje de validación" }
}
```

---

### Autenticación

### `POST /auth/login`

**Body:**

json

```json
{ "username": "maria.gonzalez", "password": "Password123" }
```

**Response 200:**

json

```json
{
  "token": "eyJ...",
  "type": "Bearer",
  "userId": 42,
  "username": "maria.gonzalez",
  "rol": "AGENDADOR",
  "expiresIn": 86400
}
```

### `POST /auth/register/paciente`

**Body:** documento, nombres, apellidos, celular (10 dígitos), genero, fechaNacimiento (opcional), correo, username, password.

**Response 201:** token JWT + mensaje de verificación de correo.

---

### Gestión de Citas

### `GET /citas/agenda` — RF1

**Roles:** AGENDADOR, MEDICO_TERAPISTA, ADMINISTRADOR

**Params:** `medicoId` (Long), `fecha` (yyyy-MM-dd)

**Response 200 (resumen):**

json

```json
{
  "medico": { "id": 1, "nombresCompletos": "Clara Inés Córdoba", "especialidad": "TERAPIA_NEURAL", "intervaloMinutos": 15 },
  "fecha": "2026-02-20",
  "citas": [
    {
      "id": 101, "hora": "07:00:00",
      "paciente": { "nombresCompletos": "Juan Pérez", "documento": "1234567890", "celular": "3001234567", "edad": 40 },
      "estado": "PROGRAMADA"
    }
  ],
  "horariosDisponibles": [
    { "hora": "07:00:00", "disponible": false },
    { "hora": "07:30:00", "disponible": true }
  ],
  "totalCitas": 12,
  "capacidadDiaria": 20,
  "porcentajeOcupacion": 60
}
```

---

### `POST /citas/manual` — RF2

**Roles:** AGENDADOR, MEDICO_TERAPISTA

**Body:**

json

```json
{
  "medicoId": 1,
  "fecha": "2026-02-20",
  "hora": "08:00:00",
  "observaciones": "Dolor lumbar crónico.",
  "pacienteDocumento": "1234567890",
  "datosPaciente": {
    "documento": "1234567890",
    "nombres": "Juan Carlos", "apellidos": "Pérez Gómez",
    "celular": "3001234567", "genero": "MASCULINO",
    "fechaNacimiento": "1985-03-15", "correo": "juan@email.com"
  }
}
```

**Lógica:** si el paciente con ese documento ya existe, se reutiliza; si no, se crea.

**Validaciones:**

- Fecha futura, médico ACTIVO
- Horario dentro de franja de atención y múltiplo del intervalo
- Día de semana en días de atención
- Horario no ocupado

**Response 201:** datos de la cita creada con información del paciente y médico.

**Errores 422 posibles:** "El horario seleccionado ya está ocupado", "El médico no está activo", "El horario está fuera de la franja de atención", "El médico no atiende los Miércoles".

---

### `POST /citas/autonomo` — RF3

**Roles:** PACIENTE

**Body:** `medicoId`, `fecha`, `hora`, `observaciones` (opcional)

**Validaciones adicionales a RF2:**

- Cuenta de paciente verificada por email
- Máximo 3 citas futuras activas
- Fecha dentro de ventana de agendamiento configurada
- Validación atómica de concurrencia al guardar

**Response 409 (concurrencia):** "Este horario fue reservado por otro paciente mientras completabas tu registro."

---

### `GET /citas/autonomo/disponibilidad`

**Roles:** PACIENTE

**Params:** `medicoId`, `fecha`

**Response:** lista de slots con `hora` y `disponible: true/false`, más info de ventana de agendamiento.

---

### Gestión de Médicos

### `GET /medicos`

**Roles:** Todos

**Params opcionales:** `especialidad`

**Response:** lista de médicos activos con `intervaloMinutos` y `proximaDisponibilidad`.

### `PUT /medicos/{medicoId}/configuracion` — RF4

**Roles:** ADMINISTRADOR

**Body:**

json

```json
{
  "diasAtencion": ["LUNES", "MARTES", "JUEVES", "VIERNES"],
  "horaInicio": "07:00:00",
  "horaFin": "12:00:00",
  "intervaloMinutos": 15
}
```

**Validaciones:** `horaFin > horaInicio`, jornada entre 2 y 8 horas, intervalo en valores válidos.

**Response 200:** incluye `capacidadDiaria` calculada. Cambios aplican solo a citas futuras.

---

### Configuración del Sistema — RF4

### `PUT /configuracion/ventana-agendamiento`

**Roles:** ADMINISTRADOR | **Body:** `{ "semanas": 4 }` (mín 1, máx 12)

### `POST /configuracion/dias-no-laborables`

**Roles:** ADMINISTRADOR

**Body:** `fecha`, `descripcion`, `aplicaTodos` (bool), `medicosIds` (requerido si `aplicaTodos = false`)

---

### Gestión de Usuarios

### `GET /usuarios` — ADMINISTRADOR

**Params opcionales:** `rol`, `estado`

### `POST /usuarios` — ADMINISTRADOR

**Body:** `username`, `password`, `rol`, `estado`

---

### Endpoints Esbozados — Sprints Posteriores

Los siguientes endpoints están contemplados en la arquitectura y el modelo de datos desde sprint 1. Su implementación completa corresponde a iteraciones posteriores.

### `PUT /citas/{id}/reagendar` — RF5

**Roles:** AGENDADOR, MEDICO_TERAPISTA

**Body:** `nuevaFecha`, `nuevaHora`, `nuevoMedicoId` (opcional), `motivo` (obligatorio)

**Lógica:** valida disponibilidad del nuevo horario, guarda registro en `HistorialCambiosCita` con estado anterior y nuevo, registra en auditoría.

**Response 200:** cita actualizada con historial de cambios.

### `GET /citas/exportar` — RF6

**Roles:** AGENDADOR, MEDICO_TERAPISTA, ADMINISTRADOR

**Params:** `medicoId`, `fecha`

**Response:** archivo CSV descargable con columnas: Hora, Nombre paciente, Documento, Celular, Observación.

**Content-Type:** `text/csv; charset=UTF-8`

### `POST /citas/{id}/historia-clinica` — RF7

**Roles:** MEDICO_TERAPISTA

**Body:** `descripcion` (obligatorio)

**Restricción:** solo puede registrarse si la cita tiene estado ATENDIDA.

**Response 201:** registro clínico creado con fecha/hora automática y médico autenticado.

### `GET /citas/{id}/historia-clinica` — RF7

**Roles:** MEDICO_TERAPISTA

**Response 200:** datos del control clínico asociado a la cita.

### `GET /reportes/citas-por-mes` — RF10

**Roles:** AGENDADOR, ADMINISTRADOR

**Params:** `anio`

**Response:** cantidad de citas agrupadas por mes y por médico.

### `GET /reportes/citas-por-medico` — RF10

**Roles:** AGENDADOR, ADMINISTRADOR

**Params:** `fechaInicio`, `fechaFin`

**Response:** totales por médico/terapista en el rango indicado.

### `GET /reportes/citas-por-especialidad` — RF10

**Roles:** AGENDADOR, ADMINISTRADOR

**Params:** `fechaInicio`, `fechaFin`

**Response:** comparativo por especialidad (TERAPIA_NEURAL, QUIROPRAXIA, FISIOTERAPIA).

---

## 8. Flujos de Usuario

### Agendador de Citas

```
Login → GET /medicos → GET /citas/agenda
      → POST /citas/manual          (crear cita)
      → PUT /citas/{id}/reagendar   (reagendar cita existente)
      → GET /citas/exportar         (exportar agenda del día a CSV)
```

### Médico/Terapista

```
Login → GET /citas/agenda (su id + fecha=HOY)
      → GET /citas/{id}
      → POST /citas/manual (ocasional)
      → PUT /citas/{id}/reagendar
      → POST /citas/{id}/historia-clinica  (después de atender)
      → GET /citas/{id}/historia-clinica
```

### Paciente

```
POST /auth/register/paciente → verificar email → Login
→ GET /medicos?especialidad=X → GET /citas/autonomo/disponibilidad
→ POST /citas/autonomo → GET /citas/mis-citas
```

### Administrador

```
Login → GET/POST /usuarios → GET/POST /medicos
      → PUT /medicos/{id}/configuracion
      → PUT /configuracion/ventana-agendamiento
      → POST /configuracion/dias-no-laborables
      → GET /reportes/citas-por-mes
      → GET /reportes/citas-por-medico
      → GET /reportes/citas-por-especialidad
```

---

## 9. Consideraciones Técnicas

### Transacciones

- Anotación `@Transactional` en servicios; `@Transactional(readOnly=true)` para consultas
- Nivel de aislamiento: `READ COMMITTED` (default PostgreSQL)
- Rollback automático ante excepciones no controladas

### Concurrencia (doble agendamiento)

**Estrategia lock optimista:**

1. Consultar disponibilidad (sin lock)
2. Re-validar dentro de la transacción al guardar
3. Constraint único en BD rechaza INSERT duplicado → HTTP 409

### Índices Clave

| Tabla | Índice | Propósito |
| --- | --- | --- |
| citas | `(medico_id, fecha)` | Consultas de agenda (RF1) |
| citas | `paciente_id`, `estado` | Citas del paciente, filtro |
| pacientes | `documento` UNIQUE | Búsqueda rápida por documento |
| usuarios | `username` UNIQUE | Login |
| medicos | `estado`, `especialidad` | Filtros de listado |
| historia_clinica | `cita_id` UNIQUE | Una historia por cita, acceso directo |
| auditoria | `(entidad, entidad_id)`, `timestamp` | Consultas de auditoría |

### Validaciones por Capa

- **DTO:** formato (`@NotNull`, `@Size`, `@Pattern`) — Jakarta Bean Validation
- **Service:** reglas de negocio complejas (médico activo, horario válido, límite citas)
- **BD:** constraints de integridad, UNIQUE, CHECK

### Manejo de Errores

```
RuntimeException
├── ResourceNotFoundException     → 404
├── BusinessRuleException         → 422
│   ├── HorarioOcupadoException
│   ├── MedicoInactivoException
│   ├── LimiteCitasExcedidoException
│   └── FechaInvalidaException
├── UnauthorizedException         → 401
└── ForbiddenException            → 403
```

Handler global con `@RestControllerAdvice`.

### Configuración de Entornos

Perfiles Spring Boot: `dev`, `test`, `prod`.

Variables de entorno críticas: `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SMTP_HOST`.

---

## 10. Estado de Cumplimiento de Requisitos

| RF | Descripción | Estado | Sprint |
| --- | --- | --- | --- |
| **RF1** | Listar citas por médico/fecha | ✅ Completo | 1 |
| **RF2** | Crear cita manual | ✅ Completo | 1 |
| **RF8** | Gestión de usuarios del sistema | ✅ Completo | 1 |
| **RF9** | Auditoría (transversal) | ✅ Activo desde sprint 1 | 1 |
| **RF3** | Agendamiento autónomo | 🔄 En progreso | 2 |
| **RF4** | Configuración de parámetros | 🔄 En progreso | 2 |
| **RF5** | Re-agendamiento de citas | 🔄 Modelo listo | 2 |
| **RF6** | Exportación de citas (CSV) | 🔄 Diseñado | 2 |
| **RF7** | Historia clínica básica | 🔄 Modelo listo | 3 |
| **RF10** | Reportes y estadísticas | 🔄 Diseñado | 3 |

**Infraestructura lista para RF5–RF10:** el modelo de datos, los módulos y la seguridad están definidos desde sprint 1. Implementar estos requisitos no requiere cambios arquitectónicos, solo agregar lógica de negocio y endpoints dentro de los módulos AGENDA y REPORTES ya existentes.

---

## 11. Decisiones Arquitectónicas

| Decisión | Elección | Razón |
| --- | --- | --- |
| Patrón | Monolito modular | Simplicidad, equipo pequeño, volumen manejable; módulos con límites claros permiten evolución sin reescritura |
| Número de módulos | 4 (SHARED, AUTH, AGENDA, REPORTES) | Granularidad por cohesión de dominio: evita acoplamiento distribuido innecesario entre entidades que solo existen juntas |
| Autenticación | JWT stateless | Escalabilidad futura, no requiere estado en servidor, compatible con apps móviles |
| BD | PostgreSQL | JSON nativo (dias_atencion), ACID robusto, open source |
| Concurrencia | Lock optimista + constraint único | Mejor performance que lock pesimista; conflictos de doble agendamiento son raros |
| Historia clínica separada de historial de cambios | Entidades distintas (`HistoriaClinica` vs `HistorialCambiosCita`) | Responsabilidades diferentes: una es registro clínico médico, la otra es log de reagendamientos administrativos |

---

## 12. Recomendaciones

### Roadmap por Sprint

**Sprint 1:** RF1 (listar citas), RF2 (cita manual), RF8 (gestión usuarios), RF9 (auditoría activa desde el inicio).

**Sprint 2:** RF3 (agendamiento autónomo), RF4 (configuración parámetros), RF5 (reagendamiento), RF6 (exportación CSV).

**Sprint 3:** RF7 (historia clínica), RF10 (reportes y estadísticas), tests automatizados (objetivo: >70% cobertura).

### Operación y Despliegue

- Dockerizar la aplicación con Docker Compose (app + PostgreSQL)
- Migraciones de BD con Flyway o Liquibase desde sprint 1
- Perfiles Spring Boot `dev` / `test` / `prod` para configuración por entorno

### Evolución Futura

- Apps móviles → la API REST ya está preparada; considerar push notifications para confirmaciones
- Múltiples centros médicos → multi-tenancy añadiendo campo `centro_id` a entidades principales; la arquitectura modular lo soporta sin rediseño

---

**Fin del Documento**

*Versión 2.0 · Marzo 2026 · Ingeniería de Software III — Universidad del Cauca*