-- Este script se ejecuta al iniciar el contenedor de postgres por primera vez
-- Crea la base de datos separada para Keycloak

CREATE DATABASE keycloak_db
    WITH
    OWNER = piedrazul_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;

GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO piedrazul_user;