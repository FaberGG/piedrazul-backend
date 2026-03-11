# Guía del Desarrollador — Sprint 1: RF1 y RF2

**Proyecto:** Sistema de Agendamiento Piedrazul  
**Alcance:** RF1 (Listar Citas de Médico por Fecha) + RF2 (Crear Cita Manual)  
**Referencia:** [ARCH-DOC.md](./ARCH-DOC.md)

---

## 1. Antes de Empezar

### 1.1 Prerrequisitos

- **Java 17** (LTS) instalado y configurado en `JAVA_HOME`
- **Maven 3.9+** (o usar el wrapper `./mvnw` incluido)
- **Docker y Docker Compose** (para la base de datos)
- **IDE:** IntelliJ IDEA (recomendado) con plugins de Lombok y Spring Boot
- **Git** configurado

### 1.2 Levantar la Base de Datos

```bash
# Desde la raíz del proyecto
docker compose up -d
```

Esto levanta:
- **PostgreSQL 15** en `localhost:5432` (DB: `piedrazul_dev`, user: `piedrazul_user`, pass: `piedrazul_pass`)
- **pgAdmin** en `http://localhost:5050` (email: `admin@piedrazul.com`, pass: `admin`)

Verificar que esté corriendo:
```bash
docker compose ps
```

### 1.3 Ejecutar la Aplicación

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

El perfil `dev` está activo por defecto (`application-dev.yml`), con `ddl-auto: create-drop` que genera las tablas automáticamente al arrancar.

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## 2. Entender la Estructura del Proyecto

```
src/main/java/com/piedrazul/backend/
├── PiedrazulBackendApplication.java       ← NO TOCAR — clase main
│
├── shared/                                ← módulo transversal (ya creado)
│   ├── security/   → JwtService, JwtAuthFilter, SecurityConfig
│   ├── audit/      → Auditoria (entidad), AuditService
│   ├── exception/  → GlobalExceptionHandler, excepciones personalizadas
│   └── dto/        → ErrorResponse
│
├── auth/                                  ← autenticación (ya creado)
│   ├── controller/ → AuthController
│   ├── service/    → AuthService
│   ├── domain/     → Usuario (entidad)
│   ├── repository/ → UsuarioRepository
│   └── dto/        → LoginRequest, AuthResponse, RegisterPacienteRequest
│
├── agenda/                                ← ⭐ MÓDULO PRINCIPAL — aquí trabajarás
│   ├── controller/ → CitaController, MedicoController
│   ├── service/    → CitaService, DisponibilidadService, MedicoService
│   ├── domain/     → Cita, Paciente, Medico, ConfiguracionMedico, ...
│   ├── repository/ → CitaRepository, PacienteRepository, MedicoRepository
│   └── dto/        → CrearCitaManualRequest, AgendaResponse, CitaResponse
│
└── reportes/                              ← no se toca en sprint 1
```

### Regla de Dependencia

```
SHARED ← AUTH ← AGENDA
```

- **AGENDA** puede usar clases de `shared.*` y consultar `auth.domain.Usuario`.
- **AGENDA** NO debe importar nada de `reportes/`.
- Toda lógica de negocio va en `service/`, nunca en `controller/`.

---

## 3. RF1 — Listar Citas de Médico por Fecha

### 3.1 ¿Qué debe hacer?

**Endpoint:** `GET /api/v1/citas/agenda?medicoId={id}&fecha={yyyy-MM-dd}`  
**Roles:** AGENDADOR, MEDICO_TERAPISTA, ADMINISTRADOR  
**Response:** JSON con las citas del día, horarios disponibles/ocupados y porcentaje de ocupación.

### 3.2 Archivos a Modificar

| Archivo | Qué hacer |
|---------|-----------|
| `agenda/service/DisponibilidadService.java` | Implementar la lógica de generación de slots |
| `agenda/service/CitaService.java` | Implementar `listarAgendaMedico()` |
| `agenda/controller/CitaController.java` | Ya tiene el endpoint definido, solo quitar el `// TODO` |
| `agenda/repository/CitaRepository.java` | Ya tiene `findByMedicoIdAndFecha()`, agregar queries si es necesario |

### 3.3 Paso a Paso

#### Paso 1: Implementar `DisponibilidadService.calcularHorariosDisponibles()`

Este método es el corazón de RF1. Debe:

1. Obtener la `ConfiguracionMedico` del médico (via `MedicoRepository`)
2. Verificar que el día de la semana esté en `diasAtencion`
3. Generar todos los slots desde `horaInicio` hasta `horaFin` con saltos de `intervaloMinutos`
4. Consultar las citas existentes para esa fecha (`CitaRepository.findByMedicoIdAndFecha()`)
5. Marcar cada slot como disponible u ocupado

**Ejemplo de lógica para generar slots:**

```java
// Pseudocódigo — implementar en DisponibilidadService
public List<LocalTime> generarTodosLosSlots(ConfiguracionMedico config) {
    List<LocalTime> slots = new ArrayList<>();
    LocalTime current = config.getHoraInicio();
    while (current.isBefore(config.getHoraFin())) {
        slots.add(current);
        current = current.plusMinutes(config.getIntervaloMinutos());
    }
    return slots;
}
```

**Fórmula de capacidad diaria:**  
`(horaFin - horaInicio en minutos) / intervaloMinutos`  
Ejemplo: (12:00 - 07:00) = 300 min / 15 = **20 citas/día**

#### Paso 2: Implementar `CitaService.listarAgendaMedico()`

```java
// Pseudocódigo — implementar en CitaService
@Transactional(readOnly = true)
public AgendaResponse listarAgendaMedico(Long medicoId, LocalDate fecha) {
    // 1. Buscar médico o lanzar ResourceNotFoundException
    Medico medico = medicoRepository.findById(medicoId)
        .orElseThrow(() -> new ResourceNotFoundException("Médico", medicoId));

    // 2. Obtener citas del día
    List<Cita> citas = citaRepository.findByMedicoIdAndFecha(medicoId, fecha);

    // 3. Obtener horarios disponibles
    List<LocalTime> disponibles = disponibilidadService
        .calcularHorariosDisponibles(medicoId, fecha);

    // 4. Calcular capacidad total y ocupación
    ConfiguracionMedico config = medico.getConfiguracion();
    int totalSlots = /* calcular con fórmula */;
    int ocupados = citas.size();
    double porcentaje = (ocupados * 100.0) / totalSlots;

    // 5. Mapear citas a CitaResponse (por ahora manualmente, después con MapStruct)
    List<CitaResponse> citasDto = citas.stream()
        .map(this::mapToCitaResponse)
        .toList();

    // 6. Construir y retornar AgendaResponse
    return AgendaResponse.builder()
        .medicoId(medicoId)
        .medicoNombre(medico.getNombres() + " " + medico.getApellidos())
        .especialidad(medico.getEspecialidad())
        .fecha(fecha)
        .citas(citasDto)
        .horariosDisponibles(disponibles.stream().map(LocalTime::toString).toList())
        .totalSlots(totalSlots)
        .slotsOcupados(ocupados)
        .porcentajeOcupacion(porcentaje)
        .build();
}
```

#### Paso 3: Inyectar Dependencias en los Servicios

En `CitaService`, necesitarás inyectar:

```java
private final CitaRepository citaRepository;
private final MedicoRepository medicoRepository;
private final PacienteRepository pacienteRepository;
private final DisponibilidadService disponibilidadService;

// Constructor con inyección
```

En `DisponibilidadService`, necesitarás:

```java
private final MedicoRepository medicoRepository;
private final CitaRepository citaRepository;
```

#### Paso 4: Verificar el Controller

El `CitaController` ya tiene el endpoint definido correctamente:

```java
@GetMapping("/agenda")
@PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'ADMINISTRADOR')")
public ResponseEntity<AgendaResponse> listarAgenda(
        @RequestParam Long medicoId,
        @RequestParam LocalDate fecha) {
    return ResponseEntity.ok(citaService.listarAgendaMedico(medicoId, fecha));
}
```

Solo asegúrate de quitar el comentario `// TODO`.

### 3.4 Probar RF1

```bash
# Usando curl (necesita token JWT — por ahora puedes desactivar seguridad temporalmente)
curl "http://localhost:8080/api/v1/citas/agenda?medicoId=1&fecha=2026-03-15"
```

**Respuesta esperada (según ARCH-DOC):**
```json
{
  "medicoId": 1,
  "medicoNombre": "Clara Inés Córdoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-03-15",
  "citas": [
    {
      "id": 101,
      "pacienteNombre": "Juan Pérez",
      "pacienteDocumento": "1234567890",
      "hora": "07:00:00",
      "estado": "PROGRAMADA"
    }
  ],
  "horariosDisponibles": ["07:30:00", "08:00:00", "..."],
  "totalSlots": 20,
  "slotsOcupados": 12,
  "porcentajeOcupacion": 60.0
}
```

---

## 4. RF2 — Crear Cita Manual

### 4.1 ¿Qué debe hacer?

**Endpoint:** `POST /api/v1/citas/manual`  
**Roles:** AGENDADOR, MEDICO_TERAPISTA  
**Body:** datos del paciente + datos de la cita  
**Lógica clave:** si el paciente (por documento) ya existe, se reutiliza; si no, se crea.

### 4.2 Archivos a Modificar

| Archivo | Qué hacer |
|---------|-----------|
| `agenda/service/CitaService.java` | Implementar `crearCitaManual()` |
| `agenda/service/DisponibilidadService.java` | Usar `estaDisponible()` para validar |
| `agenda/controller/CitaController.java` | Ya tiene el endpoint definido |
| `agenda/dto/CrearCitaManualRequest.java` | Ya tiene las validaciones Bean Validation |

### 4.3 Paso a Paso

#### Paso 1: Entender el DTO de entrada

`CrearCitaManualRequest.java` ya tiene definido todo lo necesario:

- **Datos del paciente:** documento, nombres, apellidos, celular (10 dígitos), género, fechaNacimiento (opcional), correo (opcional)
- **Datos de la cita:** medicoId, fecha, hora, observaciones (opcional)

Las validaciones de formato (`@NotBlank`, `@Pattern`, `@Email`) ya están en el DTO. Spring las ejecuta automáticamente por el `@Valid` en el controller.

#### Paso 2: Implementar `CitaService.crearCitaManual()`

Este método debe ejecutar **5 validaciones de negocio** antes de crear la cita:

```java
// Pseudocódigo — implementar en CitaService
@Transactional
public CitaResponse crearCitaManual(CrearCitaManualRequest request) {

    // ── 1. Buscar o crear paciente ──
    Paciente paciente = pacienteRepository.findByDocumento(request.getDocumento())
        .orElseGet(() -> crearNuevoPaciente(request));

    // ── 2. Validar que el médico existe y está ACTIVO ──
    Medico medico = medicoRepository.findById(request.getMedicoId())
        .orElseThrow(() -> new ResourceNotFoundException("Médico", request.getMedicoId()));

    if (!"ACTIVO".equals(medico.getEstado())) {
        throw new BusinessRuleException("El médico no está activo");
    }

    // ── 3. Validar que la fecha es futura ──
    if (!request.getFecha().isAfter(LocalDate.now())) {
        throw new BusinessRuleException("La fecha debe ser futura");
    }

    // ── 4. Validar día de atención y franja horaria ──
    ConfiguracionMedico config = medico.getConfiguracion();
    LocalTime hora = LocalTime.parse(request.getHora());

    // Verificar que el día de la semana está en diasAtencion
    String diaSemana = request.getFecha().getDayOfWeek().name(); // MONDAY, TUESDAY...
    // Mapear al español y comparar con config.getDiasAtencion()

    // Verificar que la hora está dentro de la franja
    if (hora.isBefore(config.getHoraInicio()) || !hora.isBefore(config.getHoraFin())) {
        throw new BusinessRuleException("El horario está fuera de la franja de atención");
    }

    // Verificar que la hora es múltiplo del intervalo
    long minutosDesdeInicio = Duration.between(config.getHoraInicio(), hora).toMinutes();
    if (minutosDesdeInicio % config.getIntervaloMinutos() != 0) {
        throw new BusinessRuleException("El horario no corresponde a un slot válido");
    }

    // ── 5. Validar que el horario NO está ocupado ──
    if (!disponibilidadService.estaDisponible(medico.getId(), request.getFecha(), hora)) {
        throw new BusinessRuleException("El horario seleccionado ya está ocupado");
    }

    // ── 6. Crear y persistir la cita ──
    Cita cita = Cita.builder()
        .paciente(paciente)
        .medico(medico)
        .fecha(request.getFecha())
        .hora(hora)
        .estado("PROGRAMADA")
        .observaciones(request.getObservaciones())
        .creadoPor(/* ID del usuario autenticado */)
        .build();

    cita = citaRepository.save(cita);

    // ── 7. Registrar auditoría ──
    // auditService.registrar(...)

    // ── 8. Mapear a DTO y retornar ──
    return mapToCitaResponse(cita);
}
```

#### Paso 3: Método auxiliar para crear paciente

```java
private Paciente crearNuevoPaciente(CrearCitaManualRequest request) {
    Paciente paciente = Paciente.builder()
        .documento(request.getDocumento())
        .nombres(request.getNombres())
        .apellidos(request.getApellidos())
        .celular(request.getCelular())
        .genero(request.getGenero())
        .fechaNacimiento(request.getFechaNacimiento())
        .correo(request.getCorreo())
        .build();
    return pacienteRepository.save(paciente);
}
```

#### Paso 4: Implementar `DisponibilidadService.estaDisponible()`

```java
public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
    List<Cita> citasDelDia = citaRepository.findByMedicoIdAndFecha(medicoId, fecha);
    return citasDelDia.stream()
        .filter(c -> !"CANCELADA".equals(c.getEstado()))
        .noneMatch(c -> c.getHora().equals(hora));
}
```

#### Paso 5: Método de mapeo (temporal, después usar MapStruct)

```java
private CitaResponse mapToCitaResponse(Cita cita) {
    return CitaResponse.builder()
        .id(cita.getId())
        .pacienteNombre(cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos())
        .pacienteDocumento(cita.getPaciente().getDocumento())
        .medicoNombre(cita.getMedico().getNombres() + " " + cita.getMedico().getApellidos())
        .especialidad(cita.getMedico().getEspecialidad())
        .fecha(cita.getFecha())
        .hora(cita.getHora())
        .estado(cita.getEstado())
        .observaciones(cita.getObservaciones())
        .build();
}
```

### 4.4 Probar RF2

```bash
curl -X POST http://localhost:8080/api/v1/citas/manual \
  -H "Content-Type: application/json" \
  -d '{
    "documento": "1234567890",
    "nombres": "Juan Carlos",
    "apellidos": "Pérez Gómez",
    "celular": "3001234567",
    "genero": "MASCULINO",
    "fechaNacimiento": "1985-03-15",
    "correo": "juan@email.com",
    "medicoId": 1,
    "fecha": "2026-03-20",
    "hora": "08:00:00",
    "observaciones": "Dolor lumbar crónico."
  }'
```

**Respuesta esperada (201 Created):**
```json
{
  "id": 1,
  "pacienteNombre": "Juan Carlos Pérez Gómez",
  "pacienteDocumento": "1234567890",
  "medicoNombre": "Clara Inés Córdoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-03-20",
  "hora": "08:00:00",
  "estado": "PROGRAMADA",
  "observaciones": "Dolor lumbar crónico."
}
```

**Errores posibles (422):**
```json
{ "status": 422, "message": "El horario seleccionado ya está ocupado" }
{ "status": 422, "message": "El médico no está activo" }
{ "status": 422, "message": "El horario está fuera de la franja de atención" }
{ "status": 422, "message": "El médico no atiende los Miércoles" }
```

---

## 5. Preparación de Seguridad (Mínima para Sprint 1)

Para probar RF1 y RF2 sin bloqueos, hay dos opciones:

### Opción A: Desactivar seguridad temporalmente (solo desarrollo)

En `SecurityConfig.java`, permitir todo temporalmente:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
}
```

### Opción B: Implementar seguridad mínima (recomendado)

1. Implementar `JwtService` con generación/validación de token
2. Implementar `JwtAuthFilter` para extraer token del header `Authorization: Bearer <token>`
3. Configurar `SecurityConfig` con endpoints públicos (`/auth/**`, `/swagger-ui/**`) y protegidos
4. Implementar `AuthService.login()` para obtener un token válido

> ⚠️ Si eliges Opción A, recuerda quitar el `@PreAuthorize` de los controllers o no funcionará el `hasAnyRole()` sin un `SecurityContext` configurado.

---

## 6. Datos de Prueba

Para probar los endpoints necesitas datos precargados. Puedes crear un `DataLoader` temporal:

**Crear archivo:** `src/main/java/com/piedrazul/backend/shared/DataLoader.java`

```java
@Component
@Profile("dev")
public class DataLoader implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MedicoRepository medicoRepository;
    // ...otros repos

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Crear usuario para el médico
        Usuario usuarioMedico = Usuario.builder()
            .username("dra.cordoba")
            .password(passwordEncoder.encode("Password123"))
            .rol("MEDICO_TERAPISTA")
            .estado("ACTIVO")
            .build();
        usuarioMedico = usuarioRepository.save(usuarioMedico);

        // 2. Crear médico
        Medico medico = Medico.builder()
            .usuario(usuarioMedico)
            .nombres("Clara Inés")
            .apellidos("Córdoba")
            .tipo("TERAPISTA")
            .especialidad("TERAPIA_NEURAL")
            .estado("ACTIVO")
            .build();
        medico = medicoRepository.save(medico);

        // 3. Crear configuración del médico
        ConfiguracionMedico config = ConfiguracionMedico.builder()
            .medico(medico)
            .diasAtencion("[\"LUNES\",\"MARTES\",\"JUEVES\",\"VIERNES\"]")
            .horaInicio(LocalTime.of(7, 0))
            .horaFin(LocalTime.of(12, 0))
            .intervaloMinutos(15)
            .build();
        // guardar config...

        // 4. Crear usuario agendador (para autenticarse y probar)
        Usuario agendador = Usuario.builder()
            .username("maria.gonzalez")
            .password(passwordEncoder.encode("Password123"))
            .rol("AGENDADOR")
            .estado("ACTIVO")
            .build();
        usuarioRepository.save(agendador);
    }
}
```

---

## 7. Orden de Implementación Recomendado

### Fase 1 — Infraestructura Base (antes de RF1/RF2)

| # | Tarea | Archivo(s) |
|---|-------|------------|
| 1 | Configurar SecurityConfig mínima (Opción A o B) | `shared/security/SecurityConfig.java` |
| 2 | Implementar GlobalExceptionHandler completo | `shared/exception/GlobalExceptionHandler.java` |
| 3 | Crear DataLoader con datos de prueba | `shared/DataLoader.java` (nuevo) |
| 4 | Verificar que la app arranca y las tablas se crean | `docker compose up` + `mvnw spring-boot:run` |

### Fase 2 — RF1 (Listar Agenda)

| # | Tarea | Archivo(s) |
|---|-------|------------|
| 5 | Implementar generación de slots | `agenda/service/DisponibilidadService.java` |
| 6 | Implementar `listarAgendaMedico()` | `agenda/service/CitaService.java` |
| 7 | Quitar `// TODO` del controller | `agenda/controller/CitaController.java` |
| 8 | Probar con curl/Postman/Swagger | — |

### Fase 3 — RF2 (Crear Cita Manual)

| # | Tarea | Archivo(s) |
|---|-------|------------|
| 9 | Implementar `estaDisponible()` | `agenda/service/DisponibilidadService.java` |
| 10 | Implementar `crearCitaManual()` con todas las validaciones | `agenda/service/CitaService.java` |
| 11 | Implementar mapeo Cita → CitaResponse | `agenda/service/CitaService.java` |
| 12 | Probar caso exitoso + cada error de validación | — |

### Fase 4 — Pulir

| # | Tarea | Archivo(s) |
|---|-------|------------|
| 13 | Agregar auditoría a las operaciones | `shared/audit/AuditService.java` |
| 14 | Escribir tests unitarios de CitaService | `test/.../agenda/CitaServiceTest.java` |
| 15 | Implementar seguridad JWT (si se eligió Opción A antes) | `shared/security/*` |

---

## 8. Errores Comunes y Soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `Table 'citas' doesn't exist` | PostgreSQL no está corriendo | `docker compose up -d` |
| `LazyInitializationException` | Acceder a relación LAZY fuera de transacción | Agregar `@Transactional(readOnly = true)` al método del servicio |
| `MethodArgumentNotValidException` | Body del request no pasa validaciones | Revisar los campos obligatorios en `CrearCitaManualRequest` |
| `403 Forbidden` | Sin token JWT o rol incorrecto | Verificar `SecurityConfig` o usar Opción A temporal |
| `NullPointerException` en `medico.getConfiguracion()` | El médico no tiene configuración cargada | Verificar DataLoader, o usar `JOIN FETCH` en la query |

---

## 9. Referencia Rápida de Archivos

### Entidades JPA que ya están definidas (solo implementar lógica)

| Entidad | Tabla | Relaciones clave |
|---------|-------|-----------------|
| `Usuario` | `usuarios` | 1:1 con Paciente, 1:1 con Medico |
| `Paciente` | `pacientes` | 1:N con Cita, 0:1 con Usuario |
| `Medico` | `medicos` | 1:N con Cita, 1:1 con ConfiguracionMedico |
| `ConfiguracionMedico` | `configuracion_medico` | 1:1 con Medico |
| `Cita` | `citas` | N:1 con Paciente, N:1 con Medico |

### Repositorios ya definidos

| Repositorio | Métodos disponibles |
|-------------|-------------------|
| `CitaRepository` | `findByMedicoIdAndFecha()`, `countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual()` |
| `PacienteRepository` | `findByDocumento()`, `existsByDocumento()` |
| `MedicoRepository` | `findByEstado()`, `findByEspecialidad()` |

### DTOs ya definidos

| DTO | Uso |
|-----|-----|
| `CrearCitaManualRequest` | Body de `POST /citas/manual` (con validaciones) |
| `AgendaResponse` | Response de `GET /citas/agenda` |
| `CitaResponse` | Representación de una cita individual |
| `ErrorResponse` | Estructura estándar de errores |

---

## 10. Checklist Final

- [ ] Docker Compose levantado (PostgreSQL corriendo)
- [ ] App arranca sin errores (`mvnw spring-boot:run`)
- [ ] Tablas creadas en la base de datos
- [ ] Datos de prueba cargados (al menos 1 médico con configuración)
- [ ] **RF1:** `GET /citas/agenda?medicoId=1&fecha=2026-03-20` retorna 200
- [ ] **RF1:** Respuesta incluye citas, horarios disponibles y porcentaje de ocupación
- [ ] **RF2:** `POST /citas/manual` con datos válidos retorna 201
- [ ] **RF2:** Paciente nuevo se crea automáticamente
- [ ] **RF2:** Paciente existente se reutiliza por documento
- [ ] **RF2:** Horario ocupado retorna 422
- [ ] **RF2:** Médico inactivo retorna 422
- [ ] **RF2:** Fecha pasada retorna 422
- [ ] **RF2:** Hora fuera de franja retorna 422
- [ ] **RF2:** Campos obligatorios faltantes retorna 400

---

*Guía creada para Sprint 1 — Marzo 2026*

