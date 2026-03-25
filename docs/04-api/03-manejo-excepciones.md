# Catálogo de Excepciones y Validaciones por Módulo

Este documento describe todas las excepciones (Reglas de Negocio, Recursos no Encontrados, Validaciones de Petición) que son capturadas y propagadas a través del `GlobalExceptionHandler` en la arquitectura del sistema Piedrazul.

Sirve como guía base para el mapeo o refactorización del manejo de estado HTTP de cara al Frontend (Status `400`, `404`, `422`, `500`).

---

## 1. Módulo Agenda (`CitaServiceImpl.java`)

Este módulo contiene la mayor parte de las reglas del negocio clínico del sistema.

### Validaciones de Búsqueda y Disponibilidad
| Tipo de Excepción | Escenario Clínico | Mensaje Mapeado (Legible) | Code |
| :--- | :--- | :--- | :--- |
| `ResourceNotFoundException` | Se solicita la agenda de un ID que no corresponde a ningún doctor. | `Medico no encontrado(a) con id: {id}` | 404 |
| `BusinessRuleException` | El sistema busca predecir el siguiente hueco pero el doctor referenciado ya no labora en la clínica. | `El medico no esta activo` | 422 |
| `BusinessRuleException` | Se busca agendar pero el doctor no tiene configuración de horario registrada. | `El medico no tiene configuracion horaria activa` | 422 |
| `BusinessRuleException` | Al pedir huecos con `/disponibilidad/primera`, el sistema no halla horarios en 30 días. | `No hay horarios disponibles para el medico en la ventana de busqueda` | 422 |
| `BusinessRuleException` | Al pedir `/disponibilidad/primera/global` y no hay ningún médico trabajando actualmente en la clínica. | `No hay medicos activos para agendar` | 422 |
| `BusinessRuleException` | Al pedir la primera disponibilidad general, el sistema no halla horarios de nadie en 30 días. | `No hay horarios disponibles en la ventana de busqueda` | 422 |

### Validaciones de Agendamiento Manual y Autónomo
| Tipo de Excepción | Escenario Clínico | Mensaje Mapeado (Legible) | Code |
| :--- | :--- | :--- | :--- |
| `MethodArgumentNotValidException` | Datos de JSON faltantes o defectuosos. | `Solicitud invalida: [primer error detectado]` (Con objeto `errors`) | 400 |
| `BusinessRuleException` | Se pasa una fecha con formato o valor en el pasado. | `La fecha debe ser futura` | 422 |
| `BusinessRuleException` | Se pasa una hora con otro formato que no sea el estándar. | `La hora debe tener formato HH:mm:ss` | 422 |
| `BusinessRuleException` | El día exacto (ej: Sábado) no es trabajado por el médico correspondiente. | `El medico no atiende en la fecha seleccionada` | 422 |
| `BusinessRuleException` | La cita inicia antes o termina después del turno. | `La hora esta fuera de la franja de atencion del medico` | 422 |
| `BusinessRuleException` | Minutos que no cuadrarn exactos con las rejillas del doctor (ej: agenda de 20min y pide las 08:15). | `La hora debe respetar el intervalo configurado del medico ({X} minutos)` | 422 |
| `BusinessRuleException` | Ese hueco en la agenda que acaba de validarse está pisado con una cita confirmada actual. | `Horario no disponible` / `El horario seleccionado ya esta ocupado o fuera de la franja de atencion` | 422 |
| `BusinessRuleException` | Regla RF3: Trata de agendar cuando ya apartó anticipadamente 3 citas. | `Límite de 3 citas alcanzado` | 422 |

### Validaciones de Urgencia (Cita Prioritaria)
| Tipo de Excepción | Escenario Clínico | Mensaje Mapeado (Legible) | Code |
| :--- | :--- | :--- | :--- |
| `BusinessRuleException` | Se intentó colgar una prioridad atrás de una cita que no está en base de datos en ese bloque horario. | `No existe una cita de referencia en la hora indicada` | 422 |
| `BusinessRuleException` | La referencia de origen usada como ancla ya es de tipo prioritaria. | `La cita de referencia ya es prioritaria` | 422 |
| `BusinessRuleException` | La lógica detecta que colapsarían las consultas si se empuja la agenda en este hueco. | `La agenda actual no permite insertar sobrecupo despues de la cita seleccionada` | 422 |
| `BusinessRuleException` | Matemáticamente (sumando y restando espacios) ya no caben la duración de prioridad mínima (Ej: 5min). | `No hay flexibilidad suficiente para crear una cita prioritaria de 5 minutos` / `No hay espacio inmediato para insertar la cita prioritaria` | 422 |
| `BusinessRuleException` | Dos actores de caja trataron de agendar la prioridad en el mismo hueco de 5 minutos. | `El horario prioritario ya se encuentra ocupado` | 422 |

---

## 2. Módulo Médicos (`MedicosFacade.java` y Endpoints de configuración)

Dedicado estrictamente al mantenimiento de los activos humanos y sus reglas de turnos.

| Tipo de Excepción | Escenario Clínico | Mensaje Mapeado (Legible) | Code |
| :--- | :--- | :--- | :--- |
| `ResourceNotFoundException` | El API externa u otro servicio trata de invocar operaciones en un ID Fantasma. | `Medico no encontrado(a) con id: {id}` | 404 |
| `MethodArgumentNotValidException` | Payload JSON malformado para un doctor. | `Solicitud invalida...` | 400 |
| `BusinessRuleException` | En configuración: Médico empieza turno después de su salida. | `La hora de fin debe ser posterior a la hora de inicio` | 422 |
| `BusinessRuleException` | En configuración: Turnos inusualmente cortos (<2 hrs) o pesados ​​(>8 hrs). | `La jornada debe estar entre 2 y 8 horas` | 422 |
| `BusinessRuleException` | En configuración: La duración estándar escogida no está soportada. | `El intervalo debe ser uno de: 5, 10, 15, 20, 30, 45, 60 minutos` | 422 |
| `BusinessRuleException` | En configuración: Administrador envía lista nula de días laborables. | `Debe configurar al menos un dia de atencion` | 422 |

---

## 3. Módulo Pacientes (`PacientesFacade.java` y `PacienteServiceImpl.java`)

Controla la identidad clínica del usuario receptor del sistema e historiales.

| Tipo de Excepción | Escenario Clínico | Mensaje Mapeado (Legible) | Code |
| :--- | :--- | :--- | :--- |
| `ResourceNotFoundException` | Se busca resumen o se asocian cosas a paciente inexistente vía `pacienteId`. | `Paciente no encontrado(a) con id: {id}` | 404 |
| `ResourceNotFoundException` | Se busca perfil de paciente desde su `usuarioId` en Auth pero no existe el nexo. | `Paciente no encontrado(a) con id: {usuarioId}` | 404 |
| `RuntimeException` | Alternativa legacy detectada, falla general de fetch de paciente base. *(Candidata a optimizarse a `ResourceNotFoundException`)*. | `Paciente no encontrado` | 500 (o 404 si se cambia) |
| `MethodArgumentNotValidException` | Datos insuficientes como falta de cédula o edad disparada desde JSON. | `Solicitud invalida...` | 400 |

---

## 4. Módulo Seguridad y Autenticación (`AuthServiceImpl.java`)

Validación y expedición de pases JWT inter-sistemas.

| Tipo de Excepción | Escenario Clínico | Mensaje Mapeado (Legible) | Code |
| :--- | :--- | :--- | :--- |
| `BadCredentialsException` | La dupla username/password en el endpoint `/login` es incorrecta o falsa. | `Credenciales inválidas` | 401 |
| `UsernameNotFoundException` | En capa Security Spring: El token indica un username que fue purgado o no existe. | `Usuario no encontrado: {username}` | 404 / 401 |
| `BusinessRuleException` | El usuario trata de loguearse pero la clínica lo bloqueó transitoriamente en DB. | `Usuario inactivo` | 422 |
| `BusinessRuleException` | Se intenta registrar administrador o usuario con string repetido. | `El username ya está en uso` | 422 |
| `BusinessRuleException` | Intentan registrar como paciente u otra cosa con una cédula que tiene otro usuario. | `El documento ya está registrado` | 422 |

