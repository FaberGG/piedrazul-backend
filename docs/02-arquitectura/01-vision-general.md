# Arquitectura - Vision General

## Estilo arquitectonico

El backend esta implementado como **monolito modular** con Spring Boot.

Este enfoque permite:

- Un despliegue simple (una sola aplicacion).
- Transacciones consistentes en una misma base de datos.
- Limites de modulo claros para evolucion futura.
- Menor complejidad operativa para el contexto academico y de negocio.

## Capas principales

El flujo tecnico sigue una arquitectura en capas:

1. **Controller**: expone endpoints HTTP y valida entrada.
2. **Service**: aplica reglas de negocio.
3. **Repository**: acceso a persistencia con Spring Data JPA.
4. **DB**: almacenamiento relacional (PostgreSQL en dev, H2 en test).

## Tecnologias base del proyecto

Con base en `pom.xml` y configuraciones activas:

- Java 17
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Security (JWT)
- Spring Data JPA (Hibernate)
- PostgreSQL (dev)
- H2 (test)
- Spring Modulith
- Swagger/OpenAPI (`springdoc-openapi`)
- Lombok
- MapStruct
- JJWT (`io.jsonwebtoken`)

## Perfiles y entorno

Configuracion observada en recursos del proyecto:

- Perfil por defecto: `dev` (`src/main/resources/application.yml`)
- Base de datos dev: PostgreSQL local (`application-dev.yml`)
- Base de datos test: H2 en memoria (`src/test/resources/application-test.yml`)

## Principios de diseno aplicados

- Separacion por dominio funcional (`auth`, `agenda`, `medicos`, `pacientes`, `reportes`).
- Uso de contratos publicos para comunicacion entre modulos.
- Encapsulamiento de detalles internos (repositorios/entidades fuera del alcance externo).
- Validacion de fronteras modulares con `ModularityTest`.

