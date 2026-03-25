# Manejo de Errores para Frontend (Angular)

## Checklist de este documento
- [x] Definir el contrato estandar de error que retorna el backend.
- [x] Explicar como debe reaccionar la UI por tipo de status HTTP.
- [x] Mapear errores por modulo con endpoint, status, mensaje exacto y recomendacion UI.
- [x] Documentar la logica recomendada para un interceptor HTTP global.
- [x] Listar validaciones que el frontend debe resolver localmente antes de llamar al backend.

## 1) Introduccion

Este documento es la guia de referencia para el equipo frontend Angular sobre el manejo de errores del backend Piedrazul.

Su objetivo es que cualquier desarrollador frontend pueda:
- interpretar correctamente las respuestas de error del backend,
- mostrar mensajes consistentes al usuario final,
- decidir cuando usar error inline, toast o redireccion,
- y evitar llamadas innecesarias validando datos en el formulario antes de enviar.

Fuentes usadas para esta guia:
- `docs/04-api/03-manejo-excepciones.md` (mensajes exactos, status y contexto funcional).
- `docs/04-api/01-endpoints-implementados.md` (contratos por endpoint y flujo).

---

## 2) Formato estandar de respuesta de error

El backend usa un formato unificado de error.

### 2.1 Estructura general (401, 404, 422, 500)

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Mensaje legible del error",
  "timestamp": "2026-03-25T12:43:21.0415695"
}
```

### 2.2 Estructura para validacion de campos (400)

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Solicitud invalida: El celular debe tener 10 dígitos",
  "timestamp": "2026-03-25T12:43:21.0415695",
  "errors": {
    "celular": "El celular debe tener 10 dígitos"
  }
}
```

### 2.3 Significado de campos

- `status`: codigo HTTP numerico.
- `error`: descripcion corta estandar HTTP (`Bad Request`, `Unauthorized`, `Not Found`, `Unprocessable Entity`, `Internal Server Error`).
- `message`: mensaje legible y util para UI (ya viene redactado para usuario final).
- `timestamp`: fecha/hora del error en backend, util para trazabilidad.
- `errors` (solo 400): mapa `campo -> mensaje` para pintar errores inline en formularios.

### 2.4 Ejemplos por status

#### 400 - Bad Request (validaciones)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Solicitud invalida: La hora es obligatoria",
  "timestamp": "2026-03-25T12:43:21.0415695",
  "errors": {
    "hora": "La hora es obligatoria",
    "fecha": "La fecha es obligatoria"
  }
}
```

#### 401 - Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciales inválidas",
  "timestamp": "2026-03-25T12:43:21.0415695"
}
```

#### 404 - Not Found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Medico no encontrado(a) con id: 99",
  "timestamp": "2026-03-25T12:43:21.0415695"
}
```

#### 422 - Unprocessable Entity
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "El medico no esta activo",
  "timestamp": "2026-03-25T12:43:21.0415695"
}
```

#### 500 - Internal Server Error
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error interno del servidor",
  "timestamp": "2026-03-25T12:43:21.0415695"
}
```

---

## 3) Estrategia de presentacion por tipo de error

| HTTP Status | Accion recomendada en frontend |
| --- | --- |
| `400` | Mostrar errores inline por campo usando `errors`. Si una key coincide con un control del formulario, pintar mensaje debajo del input. Usar `message` como fallback general. |
| `401` | Limpiar sesion y redirigir a login. Si el error viene de `/auth/login`, mostrar `Credenciales inválidas`. Si viene de otro endpoint, tratarlo como sesion expirada/no autorizada. |
| `404` | Mostrar toast o mensaje en pantalla con `message`. No redirigir automaticamente, salvo que la vista dependa del recurso inexistente. |
| `422` | Mostrar toast o mensaje contextual cerca de la accion. El `message` ya es legible y debe mostrarse tal cual. |
| `500` | Mostrar mensaje generico: `Ocurrió un error inesperado, intenta de nuevo`. No mostrar detalles tecnicos. |

Notas UI:
- Prioridad para formularios: primero `errors[campo]`, luego `message` general.
- No hardcodear textos alternos si el backend ya envia mensaje exacto util.
- Mantener consistencia de estilo visual (toast para reglas de negocio; inline para validacion).

---

## 4) Referencia de errores por modulo

## 4.1 Modulo Agenda

| Endpoint relacionado | HTTP Status | Mensaje exacto (`message`) | Cuando ocurre | Sugerencia de presentacion UI |
| --- | --- | --- | --- | --- |
| `GET /api/v1/citas/agenda` `GET /api/v1/citas/agenda-dinamica` `GET /api/v1/citas/disponibilidad/primera` `POST /api/v1/citas/manual` `POST /api/v1/citas/prioridad` `POST /api/v1/citas/autonomo` | `404` | `Medico no encontrado(a) con id: {id}` | Se consulta/agenda con `medicoId` inexistente. | Toast con mensaje backend; mantener al usuario en la pantalla para corregir medico. |
| Mismos endpoints de agenda/citas | `422` | `El medico no esta activo` | Medico existe pero no esta activo. | Mensaje contextual en selector de medico + toast. |
| Mismos endpoints de agenda/citas | `422` | `El medico no tiene configuracion horaria activa` | Medico sin configuracion de jornada activa. | Toast y deshabilitar accion de agendar para ese medico. |
| `GET /api/v1/citas/disponibilidad/primera` | `422` | `No hay horarios disponibles para el medico en la ventana de busqueda` | No hay slots para ese medico en la ventana de busqueda. | Mostrar estado vacio en panel de disponibilidad. |
| `GET /api/v1/citas/disponibilidad/primera/global` | `422` | `No hay medicos activos para agendar` | No existen medicos activos en sistema. | Mostrar pantalla vacia con CTA para administracion. |
| `GET /api/v1/citas/disponibilidad/primera/global` | `422` | `No hay horarios disponibles en la ventana de busqueda` | Hay medicos activos pero sin disponibilidad en ventana. | Estado vacio con recomendacion de cambiar fecha/filtro. |
| `POST /api/v1/citas/manual` | `400` | `Solicitud invalida: {primer error}` | Faltan campos o formato invalido en body. | Error inline por campo desde `errors`. |
| `POST /api/v1/citas/manual` `POST /api/v1/citas/autonomo` | `422` | `La fecha debe ser futura` | Fecha de cita no permitida por regla vigente. | Mostrar junto a selector de fecha + toast corto. |
| `POST /api/v1/citas/manual` `POST /api/v1/citas/prioridad` | `422` | `La hora debe tener formato HH:mm:ss` | Hora invalida para parseo backend. | Normalizar control de hora y mostrar error inline. |
| `POST /api/v1/citas/manual` | `422` | `El medico no atiende en la fecha seleccionada` | Dia elegido no esta en `diasAtencion`. | Mensaje bajo selector de fecha y sugerir dia valido. |
| `POST /api/v1/citas/manual` | `422` | `La hora esta fuera de la franja de atencion del medico` | Hora fuera de `horaInicio`/`horaFin`. | Marcar campo hora y recargar opciones de slots validos. |
| `POST /api/v1/citas/manual` | `422` | `La hora debe respetar el intervalo configurado del medico ({X} minutos)` | Hora no alineada al intervalo (ej. 08:15 con intervalo de 20). | Mostrar error inline y autocompletar horas disponibles. |
| `POST /api/v1/citas/autonomo` | `422` | `Horario no disponible` | Slot ocupado o invalido en flujo autonomo. | Toast contextual junto al horario elegido. |
| `POST /api/v1/citas/manual` | `422` | `El horario seleccionado ya esta ocupado o fuera de la franja de atencion` | Slot ocupado o fuera de franja en flujo manual. | Toast y refrescar disponibilidad del medico. |
| `POST /api/v1/citas/autonomo` | `422` | `Límite de 3 citas alcanzado` | Paciente ya tiene 3 citas futuras activas. | Modal/toast informativo con opcion de ver citas actuales. |
| `POST /api/v1/citas/prioridad` | `422` | `No existe una cita de referencia en la hora indicada` | No existe cita base para insertar prioridad. | Toast y forzar recarga del panel dinamico. |
| `POST /api/v1/citas/prioridad` | `422` | `La cita de referencia ya es prioritaria` | Se intenta abrir prioridad sobre otra prioridad. | Mensaje contextual en tarjeta de cita. |
| `POST /api/v1/citas/prioridad` | `422` | `La agenda actual no permite insertar sobrecupo despues de la cita seleccionada` | Regla de sobrecupo bloquea insercion por colision de agenda. | Toast explicativo; sugerir otra cita base. |
| `POST /api/v1/citas/prioridad` | `422` | `No hay flexibilidad suficiente para crear una cita prioritaria de 5 minutos` | Calculo de hueco + flexibilidad insuficiente. | Mensaje contextual en agenda dinamica. |
| `POST /api/v1/citas/prioridad` | `422` | `No hay espacio inmediato para insertar la cita prioritaria` | No existe hueco inmediato posterior para ubicar prioridad. | Toast y mantener foco en bloque horario. |
| `POST /api/v1/citas/prioridad` | `422` | `El horario prioritario ya se encuentra ocupado` | Colision por concurrencia al crear prioridad. | Toast de conflicto y recarga automatica de la agenda. |

## 4.2 Modulo Medicos

| Endpoint relacionado | HTTP Status | Mensaje exacto (`message`) | Cuando ocurre | Sugerencia de presentacion UI |
| --- | --- | --- | --- | --- |
| `GET /api/v1/medicos/{medicoId}/configuracion` `PUT /api/v1/medicos/{medicoId}/configuracion` | `404` | `Medico no encontrado(a) con id: {id}` | `medicoId` inexistente. | Toast y volver al listado de medicos. |
| `PUT /api/v1/medicos/{medicoId}/configuracion` | `400` | `Solicitud invalida: {primer error}` | Body invalido (campos obligatorios/formato). | Error inline en formulario de configuracion. |
| `PUT /api/v1/medicos/{medicoId}/configuracion` | `422` | `La hora de fin debe ser posterior a la hora de inicio` | `horaFin <= horaInicio`. | Error inline entre campos de horario. |
| `PUT /api/v1/medicos/{medicoId}/configuracion` | `422` | `La jornada debe estar entre 2 y 8 horas` | Duracion fuera de rango permitido. | Mensaje bajo bloque de jornada. |
| `PUT /api/v1/medicos/{medicoId}/configuracion` | `422` | `El intervalo debe ser uno de: 5, 10, 15, 20, 30, 45, 60 minutos` | Intervalo no soportado por negocio. | Restringir selector a valores permitidos + toast si llega backend. |
| `PUT /api/v1/medicos/{medicoId}/configuracion` | `422` | `Debe configurar al menos un dia de atencion` | Lista de dias vacia o nula. | Marcar grupo de dias y exigir al menos uno. |

## 4.3 Modulo Pacientes

| Endpoint relacionado | HTTP Status | Mensaje exacto (`message`) | Cuando ocurre | Sugerencia de presentacion UI |
| --- | --- | --- | --- | --- |
| `GET /api/v1/pacientes/{id}` | `404` | `Paciente no encontrado(a) con id: {id}` | Se consulta detalle de paciente inexistente. | Toast + volver a listado/buscador. |
| Flujos internos de agenda/autenticacion que buscan paciente por usuario | `404` | `Paciente no encontrado(a) con id: {usuarioId}` | Usuario autenticado sin perfil paciente asociado. | Redirigir a soporte/perfil incompleto con mensaje claro. |
| Endpoints de pacientes con `@Valid` (si aplica DTO validado) | `400` | `Solicitud invalida: {primer error}` | Payload invalido por formato o faltantes. | Error inline en formulario correspondiente. |
| (Legacy, si reaparece en algun flujo antiguo) | `500` | `Error interno del servidor` | Error runtime no mapeado de forma especifica. | Mensaje generico al usuario; log solo en dev. |

## 4.4 Modulo Seguridad y Autenticacion

| Endpoint relacionado | HTTP Status | Mensaje exacto (`message`) | Cuando ocurre | Sugerencia de presentacion UI |
| --- | --- | --- | --- | --- |
| `POST /api/v1/auth/login` | `401` | `Credenciales inválidas` | Username/password incorrectos. | Mostrar mensaje en formulario de login sin redireccion. |
| Endpoints autenticados (cualquiera fuera de login) | `401` | `No autorizado` o mensaje de autorizacion del backend | Token ausente, invalido o expirado. | Limpiar sesion y redirigir a login. |
| Flujos de seguridad (UserDetailsService) | `404` | `Usuario no encontrado: {username}` | Username del contexto/token no existe en BD. | Forzar logout y redirigir a login. |
| `POST /api/v1/auth/login` | `422` | `Usuario inactivo` | Usuario existe pero no puede iniciar sesion por estado. | Mensaje en login con recomendacion de contacto soporte. |
| `POST /api/v1/auth/register/paciente` `POST /api/v1/auth/register/medico` `POST /api/v1/auth/register/admin` | `422` | `El username ya está en uso` | Registro con username repetido. | Error inline en campo username. |
| `POST /api/v1/auth/register/paciente` | `422` | `El documento ya está registrado` | Documento ya existe en sistema. | Error inline en documento + sugerir recuperar cuenta. |

## 4.5 Errores transversales

| Endpoint relacionado | HTTP Status | Mensaje exacto (`message`) | Cuando ocurre | Sugerencia de presentacion UI |
| --- | --- | --- | --- | --- |
| Cualquier endpoint | `500` | `Error interno del servidor` | Excepcion no controlada en backend. | Mostrar mensaje generico: `Ocurrió un error inesperado, intenta de nuevo`. |

---

## 5) Recomendacion de interceptor HTTP global (Angular)

Se recomienda implementar un interceptor HTTP global con esta logica:

1. Capturar toda respuesta de error con `status >= 400`.
2. Parsear el objeto estandar de backend (`status`, `error`, `message`, `timestamp`, `errors?`).
3. Si `status === 400`:
   - leer `errors` (si existe),
   - iterar keys,
   - inyectar mensajes en los controles del formulario activo que coincidan por nombre,
   - usar `message` como fallback general.
4. Si `status === 401`:
   - si endpoint es distinto de `/auth/login`, limpiar sesion y redirigir a login,
   - si endpoint es `/auth/login`, mostrar `Credenciales inválidas` en la vista de login.
5. Si `status === 422` o `status === 404`:
   - mostrar `message` en toast/notificacion.
6. Si `status === 500`:
   - mostrar mensaje generico al usuario,
   - no exponer detalle tecnico.
7. Registrar el error completo en consola solo en entorno de desarrollo.

Recomendacion adicional:
- centralizar el mapping de mensajes en un servicio `ErrorPresenterService` para evitar duplicidad entre componentes.

---

## 6) Errores que el frontend debe manejar localmente

Estos casos deben validarse antes de invocar backend para evitar roundtrips innecesarios:

- Campos requeridos vacios.
- Formato de correo invalido.
- Formato de fecha invalido.
- Celular con menos o mas de 10 digitos.
- Hora con formato invalido (si el control permite ingreso manual).
- Seleccion de fecha/hora vacia en formularios de agendamiento.
- Limites basicos de longitud (usuario, nombres, observaciones), segun reglas del formulario.

Checklist recomendado por formulario:
- Login: username y password requeridos.
- Registro paciente: documento, nombres, apellidos, celular, genero, fecha, correo.
- Cita manual/autonoma: medico, fecha, hora y datos paciente (si aplica).
- Configuracion de agenda medico: dias, horaInicio, horaFin, intervalo.

Con esto, el backend queda para validar reglas de negocio y el frontend para validar forma y experiencia de usuario.

