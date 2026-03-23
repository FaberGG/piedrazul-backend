# Modelo de Datos y Diccionario (Resumen)

## Objetivo

Documentar entidades principales del backend y su responsabilidad de negocio, sin reemplazar una especificacion de base de datos detallada.

## Entidades clave

## `Usuario` (modulo `auth`)

Representa la identidad autenticable del sistema.

Campos relevantes:

- `id`
- `username`
- `password` (hash BCrypt)
- `rol`
- `estado`

## `Paciente` (modulo `pacientes`)

Representa informacion administrativa basica del paciente.

Campos relevantes:

- `id`
- `documento`
- `nombres`
- `apellidos`
- `celular`
- `correo`
- `fechaNacimiento`
- `genero`

## `Medico` (modulo `medicos`)

Representa profesional de salud y su configuracion de atencion.

Campos relevantes:

- `id`
- `usuario`
- `nombres`, `apellidos`
- `especialidad`
- `tipo`
- `estado`
- `horaInicioAtencion`
- `horaFinAtencion`
- `intervaloMinutos`
- `diasAtencion`

## `Cita` (modulo `agenda`)

Representa una reserva de atencion entre paciente y medico.

Campos relevantes:

- `id`
- `pacienteId`
- `medicoId`
- `fecha`
- `hora`
- `duracionMinutos`
- `tipoCita` (`ESTANDAR`, `PRIORIDAD`)
- `estado` (`PROGRAMADA`, `CONFIRMADA`, `ATENDIDA`, `CANCELADA`)
- `observaciones`
- `creadoPor`
- `createdAt`

## `HistorialCambiosCita` (modulo `agenda`)

Entidad orientada a trazabilidad de cambios de agenda.

## Relaciones funcionales relevantes

- Un `Usuario` puede estar asociado a perfil de `Paciente` o `Medico`.
- Una `Cita` referencia por ID a `Paciente` y `Medico` desde el modulo `agenda`.
- `agenda` no depende de entidades JPA de otros modulos; consume APIs publicas.

## Reglas de integridad relevantes

- Evitar doble cita en el mismo medico/fecha/hora.
- Excluir citas `CANCELADA` para calculo de ocupacion y disponibilidad.
- Respetar intervalo y franja de atencion configurada por medico.

## Indices y optimizacion (recomendado)

- `citas(medico_id, fecha)` para consulta de agenda diaria.
- `pacientes(documento)` para busqueda por prefijo/autocompletado.
- `citas(paciente_id, fecha)` para consultas historicas por paciente.

## Nota

Este documento es un diccionario funcional compacto. Para evolucion de esquema (DDL, migraciones, constraints avanzadas), se recomienda mantener una especificacion tecnica adicional (Flyway/Liquibase cuando aplique).

