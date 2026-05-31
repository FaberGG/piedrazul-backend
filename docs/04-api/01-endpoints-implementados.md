# Endpoints Implementados (Detalle de Contratos)

Este documento describe contratos HTTP con formato operativo (parametros, body y response JSON) similar al `README.md` raiz.

El catalogo central de endpoints y su estado se mantiene en:

- [`../README.md`](../../README.md) -> seccion `Endpoints implementados (catalogo central)`

Base URL: `http://localhost:8080/api/v1`

## Convenciones

- Excepto endpoints publicos, se requiere header:

```http
Authorization: Bearer <jwt>
Content-Type: application/json
```

- Si el token falta o es invalido, la API responde:

```json
{
  "error": "No autorizado"
}
```

- Nota de roles: existe coexistencia documental/codigo entre `ADMIN` y `ADMINISTRADOR` en algunos flujos historicos.

## Auth

> Nota: el login no lo expone Spring. El token se obtiene en Keycloak. Ver guia en [`05-keycloak-autenticacion.md`](05-keycloak-autenticacion.md).

### `POST /auth/register/paciente`

- Estado: Implementado
- Auth requerida: No
- Query params: No aplica
- Path params: No aplica
- Body:

```json
{
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

- Response `201`: sin cuerpo (`ResponseEntity<Void>`).

### `POST /auth/register/admin`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `ADMIN`
- Query params: No aplica
- Path params: No aplica
- Body:

```json
{
  "username": "admin.demo",
  "password": "Password123"
}
```

- Response `201`: sin cuerpo (`ResponseEntity<Void>`).

### `POST /auth/register/medico`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `ADMIN`
- Query params: No aplica
- Path params: No aplica
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

- Response `201`: sin cuerpo (`ResponseEntity<Void>`).

## Agenda

### `POST /citas/manual` (RF2)

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`
- Query params: No aplica
- Path params: No aplica
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

- Response `201`:

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

- Validaciones clave:
  - fecha futura
  - medico existente y activo
  - horario disponible segun agenda
  - parseo de hora `HH:mm:ss`
  - paciente por documento: reutiliza o crea

### `GET /citas/agenda` (RF1)

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `ADMIN`
- Path params: No aplica
- Query params esperados:

```json
{
  "medicoId": 1,
  "fecha": "2026-03-20"
}
```

- Response `200`:

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

### `POST /citas/autonomo` (RF3)

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `PACIENTE`
- Query params: No aplica
- Path params: No aplica
- Validaciones: bloquea si el paciente ya tiene una cita PROGRAMADA futura; límite de 3 citas futuras activas
- Body:

```json
{
  "medicoId": 1,
  "fecha": "2026-03-20",
  "hora": "09:00:00",
  "observaciones": "Control"
}
```

- Response `201`:

```json
{
  "id": 202,
  "pacienteNombre": "Paciente Demo",
  "pacienteDocumento": "1234567890",
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-03-20",
  "hora": "09:00:00",
  "estado": "PROGRAMADA",
  "observaciones": "Control"
}
```

### `GET /citas/agenda-dinamica`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `ADMIN`
- Path params: No aplica
- Query params esperados:

```json
{
  "medicoId": 1,
  "fecha": "2026-06-10"
}
```

- Response `200` (estructura orientada a UI):

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

### `GET /citas/agenda-dinamica/stream`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `ADMIN`
- Tipo de respuesta: `text/event-stream`
- Query params esperados:

```json
{
  "medicoId": 1,
  "fecha": "2026-06-10"
}
```

- Eventos SSE emitidos:
  - `connected`: confirma suscripcion.
  - `agenda-snapshot`: snapshot inicial con la misma estructura de `GET /citas/agenda-dinamica`.
  - `agenda-updated`: evento de invalidacion cuando cambia agenda del medico/fecha.

- Ejemplo de evento `agenda-updated`:

```json
{
  "medicoId": 1,
  "fecha": "2026-06-10",
  "citaId": 333,
  "accion": "CITA_MANUAL_CREADA",
  "changedAt": "2026-06-10T14:30:00Z"
}
```

### `POST /citas/prioridad`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `ADMIN`
- Query params: No aplica
- Path params: No aplica
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

- Response `201` (ejemplo):

```json
{
  "id": 333,
  "pacienteNombre": "Paciente Prioritario",
  "pacienteDocumento": "1234567890",
  "medicoNombre": "Dra. Maria Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-06-10",
  "hora": "09:05:00",
  "estado": "PROGRAMADA",
  "observaciones": "Sobrecupo autorizado"
}
```

### `PATCH /citas/{id}/reagendar` (RF8)

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `TERAPISTA`, `MEDICO`, `ADMIN`
- Path params: `id` — ID de la cita a reagendar
- Query params: No aplica
- Validaciones: solo citas en estado `ATENDIDA`; nuevo slot debe estar disponible
- Body:

```json
{
  "nuevaFecha": "2026-07-15",
  "nuevaHora": "09:00:00",
  "motivo": "Seguimiento post-consulta",
  "medicoNuevoId": null
}
```

- Response `200` (cita con estado PROGRAMADA en nueva fecha):

```json
{
  "id": 101,
  "pacienteNombre": "Juan Carlos Perez",
  "pacienteDocumento": "1234567890",
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-07-15",
  "hora": "09:00:00",
  "estado": "PROGRAMADA",
  "observaciones": "Dolor lumbar cronico"
}
```

### `GET /citas/{id}/historial` (RF8)

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `TERAPISTA`, `MEDICO`, `ADMIN`
- Path params: `id` — ID de la cita
- Response `200` (lista ordenada del más reciente al más antiguo):

```json
[
  {
    "id": 1,
    "fechaAnterior": "2026-06-10",
    "horaAnterior": "08:00:00",
    "medicoAnteriorId": 1,
    "fechaNueva": "2026-07-15",
    "horaNueva": "09:00:00",
    "medicoNuevoId": 1,
    "motivo": "Seguimiento post-consulta",
    "modificadoPor": "uuid-del-usuario",
    "creadoEn": "2026-06-10T14:35:00"
  }
]
```

### `GET /citas/{id}`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `TERAPISTA`, `MEDICO`, `ADMIN`
- Path params: `id` — ID de la cita
- Response `200`:

```json
{
  "id": 101,
  "pacienteNombre": "Juan Carlos Perez",
  "pacienteDocumento": "1234567890",
  "pacienteCelular": "3001234567",
  "pacienteCorreo": "juan@email.com",
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-06-10",
  "hora": "08:00:00",
  "estado": "PROGRAMADA",
  "observaciones": "Control",
  "esPrimeraCita": true
}
```

- Nota: `esPrimeraCita` es `true` si no existe ninguna cita con ID menor para el mismo paciente. Usado en frontend para controlar permisos de edición del MEDICO.

### `PATCH /citas/{id}`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `MEDICO` (solo primera cita del paciente), `ADMIN` (cualquier cita)
- Path params: `id` — ID de la cita
- Todos los campos son opcionales; al menos uno debe estar presente
- Transiciones de estado permitidas: `PROGRAMADA → ATENDIDA`, `PROGRAMADA → CANCELADA`
- Body:

```json
{
  "nuevoEstado": "ATENDIDA",
  "nuevasObservaciones": "Paciente con mejoría notable",
  "pacienteNombres": "Juan Carlos",
  "pacienteApellidos": "Perez Gomez",
  "pacienteDocumento": "1234567890",
  "pacienteCelular": "3001234567",
  "pacienteCorreo": "juan@email.com"
}
```

- Response `200`:

```json
{
  "id": 101,
  "pacienteNombre": "Juan Carlos Perez Gomez",
  "pacienteDocumento": "1234567890",
  "medicoNombre": "Clara Ines Cordoba",
  "especialidad": "TERAPIA_NEURAL",
  "fecha": "2026-06-10",
  "hora": "08:00:00",
  "estado": "ATENDIDA",
  "observaciones": "Paciente con mejoría notable"
}
```

- Errores:
  - `422` — transición no permitida
  - `422` — MEDICO intenta modificar cita que no es la primera del paciente
  - `403` — AGENDADOR sin acceso

### `GET /citas/disponibilidad/primera`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `PACIENTE`, `ADMIN`
- Query params: varian segun criterio de busqueda de disponibilidad por medico/fecha
- Response `200` (ejemplo):

```json
{
  "medicoId": 1,
  "fecha": "2026-06-10",
  "hora": "07:15:00",
  "disponible": true
}
```

### `GET /citas/disponibilidad/primera/global`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `PACIENTE`, `ADMIN`
- Query params: varian segun criterio global de disponibilidad
- Response `200` (ejemplo):

```json
{
  "medicoId": 3,
  "medicoNombre": "Dr. Pablo Noguera",
  "especialidad": "FISIOTERAPIA",
  "fecha": "2026-06-10",
  "hora": "08:00:00",
  "disponible": true
}
```

## Pacientes

### `GET /pacientes`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `ADMIN`, `MEDICO`
- Query params: No aplica
- Path params: No aplica
- Response `200`:

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

### `GET /pacientes/{id}`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `ADMIN`, `MEDICO`, `PACIENTE`
- Path params:

```json
{
  "id": 1
}
```

- Query params: No aplica
- Response `200`:

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

### `GET /pacientes/buscar`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `ADMIN`
- Path params: No aplica
- Query params:

```json
{
  "documento": "123",
  "limit": 5
}
```

- Response `200`:

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

## Medicos

### `GET /medicos`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `PACIENTE`, `ADMIN`
- Path params: No aplica
- Query params opcionales:

```json
{
  "especialidad": "TERAPIA_NEURAL"
}
```

- Response `200`:

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

### `GET /medicos/{medicoId}/configuracion`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `MEDICO_TERAPISTA`, `MEDICO`, `PACIENTE`, `ADMIN`
- Path params:

```json
{
  "medicoId": 1
}
```

- Query params: No aplica
- Response `200`:

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

### `PUT /medicos/{medicoId}/configuracion`

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `ADMIN`
- Path params:

```json
{
  "medicoId": 1
}
```

- Query params: No aplica
- Body:

```json
{
  "diasAtencion": ["MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"],
  "horaInicio": "07:00:00",
  "horaFin": "12:00:00",
  "intervaloMinutos": 15
}
```

- Response `200`:

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

## Reportes

### `GET /reportes/citas`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `ADMIN` (validar estandarizacion con `ADMINISTRADOR` en legado)
- Path params: No aplica
- Query params:

```json
{
  "desde": "2026-03-01",
  "hasta": "2026-03-31"
}
```

- Response `200`:

---

```json
{
  "desde": "2026-03-01",
  "hasta": "2026-03-31",
  "totalCitas": 25,
  "citasProgramadas": 12,
  "citasAtendidas": 10,
  "citasCanceladas": 3,
  "porcentajeOcupacion": 0.0
}
```
### `GET /reportes/citas/reporteDiario`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `ADMIN`, `MEDICO`, `TERAPISTA`
- Path params: No aplica
- Query params:

```json
{
  "dia": "2026-05-10",
  "medicoId": 1,
  "exportFormat": "CSV"
}
```

- Response `200`: Archivo `.csv` descargable

---

### `GET /reportes/formatos`

- Estado: Implementado
- Auth requerida: Si
- Roles requeridos: `AGENDADOR`, `ADMIN`, `MEDICO`, `TERAPISTA`
- Path params: No aplica
- Query params: No aplica
- Response `200`:

```json
["CSV", "PDF"]
```


## Configuracion de Agenda

### `GET /configuracion/agenda`

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `ADMIN`
- Response `200` (ejemplo):

```json
{
  "ventanaAgendamientoSemanas": 4
}
```

### `PUT /configuracion/agenda/ventana`

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `ADMIN`
- Body:

```json
{
  "ventanaAgendamientoSemanas": 4
}
```

- Validaciones clave: minimo 1 semana, maximo 12 semanas.
- Response `200` (ejemplo):

```json
{
  "ventanaAgendamientoSemanas": 4
}
```

### `GET /configuracion/agenda/dias-no-laborales`

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `ADMIN`
- Response `200` (ejemplo):

```json
[
  {
    "id": 1,
    "fecha": "2026-01-01",
    "descripcion": "Anio Nuevo"
  }
]
```

### `POST /configuracion/agenda/dias-no-laborales`

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `ADMIN`
- Body:

```json
{
  "fecha": "2026-01-01",
  "descripcion": "Anio Nuevo"
}
```

- Response `201` (ejemplo):

```json
{
  "id": 1,
  "fecha": "2026-01-01",
  "descripcion": "Anio Nuevo"
}
```

### `DELETE /configuracion/agenda/dias-no-laborales/{id}`

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `ADMIN`
- Response `204`: sin cuerpo.

### `POST /configuracion/agenda/dias-no-laborales/importar-festivos?anio=YYYY`

- Estado: Implementado
- Auth requerida: Si
- Rol requerido: `ADMIN`
- Response `200` (ejemplo):

```json
[
  {
    "id": 1,
    "fecha": "2026-01-01",
    "descripcion": "Anio Nuevo"
  }
]
```

## Nota de mantenimiento

- Si se agrega o cambia un endpoint, actualizar primero el catalogo central en `README.md`.
- Despues actualizar este documento con detalle de `roles + parametros + body + response`.
