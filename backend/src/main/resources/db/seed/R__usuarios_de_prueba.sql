-- Un usuario de prueba por rol, vinculados a las cuentas de Firebase del proyecto mediturno-auth-db915.
-- Migración repetible de Flyway (prefijo R__): se aplica sola al arrancar el backend, después de las versionadas.
-- Es idempotente: si los usuarios ya existen, no hace nada. Si se edita este archivo, Flyway lo vuelve a aplicar.

INSERT INTO usuario (correo, estado, proveedor_acceso, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, firebase_uid) VALUES
    ('paciente@mediturno.com',    'ACTIVO', 'PASSWORD', LOCALTIMESTAMP, '9.000.001-1', 'Ana',    'López',  '1988-04-12', 'tDXxZBwlXSOUc2HlBfACrR918FI2'),
    ('medico@mediturno.com',      'ACTIVO', 'PASSWORD', LOCALTIMESTAMP, '9.000.002-2', 'Laura',  'Gómez',  '1975-05-10', 'PMcu5ZRMwnUArtkPUUePmlZvAqP2'),
    ('funcionario@mediturno.com', 'ACTIVO', 'PASSWORD', LOCALTIMESTAMP, '9.000.003-3', 'Juan',   'Pérez',  '1985-07-22', 's4T3SZjvjNRllOQJbNG0AaZ9nzl1'),
    ('admin@mediturno.com',       'ACTIVO', 'PASSWORD', LOCALTIMESTAMP, '9.000.004-4', 'Carlos', 'Méndez', '1980-03-15', 'TFAeewCHSkbrsF9AfeMGDBVtMUY2')
ON CONFLICT (correo) DO NOTHING;

INSERT INTO paciente (id_usuario, fecha_alta)
    SELECT id_usuario, LOCALTIMESTAMP FROM usuario WHERE correo = 'paciente@mediturno.com'
ON CONFLICT DO NOTHING;
INSERT INTO medico (id_usuario, matricula)
    SELECT id_usuario, 'MAT-1001' FROM usuario WHERE correo = 'medico@mediturno.com'
ON CONFLICT DO NOTHING;
INSERT INTO funcionario (id_usuario)
    SELECT id_usuario FROM usuario WHERE correo = 'funcionario@mediturno.com'
ON CONFLICT DO NOTHING;
INSERT INTO administrador (id_usuario)
    SELECT id_usuario FROM usuario WHERE correo = 'admin@mediturno.com'
ON CONFLICT DO NOTHING;
