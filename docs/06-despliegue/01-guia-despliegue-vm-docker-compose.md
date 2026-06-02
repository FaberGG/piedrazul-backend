# Guia de despliegue en VM (Docker Compose + Nginx)

Esta guia describe el despliegue del backend en una VM usando Docker Compose y Nginx como reverse proxy.

## 1. Resumen de arquitectura (operativa)

- Monolito modular en Spring Boot (un solo contenedor de API).
- PostgreSQL como base de datos principal.
- Keycloak como Identity Provider (OIDC/OAuth2), con base de datos separada en el mismo PostgreSQL.
- Nginx como reverse proxy y terminacion TLS, con dos hostnames publicos:
  - API: `https://api.tu-dominio.com`
  - Keycloak: `https://auth.tu-dominio.com`

## 2. Requisitos de la VM

- Docker Engine + Docker Compose v2.
- Puertos 80 y 443 abiertos en la VM.
- DNS apuntando a la VM:
  - `api.tu-dominio.com` -> IP publica de la VM
  - `auth.tu-dominio.com` -> IP publica de la VM
- Certificados TLS en `docker/nginx/certs/`:
  - `fullchain.pem`
  - `privkey.pem`

## 3. Preparar variables de entorno

1. Copia la plantilla:

```bash
cp .env.prod.example .env.prod
```

2. Edita `.env.prod` y define valores reales:

- Credenciales de PostgreSQL.
- Secret del cliente `piedrazul-backend` en Keycloak.
- Hostnames publicos de Nginx.

Notas:

- El backend valida JWT con `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`, debe apuntar al dominio publico de Keycloak.
- `KEYCLOAK_SERVER_URL` se usa para el cliente admin del backend; el default `http://keycloak:8080` es interno y funciona dentro del stack.

## 4. Configurar Nginx

El proxy se genera desde una plantilla en `docker/nginx/templates/piedrazul.conf.template` usando variables de entorno:

- `NGINX_API_HOST`
- `NGINX_AUTH_HOST`

Asegura los certificados TLS en `docker/nginx/certs/`.

## 5. Primer despliegue

1. Construye e inicia el stack:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

2. Verifica servicios:

```bash
docker compose -f docker-compose.prod.yml ps
```

3. Revisa logs si es necesario:

```bash
docker compose -f docker-compose.prod.yml logs -f keycloak
```

## 6. Keycloak (realm y cliente)

- El archivo `docker/keycloak/piedrazul-realm.json` se importa en el primer arranque.
- Si el realm ya existe, puedes quitar `--import-realm` del comando en `docker-compose.prod.yml`.
- Verifica que el cliente `piedrazul-backend` tenga:
  - Client authentication habilitada.
  - Service account habilitada.
  - Permiso `realm-management -> manage-users`.

## 7. Checklist post-despliegue

- `https://auth.tu-dominio.com` responde y muestra Keycloak.
- `https://api.tu-dominio.com/api/v1/medicos` responde con `401` sin token.
- El backend valida tokens emitidos por Keycloak.

## 8. Ajustes recomendados de produccion

- Rotar secretos y usar contrasenas fuertes.
- Configurar backups de la base de datos (`piedrazul_data`).
- Revisar CORS en `application-prod.yml` antes de poner en publico.
- Ajustar rate limits en `application-prod.yml` segun el trafico real.

