-- init.sql
-- Referencias de prueba: keycloak_id debe existir en el realm.
INSERT INTO usuarios (id, keycloak_id, username, rol, estado, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'kc-admin-demo', 'admin', 'ADMIN', 'ACTIVO', NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'kc-paciente-demo', 'paciente1', 'PACIENTE', 'ACTIVO', NOW(), NOW()),
('33333333-3333-3333-3333-333333333333', 'kc-medico-demo', 'medico1', 'MEDICO', 'ACTIVO', NOW(), NOW());

INSERT INTO medicos (usuario_id, nombres, apellidos, especialidad, tipo, estado) VALUES
('33333333-3333-3333-3333-333333333333', 'Carlos', 'Ramirez', 'QUIROPRAXIA', 'MEDICO', 'ACTIVO');

INSERT INTO pacientes (usuario_id, documento, nombres, apellidos, celular, genero, created_at) VALUES
('22222222-2222-2222-2222-222222222222', '123456789', 'Juan', 'Perez', '3001234567', 'MASCULINO', NOW());
