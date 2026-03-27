-- init.sql
INSERT INTO usuarios (username, password, rol, estado, created_at, updated_at) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMINISTRADOR', 'ACTIVO', NOW(), NOW()),
('agendador1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'AGENDADOR', 'ACTIVO', NOW(), NOW()),
('paciente1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PACIENTE', 'ACTIVO', NOW(), NOW()),
('medico1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'MEDICO_TERAPISTA', 'ACTIVO', NOW(), NOW());

INSERT INTO medicos (usuario_id, nombres, apellidos, especialidad, tipo, estado) VALUES
(4, 'Carlos', 'Ramirez', 'QUIROPRAXIA', 'MEDICO', 'ACTIVO');

INSERT INTO pacientes (usuario_id, documento, nombres, apellidos, celular, genero, created_at) VALUES
(3, '123456789', 'Juan', 'Perez', '3001234567', 'MASCULINO', NOW());