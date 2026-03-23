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

### `POST /auth/login`

- Estado: Implementado
- Auth requerida: No
- Query params: No aplica
- Path params: No aplica
- Body:

```json
{
  "username": "maria.gonzalez",
  "password": "Password123"
}
```

- Response `200`:

```json
{
  "token": "eyJ...",
  "userId": 42,
  "username": "maria.gonzalez",
  "rol": "AGENDADOR",
  "expiresIn": 86400
}
```

### `POST /auth/register/paciente`

- Estado: Implementado
- Auth requerida: No
- Query params: No aplica
- Path params: No aplica
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

- Response `201`:

```json
{
  "token": "eyJ...",
  "userId": 100,
  "username": "paciente.demo",
  "rol": "PACIENTE",
  "expiresIn": 86400
}
```

### `POST /auth/register/admin`

- Estado: Implementado
- Auth requerida: No (estado actual)
- Query params: No aplica
- Path params: No aplica
- Body:

```json
{
  "username": "admin.demo",
  "password": "Password123"
}
```

- Response `201`:

```json
{
  "token": "eyJ...",
  "userId": 1,
  "username": "admin.demo",
  "rol": "ADMIN",
  "expiresIn": 86400
}
```

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

- Response `201`:

```json
{
  "token": "eyJ...",
  "userId": 200,
  "username": "medico.demo",
  "rol": "MEDICO",
  "expiresIn": 86400
}
```

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

- Estado: Definido (servicio pendiente)
- Auth requerida: Si
- Rol requerido: `PACIENTE`
- Query params: No aplica
- Path params: No aplica
- Body:

```json
{
  "medicoId": 1,
  "fecha": "2026-03-20",
  "hora": "09:00:00",
  "observaciones": "Control"
}
```

- Response esperada al completar implementacion (`201`):

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

## Nota de mantenimiento

- Si se agrega o cambia un endpoint, actualizar primero el catalogo central en `README.md`.
- Despues actualizar este documento con detalle de `roles + parametros + body + response`.
