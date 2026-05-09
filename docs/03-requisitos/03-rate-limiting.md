# Rate Limiting (Detalle)

Este documento explica como funciona el rate limiting en el backend, la implementacion actual, la configuracion disponible y su aplicabilidad por endpoint. Tambien cubre el caso de carga masiva en desarrollo con scripts.

## 1) Objetivo

- Reducir abuso y automatizaciones no deseadas en endpoints publicos.
- Proteger recursos de lectura frecuentes usados por `PACIENTE`.
- Devolver respuestas claras cuando se excede el limite.

## 2) Alcance actual

El rate limiting se aplica solo a:

- Endpoints publicos listados en `app.rate-limiting.public-policies`.
- Endpoints accesibles por `PACIENTE` listados en `app.rate-limiting.paciente-policies`.

No aplica automaticamente a otros roles ni a endpoints no listados.

## 3) Implementacion actual

Componentes principales:

- Filtro: `src/main/java/com/piedrazul/backend/shared/security/RateLimitingFilter.java`.
- Registro en cadena de seguridad: `src/main/java/com/piedrazul/backend/shared/security/SecurityConfig.java`.
- Configuracion: `src/main/resources/application.yml` bajo `app.rate-limiting`.

### 3.1 Flujo simplificado

1. El filtro identifica si el request es autenticado y si tiene rol `PACIENTE`.
2. Se selecciona una politica que haga match por metodo + ruta.
3. Se calcula la clave (IP o `sub` del JWT).
4. Se consume un token del bucket.
5. Si no hay tokens, se responde `429` con `Retry-After`.

### 3.2 Clave de limitacion

- Politicas publicas: por IP (`X-Forwarded-For` si existe, si no `remoteAddr`).
- Politicas `PACIENTE`: por `sub` del JWT; si no existe, por `name`; si no existe, por IP.

## 4) Algoritmo usado (Bucket4j)

Se usa Bucket4j con token bucket clasico:

- Capacidad = `capacity`.
- Refill = `Refill.greedy(capacity, window)`.
- Cada request consume 1 token.

Con refill greedy, los tokens se reponen de forma continua a razon de:

```
capacity / window
```

Ejemplo: `capacity = 5` y `window = 1m` => 1 token cada 12 segundos.

### 4.1 ¿Cuando puede reintentar una IP bloqueada?

Si una IP excede el limite (por ejemplo, al registrar varios pacientes rapidamente):

- Recibe HTTP `429` y un header `Retry-After` con los segundos estimados.
- Puede reintentar cuando pase ese tiempo y el bucket tenga al menos 1 token.
- El bucket se recupera totalmente al completar el intervalo (`window`).

En el caso de `5/min`, el siguiente registro exitoso suele ocurrir ~12s despues del ultimo token consumido.

## 5) Configuracion (YAML)

Ubicacion: `src/main/resources/application.yml`.

Estructura general:

```yaml
app:
  rate-limiting:
    public-policies:
      - id: public-register-paciente
        method: POST
        path: /api/v1/auth/register/paciente
        capacity: 5
        window: 1m
    paciente-policies:
      - id: paciente-medicos
        method: GET
        path: /api/v1/medicos
        capacity: 60
        window: 1m
```

Campos por politica:

- `id`: identificador unico (se usa internamente en la clave del bucket).
- `method`: metodo HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- `path`: patron AntPathMatcher (soporta `*` y `**`).
- `capacity`: numero de solicitudes permitidas por ventana.
- `window`: duracion (formato Spring Boot, ej. `10s`, `1m`, `5m`).

Si una lista queda vacia, no se aplica rate limiting a ese grupo.

## 6) Politicas actuales y viabilidad por endpoint

Las politicas actuales son las que figuran en `app.rate-limiting`.

### 6.1 Endpoint publico

- `POST /api/v1/auth/register/paciente` (publico)
  - Viable y recomendable: es el unico endpoint publico y es tipico objetivo de abuso.
  - Impacto: si una IP registra muchos pacientes seguidos, se bloqueara temporalmente.

### 6.2 Endpoints para `PACIENTE`

- `GET /api/v1/medicos`
  - Viable: endpoint de consulta frecuente y expuesto a scraping.

- `GET /api/v1/medicos/*/configuracion`
  - Viabilidad: si el rol `PACIENTE` no lo consume, la politica no aplica.
  - Nota: si este endpoint es realmente solo admin, no tiene efecto.

- `GET /api/v1/pacientes/*`
  - Viable: el paciente consulta su propio detalle.

- `POST /api/v1/citas/autonomo`
  - Viable: evita spam de creacion de citas por el mismo paciente.

- `GET /api/v1/citas/disponibilidad/primera`
  - Viable: consulta de disponibilidad frecuente.

- `GET /api/v1/citas/disponibilidad/primera/global`
  - Viable: endpoint de recomendacion inicial en el flujo `PACIENTE`.

- `GET /api/v1/citas/disponibilidad/franjas`
  - Viable: consultas repetidas de franjas.

### 6.3 Endpoints no cubiertos

- Cualquier endpoint no listado no aplica rate limiting actualmente.
- Si se requiere proteger nuevos endpoints, deben agregarse en `app.rate-limiting`.

## 7) Respuesta cuando se excede el limite

- HTTP `429 Too Many Requests`
- Header `Retry-After` con segundos aproximados para reintentar.
- Body: `{"error":"Rate limit excedido"}`

## 8) Entorno de desarrollo y scripts de carga

Script relevante:

- `scripts/python/posts-pacientes-citas/registrar_pacientes.py` usa `POST /api/v1/auth/register/paciente`.

Si se desea poblar pacientes rapidamente en desarrollo, hay dos opciones:

### Opcion A: Aumentar limites en `application-dev.yml`

Agregar un override por perfil de desarrollo:

```yaml
app:
  rate-limiting:
    public-policies:
      - id: public-register-paciente
        method: POST
        path: /api/v1/auth/register/paciente
        capacity: 2000
        window: 1m
```

Esto permite cargas masivas sin afectar produccion.

### Opcion B: Deshabilitar rate limiting publico en dev

```yaml
app:
  rate-limiting:
    public-policies: []
```

Esto elimina el limite solo en el perfil donde se aplique.

### Recomendacion operativa para scripts

- Respetar `Retry-After` si el script recibe `429`.
- Insertar una pausa entre requests si la carga es secuencial.
- Para alta concurrencia, elevar `capacity` y reducir `window` en dev.

## 9) Limitaciones conocidas

- Los buckets son en memoria local; en despliegues con multiples instancias los limites no se comparten.
- `X-Forwarded-For` requiere que el proxy lo establezca y sea confiable.
- El rate limiting solo se aplica a endpoints listados; no es global.

## 10) Referencias

- Configuracion: `src/main/resources/application.yml`
- Filtro: `src/main/java/com/piedrazul/backend/shared/security/RateLimitingFilter.java`
- Security: `src/main/java/com/piedrazul/backend/shared/security/SecurityConfig.java`
- Script de registro masivo: `scripts/python/posts-pacientes-citas/registrar_pacientes.py`

