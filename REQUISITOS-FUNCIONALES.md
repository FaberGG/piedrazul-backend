# Especificación de Requisitos Funcionales
## Sistema de Agendamiento de Citas Médicas — Red de Servicios Médicos de Piedrazul

**Universidad del Cauca — Facultad de Ingeniería Electrónica y Telecomunicaciones**  
**Programa de Ingeniería de Sistemas — Ingeniería de Software III**  
**Semestre 2026.1**

---

## Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Roles del Sistema](#2-roles-del-sistema)
3. [Requisitos del Sprint 1 (Alta Prioridad)](#3-requisitos-del-sprint-1-alta-prioridad)
    - [RF-01 Listar citas por médico y fecha](#rf-01-listar-citas-por-médico-y-fecha)
    - [RF-02 Crear cita manualmente (agendador)](#rf-02-crear-cita-manualmente-agendador)
    - [RF-03 Agendar cita autónoma (paciente)](#rf-03-agendar-cita-autónoma-paciente)
    - [RF-04 Configurar parámetros del sistema (administrador)](#rf-04-configurar-parámetros-del-sistema-administrador)
4. [Requisitos Funcionales Completos](#4-requisitos-funcionales-completos)
    - [RF-05 Gestión de usuarios del sistema](#rf-05-gestión-de-usuarios-del-sistema)
    - [RF-06 Autenticación y control de acceso](#rf-06-autenticación-y-control-de-acceso)
    - [RF-07 Gestión de médicos y terapistas](#rf-07-gestión-de-médicos-y-terapistas)
    - [RF-08 Re-agendamiento de citas](#rf-08-re-agendamiento-de-citas)
    - [RF-09 Exportación de citas](#rf-09-exportación-de-citas)
    - [RF-10 Historia clínica básica](#rf-10-historia-clínica-básica)
    - [RF-11 Auditoría del sistema](#rf-11-auditoría-del-sistema)
    - [RF-12 Reportes y estadísticas](#rf-12-reportes-y-estadísticas)
5. [Matriz de Priorización](#5-matriz-de-priorización)

---

## 1. Introducción

Este documento especifica los requisitos funcionales del sistema de agendamiento de citas médicas para la **Red de Servicios Médicos de Piedrazul**, ubicada en el kilómetro 5 vía al Huila, Popayán. El sistema busca reemplazar el actual software de escritorio con una aplicación web moderna que permita tanto el agendamiento autónomo por parte de los pacientes como el agendamiento manual por parte del personal, optimizando la gestión de citas y liberando tiempo al personal médico.

Los requisitos marcados con el indicador **🟠 Sprint 1** corresponden a las historias de usuario priorizadas para la primera iteración de desarrollo, por ser las de mayor valor para el cliente.

---

## 2. Roles del Sistema

| Rol | Descripción |
|-----|-------------|
| **Administrador** | Gestiona usuarios, médicos/terapistas y parámetros de configuración del sistema. |
| **Agendador de citas** | Crea, modifica y consulta citas médicas de forma manual en nombre de los pacientes. |
| **Médico / Terapista** | Consulta su agenda, registra historias clínicas y puede agendar citas manualmente. |
| **Paciente** | Usuario registrado que agenda sus propias citas de forma autónoma a través del portal web. |

---

## 3. Requisitos del Sprint 1 (Alta Prioridad)

> Los siguientes cuatro requisitos han sido identificados como los de mayor valor para el cliente y deben ser implementados en el primer corte del proyecto.

---

### RF-01 Listar citas por médico y fecha

> 🟠 **Sprint 1** | Actor principal: **Agendador de citas**

**Historia de usuario:**
> *"Yo como agendador de citas necesito listar las citas médicas de un determinado médico/terapista en una fecha determinada para visualizar el listado y la cantidad de citas."*

#### Descripción

El sistema debe permitir al agendador de citas consultar el listado de citas asignadas a un médico o terapista específico en una fecha determinada, facilitando la planificación y organización de la jornada laboral del centro médico.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-01.1 | El sistema presenta un formulario de búsqueda con al menos dos campos: selector de médico/terapista y selector de fecha. |
| CA-01.2 | Los resultados de la búsqueda se muestran en una tabla ordenada cronológicamente por hora de la cita. |
| CA-01.3 | La tabla de resultados muestra como mínimo: hora de la cita, número de documento del paciente, nombre completo del paciente, celular y observaciones. |
| CA-01.4 | El sistema muestra el total de citas encontradas para el médico y la fecha seleccionados. |
| CA-01.5 | Si no existen citas para los criterios de búsqueda indicados, el sistema muestra un mensaje informativo. |
| CA-01.6 | El listado solo es accesible para usuarios con rol de agendador de citas, médico/terapista o administrador. |

#### Datos de entrada

- Médico o terapista (selector de lista)
- Fecha de consulta (selector de calendario)

#### Datos de salida (columnas de la tabla)

- Hora de la cita
- Número de documento de identidad del paciente
- Nombre completo del paciente
- Número de celular
- Observaciones
- Total de citas (contador)

#### Notas de diseño

Se recomienda implementar un panel de búsqueda en la parte superior con los filtros, y los resultados debajo en forma de tabla paginada o con scroll. La consulta debe ejecutarse al presionar un botón de búsqueda o al cambiar los valores de los filtros.

---

### RF-02 Crear cita manualmente (agendador)

> 🟠 **Sprint 1** | Actor principal: **Agendador de citas**

**Historia de usuario:**
> *"Yo como agendador de citas necesito crear una nueva cita de un paciente que me ha contactado por WhatsApp para hacer efectiva esa cita."*

#### Descripción

El sistema debe permitir al agendador registrar una nueva cita médica de forma manual, capturando los datos básicos del paciente y los detalles de la cita. El agendador ingresa la información que el paciente le ha suministrado por WhatsApp u otro canal de comunicación.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-02.1 | El sistema presenta un formulario para capturar los datos del paciente y de la cita. |
| CA-02.2 | El sistema permite buscar un paciente existente por número de documento antes de crear uno nuevo, evitando duplicados. |
| CA-02.3 | Si el paciente no existe, el sistema permite registrarlo con sus datos básicos en el mismo flujo. |
| CA-02.4 | El selector de hora muestra únicamente los horarios disponibles según el médico/terapista seleccionado y el intervalo de tiempo configurado para ese profesional. |
| CA-02.5 | El sistema no permite registrar dos citas para el mismo médico en la misma fecha y hora. |
| CA-02.6 | Al guardar la cita exitosamente, el sistema muestra un mensaje de confirmación con el resumen de la cita creada. |
| CA-02.7 | Los campos marcados como obligatorios deben ser validados antes de permitir guardar. |
| CA-02.8 | Solo pueden crear citas manualmente los usuarios con rol de agendador de citas o médico/terapista. |

#### Datos del paciente a capturar

| Campo | Obligatorio |
|-------|-------------|
| Número de documento de identidad | Sí |
| Nombres y apellidos completos | Sí |
| Celular | Sí |
| Género (Hombre / Mujer / Otro) | Sí |
| Fecha de nacimiento | No |
| Correo electrónico | No |

#### Datos de la cita a capturar

| Campo | Obligatorio | Observaciones |
|-------|-------------|---------------|
| Médico / Terapista | Sí | Solo profesionales activos |
| Fecha de la cita | Sí | Solo días hábiles configurados |
| Hora de la cita | Sí | Basada en el intervalo del médico y horarios disponibles |
| Observaciones | No | Texto libre |

#### Reglas de negocio

- Solo pueden asignarse citas a médicos o terapistas con estado **activo**.
- El intervalo entre citas se determina por la configuración individual de cada médico/terapista (ver RF-04 y RF-07).
- No se pueden crear citas en fechas pasadas.
- No se pueden crear dos citas con el mismo médico en el mismo slot de tiempo.

---

### RF-03 Agendar cita autónoma (paciente)

> 🟠 **Sprint 1** | Actor principal: **Paciente**

**Historia de usuario:**
> *"Yo como paciente necesito agendar una cita mediante la web para tener una cita de manera sencilla y rápida sin tener que usar WhatsApp."*

#### Descripción

El sistema debe ofrecer un portal de autogestión para que los pacientes registrados puedan agendar sus propias citas médicas sin depender del personal del centro. El proceso debe ser intuitivo, seguro y eficiente, mostrando las franjas disponibles para cada médico o terapista.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-03.1 | El sistema requiere que el paciente inicie sesión con usuario y contraseña para acceder al módulo de agendamiento autónomo. |
| CA-03.2 | El sistema permite al paciente registrarse por primera vez mediante un formulario de creación de cuenta que incluye validación de correo electrónico o número telefónico. |
| CA-03.3 | El sistema presenta un asistente de agendamiento que guía al paciente paso a paso en la selección del profesional, especialidad, fecha y hora. |
| CA-03.4 | El sistema muestra las franjas horarias disponibles para cada médico/terapista según la configuración vigente (días de atención, horario y intervalo entre citas). |
| CA-03.5 | El sistema sugiere la fecha y hora más próxima disponible, teniendo en cuenta la ventana de tiempo habilitada para citas (en semanas). |
| CA-03.6 | El sistema puede mostrar varias opciones de horario disponible para que el paciente seleccione la que más le convenga. |
| CA-03.7 | Antes de confirmar, el sistema muestra un resumen de la cita para que el paciente la valide. |
| CA-03.8 | Al confirmar la cita, el sistema muestra un mensaje de confirmación con los detalles del agendamiento. |
| CA-03.9 | El sistema implementa mecanismos que dificulten el agendamiento masivo automatizado o fraudulento (por ejemplo, CAPTCHA o verificación de identidad). |
| CA-03.10 | El sistema valida que los datos ingresados por el paciente no estén incompletos o sean claramente inválidos. |
| CA-03.11 | La interfaz debe ser usable con capacitación mínima, empleando lenguaje claro y retroalimentación visual en errores y confirmaciones. |

#### Flujo principal del asistente de agendamiento

1. El paciente inicia sesión (o se registra si es la primera vez).
2. El paciente selecciona la especialidad o el médico/terapista de su preferencia.
3. El sistema muestra las fechas y franjas horarias disponibles.
4. El paciente selecciona una fecha y hora.
5. El sistema presenta un resumen de la cita.
6. El paciente confirma y el sistema registra la cita.

#### Medidas de seguridad requeridas

- Verificación de identidad al crear cuenta (correo electrónico o número telefónico).
- Controles contra creación masiva de cuentas o reservas automatizadas.
- Validación básica de datos ingresados (campos obligatorios, formatos correctos).
- Los mecanismos de seguridad no deben afectar significativamente la experiencia del usuario legítimo.

---

### RF-04 Configurar parámetros del sistema (administrador)

> 🟠 **Sprint 1** | Actor principal: **Administrador**

**Historia de usuario:**
> *"Yo como administrador necesito configurar los parámetros del sistema para que el agendamiento de citas autónomo funcione acorde a la disponibilidad de los médicos y terapistas de Piedrazul."*

#### Descripción

El sistema debe proporcionar al administrador un módulo de configuración que defina los parámetros globales y por profesional que rigen el agendamiento autónomo de citas. Estos parámetros determinan cuándo, cómo y con qué intervalos se pueden reservar citas.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-04.1 | Solo los usuarios con rol de administrador pueden acceder y modificar la configuración del sistema. |
| CA-04.2 | El administrador puede configurar la ventana de tiempo futura en la que se habilitarán citas (expresada en semanas). |
| CA-04.3 | El administrador puede configurar, para cada médico/terapista, los días de la semana en los que atiende. |
| CA-04.4 | El administrador puede configurar, para cada médico/terapista, la franja horaria de atención (hora de inicio y hora de fin). |
| CA-04.5 | El administrador puede configurar, para cada médico/terapista, el intervalo de tiempo en minutos entre cita y cita. |
| CA-04.6 | Los cambios en la configuración aplican de manera inmediata para nuevas citas; las citas ya agendadas no se ven afectadas. |
| CA-04.7 | El sistema valida que la franja horaria configurada sea coherente (hora de inicio anterior a hora de fin). |
| CA-04.8 | El sistema valida que el intervalo entre citas sea un valor positivo mayor a cero. |
| CA-04.9 | El administrador puede visualizar un resumen de la configuración actual antes de guardar cambios. |

#### Parámetros globales del sistema

| Parámetro | Descripción |
|-----------|-------------|
| Ventana de agendamiento | Número de semanas hacia adelante en las que los pacientes pueden reservar citas. |

#### Parámetros por médico / terapista

| Parámetro | Descripción | Ejemplo |
|-----------|-------------|---------|
| Días de atención | Días de la semana en los que el profesional atiende pacientes. | Lunes, Miércoles, Viernes |
| Franja horaria | Hora de inicio y hora de fin de la jornada de atención. | 07:00 AM – 11:00 AM |
| Intervalo entre citas | Tiempo en minutos que separa una cita de la siguiente. | 5, 10, 15 minutos |

---

## 4. Requisitos Funcionales Completos

> Los siguientes requisitos complementan los del Sprint 1 y conforman el alcance total del sistema. Serán implementados en los sprints 2 y 3 según se priorice con el cliente.

---

### RF-05 Gestión de usuarios del sistema

**Actor principal:** Administrador

#### Descripción

El sistema debe permitir la creación, edición, consulta y desactivación de los usuarios que interactúan con la aplicación. La gestión de usuarios es una función exclusiva del administrador.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-05.1 | El administrador puede crear nuevos usuarios del sistema con todos los campos requeridos. |
| CA-05.2 | El nombre de usuario (login) debe ser único en el sistema; el sistema debe validar esto al crear o editar. |
| CA-05.3 | Las contraseñas se almacenan de forma cifrada y nunca en texto plano. |
| CA-05.4 | El administrador puede asignar uno de los siguientes roles a cada usuario: Médico/Terapista, Agendador de citas, Administrador. |
| CA-05.5 | Cuando el rol asignado es Médico/Terapista, el sistema permite asociar el usuario con el registro del médico o terapista correspondiente. |
| CA-05.6 | El administrador puede cambiar el estado de un usuario entre activo e inactivo; un usuario inactivo no puede iniciar sesión. |
| CA-05.7 | El administrador puede editar los datos de un usuario existente (excepto el login, que es inmutable). |
| CA-05.8 | El administrador puede consultar el listado de usuarios con filtros por rol y estado. |
| CA-05.9 | (Opcional) El sistema permite la recuperación de contraseña mediante preguntas de seguridad o correo electrónico. |

#### Datos del usuario

| Campo | Obligatorio | Descripción |
|-------|-------------|-------------|
| Nombre de usuario (login) | Sí | Único en el sistema |
| Contraseña | Sí | Almacenada de forma cifrada |
| Nombre completo | Sí | |
| Rol | Sí | Médico/Terapista, Agendador, Administrador |
| Estado | Sí | Activo / Inactivo |
| Asociación con médico/terapista | Condicional | Requerido si el rol es Médico/Terapista |

---

### RF-06 Autenticación y control de acceso

**Actor principal:** Todos los roles

#### Descripción

El sistema debe contar con un mecanismo de autenticación basado en usuario y contraseña, y debe controlar el acceso a las funcionalidades según el rol del usuario autenticado.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-06.1 | El sistema presenta una pantalla de inicio de sesión con campos de nombre de usuario y contraseña. |
| CA-06.2 | Tras una autenticación exitosa, el sistema redirige al usuario a la pantalla principal correspondiente a su rol. |
| CA-06.3 | El sistema restringe el acceso a funcionalidades según el rol del usuario; cada usuario solo puede realizar las acciones autorizadas para su perfil. |
| CA-06.4 | Un usuario inactivo no puede iniciar sesión; el sistema muestra un mensaje informativo. |
| CA-06.5 | El sistema cierra la sesión automáticamente tras un periodo configurable de inactividad. |
| CA-06.6 | (Opcional) El sistema registra los intentos fallidos de inicio de sesión en el log de auditoría. |

#### Matriz de acceso por rol (resumen)

| Funcionalidad | Paciente | Agendador | Médico/Terapista | Administrador |
|---------------|----------|-----------|------------------|---------------|
| Agendar cita autónoma | ✅ | — | — | — |
| Listar citas | — | ✅ | ✅ | ✅ |
| Crear cita manual | — | ✅ | ✅ | ✅ |
| Re-agendar cita | — | ✅ | ✅ | ✅ |
| Exportar citas | — | ✅ | ✅ | ✅ |
| Historia clínica | — | — | ✅ | ✅ |
| Gestión de usuarios | — | — | — | ✅ |
| Gestión de médicos | — | — | — | ✅ |
| Configurar parámetros | — | — | — | ✅ |
| Ver auditoría | — | — | — | ✅ |
| Ver reportes | — | — | ✅ | ✅ |

---

### RF-07 Gestión de médicos y terapistas

**Actor principal:** Administrador

#### Descripción

El sistema debe permitir administrar el catálogo de médicos y terapistas del centro, incluyendo la creación, edición, consulta y cambio de estado de cada profesional.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-07.1 | El administrador puede crear nuevos médicos o terapistas con todos los campos requeridos. |
| CA-07.2 | El administrador puede editar los datos de un profesional existente. |
| CA-07.3 | El administrador puede cambiar el estado de un profesional entre activo e inactivo. |
| CA-07.4 | Solo los profesionales con estado activo pueden ser asignados a nuevas citas. |
| CA-07.5 | El administrador puede consultar el listado de profesionales con filtros por tipo, especialidad y estado. |
| CA-07.6 | (Opcional) El sistema permite configurar los horarios de atención (días y franjas) por cada profesional directamente en su ficha. |

#### Datos del médico / terapista

| Campo | Obligatorio | Opciones |
|-------|-------------|---------|
| Nombres completos | Sí | |
| Tipo de profesional | Sí | Médico / Terapista |
| Especialidad | Sí | Terapia neural / Quiropraxia / Fisioterapia |
| Intervalo de atención (minutos) | Sí | Valor entero positivo |
| Estado | Sí | Activo / Inactivo |

---

### RF-08 Re-agendamiento de citas

**Actor principal:** Agendador de citas, Médico/Terapista

#### Descripción

El sistema debe permitir modificar la fecha y/o hora de una cita ya registrada, conservando el historial de cambios asociados a cada re-agendamiento.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-08.1 | El sistema permite buscar una cita existente para proceder con el re-agendamiento. |
| CA-08.2 | El usuario puede seleccionar una nueva fecha y una nueva hora disponible para el mismo médico, u opcionalmente cambiar el médico asignado. |
| CA-08.3 | El selector de nueva hora muestra únicamente franjas disponibles según la configuración del médico seleccionado. |
| CA-08.4 | El sistema registra en el historial de la cita: fecha del cambio, usuario que realizó el re-agendamiento, fecha/hora anterior y nueva fecha/hora. |
| CA-08.5 | No se permite re-agendar una cita a una fecha u hora que ya esté ocupada por otro paciente con el mismo médico. |
| CA-08.6 | Al confirmar el re-agendamiento, el sistema muestra un mensaje de éxito con el resumen del cambio realizado. |

---

### RF-09 Exportación de citas

**Actor principal:** Agendador de citas, Médico/Terapista, Administrador

#### Descripción

El sistema debe permitir exportar el listado de citas de un médico o terapista en una fecha específica a un formato de texto compatible con hojas de cálculo, para facilitar la organización diaria del centro médico.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-09.1 | La opción de exportación está disponible desde el listado de citas (RF-01). |
| CA-09.2 | El archivo exportado tiene formato CSV (valores separados por comas o punto y coma), compatible con Microsoft Excel y LibreOffice Calc. |
| CA-09.3 | El archivo incluye como mínimo: hora de la cita, nombre completo del paciente y observaciones. |
| CA-09.4 | El nombre del archivo exportado incluye el nombre del médico y la fecha de las citas (por ejemplo: `citas_DrCordoba_2026-01-28.csv`). |
| CA-09.5 | El archivo puede ser abierto, editado e impreso desde una aplicación de hoja de cálculo estándar. |

---

### RF-10 Historia clínica básica

**Actor principal:** Médico / Terapista

#### Descripción

El sistema debe permitir al médico o terapista registrar el control o procedimiento realizado durante una consulta, asociándolo a la cita y al paciente correspondiente.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-10.1 | El médico puede registrar una historia clínica básica desde la vista de detalle de una cita atendida. |
| CA-10.2 | Cada registro clínico almacena como mínimo: fecha y hora de la atención, profesional que realizó el control y descripción del procedimiento realizado. |
| CA-10.3 | La historia clínica de un paciente puede contener múltiples registros, uno por cada consulta atendida. |
| CA-10.4 | El médico puede consultar el historial de controles previos de un paciente. |
| CA-10.5 | La información clínica es accesible únicamente para usuarios con rol de médico/terapista o administrador. |
| CA-10.6 | Una vez registrado, un control clínico no puede eliminarse; solo puede editarse mediante mecanismos controlados que conserven el historial de modificaciones. |
| CA-10.7 | Toda consulta o modificación de la historia clínica queda registrada en el sistema de auditoría. |

#### Datos del registro clínico

| Campo | Obligatorio |
|-------|-------------|
| Fecha y hora de la atención | Sí (automático) |
| Profesional que realizó el control | Sí (automático, usuario en sesión) |
| Descripción del procedimiento o control médico | Sí |

---

### RF-11 Auditoría del sistema

**Actor principal:** Administrador

#### Descripción

El sistema debe registrar automáticamente las acciones relevantes realizadas por los usuarios, permitiendo su consulta posterior para fines de control, seguimiento y verificación.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-11.1 | El sistema registra automáticamente cada acción crítica sin requerir intervención del usuario. |
| CA-11.2 | Cada registro de auditoría incluye: identificador del usuario que ejecutó la acción, descripción de la acción realizada, y fecha y hora exactas del evento. |
| CA-11.3 | Las acciones auditadas incluyen al menos: creación, modificación y desactivación de usuarios; agendamiento y re-agendamiento de citas; y registro y modificación de historias clínicas. |
| CA-11.4 | Los registros de auditoría son de solo lectura; no pueden modificarse ni eliminarse. |
| CA-11.5 | Solo el administrador puede consultar los registros de auditoría. |
| CA-11.6 | El administrador puede filtrar los registros por usuario, tipo de acción y rango de fechas. |

#### Eventos auditados

| Categoría | Eventos |
|-----------|---------|
| Usuarios | Creación, edición, desactivación |
| Citas | Agendamiento, re-agendamiento, cancelación |
| Historia clínica | Registro de controles, edición de controles, consulta |
| Sesiones | (Opcional) Intentos fallidos de inicio de sesión |

---

### RF-12 Reportes y estadísticas

**Actor principal:** Administrador, Médico/Terapista

#### Descripción

El sistema debe permitir la generación de reportes estadísticos sobre la actividad del centro médico, presentados de forma clara mediante tablas y gráficos.

#### Criterios de aceptación

| ID | Criterio |
|----|----------|
| CA-12.1 | El sistema genera un reporte de cantidad de citas por mes para un año seleccionado. |
| CA-12.2 | El sistema genera un reporte de cantidad de citas por médico o terapista. |
| CA-12.3 | El sistema genera comparativos de citas por especialidad. |
| CA-12.4 | Los reportes se presentan mediante tablas y gráficas de barras, líneas o similares. |
| CA-12.5 | El usuario puede aplicar filtros de fecha, médico y especialidad para acotar los resultados. |
| CA-12.6 | (Opcional) Los reportes pueden exportarse en formato PDF o Excel. |

---

## 5. Matriz de Priorización

| ID | Requisito | Sprint | Valor para el cliente | Complejidad |
|----|-----------|--------|-----------------------|-------------|
| RF-01 | Listar citas por médico y fecha | 🟠 Sprint 1 | Alta | Baja |
| RF-02 | Crear cita manualmente | 🟠 Sprint 1 | Alta | Media |
| RF-03 | Agendar cita autónoma (paciente) | 🟠 Sprint 1 | Alta | Alta |
| RF-04 | Configurar parámetros del sistema | 🟠 Sprint 1 | Alta | Media |
| RF-05 | Gestión de usuarios | Sprint 2 | Alta | Media |
| RF-06 | Autenticación y control de acceso | Sprint 2 | Alta | Media |
| RF-07 | Gestión de médicos y terapistas | Sprint 2 | Alta | Baja |
| RF-08 | Re-agendamiento de citas | Sprint 2 | Media | Media |
| RF-09 | Exportación de citas | Sprint 2 | Media | Baja |
| RF-10 | Historia clínica básica | Sprint 3 | Media | Media |
| RF-11 | Auditoría del sistema | Sprint 3 | Media | Media |
| RF-12 | Reportes y estadísticas | Sprint 3 | Media | Media |

---

*Documento elaborado para el proyecto de clase 2026.1 — Ingeniería de Software III, Universidad del Cauca.*