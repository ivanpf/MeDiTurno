-- Datos sintéticos, únicamente para la base LOCAL de desarrollo.
-- No es una migración automática. Usa las cuentas de prueba existentes; no crea cuentas en Firebase.
BEGIN;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM medico m JOIN usuario u USING (id_usuario) WHERE u.correo = 'medico@mediturno.com')
    OR NOT EXISTS (SELECT 1 FROM paciente p JOIN usuario u USING (id_usuario) WHERE u.correo = 'paciente@mediturno.com') THEN
    RAISE EXCEPTION 'Primero deben existir los usuarios de prueba de MeDiTurno';
  END IF;
END $$;

INSERT INTO especialidad (codigo, nombre, descripcion, requiere_pase, estado) VALUES
  ('CU8_DEMO', 'Clínica general (demo CU-8)', 'Datos sintéticos para probar reservas desde mobile', FALSE, 'ACTIVO'),
  ('CU8_PASE', 'Especialidad con pase (demo CU-8)', 'Permite probar el rechazo sin pase vigente', TRUE, 'ACTIVO')
ON CONFLICT DO NOTHING;

INSERT INTO consultorio (codigo, nombre, ubicacion, descripcion, estado)
VALUES ('CU8_DEMO', 'Consultorio de demostración CU-8', 'Planta baja (demo)', 'Solo pruebas locales', 'ACTIVO')
ON CONFLICT DO NOTHING;

INSERT INTO medico_especialidad (id_medico, codigo_especialidad)
SELECT m.id_usuario, e.codigo FROM medico m JOIN usuario u USING (id_usuario)
CROSS JOIN especialidad e WHERE u.correo = 'medico@mediturno.com' AND e.codigo IN ('CU8_DEMO', 'CU8_PASE')
ON CONFLICT DO NOTHING;

INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia,
  hora_inicio, hora_fin, cupo_maximo, vigente_desde, vigente_hasta, estado)
SELECT m.id_usuario, e.codigo, 'CU8_DEMO',
  (ARRAY['LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO'])[EXTRACT(ISODOW FROM CURRENT_DATE + 1)::int],
  CASE WHEN e.codigo = 'CU8_DEMO' THEN TIME '09:00' ELSE TIME '10:00' END,
  CASE WHEN e.codigo = 'CU8_DEMO' THEN TIME '10:00' ELSE TIME '11:00' END,
  7, CURRENT_DATE + 1, CURRENT_DATE + 56, 'ACTIVO'
FROM medico m JOIN usuario u USING (id_usuario) CROSS JOIN especialidad e
WHERE u.correo = 'medico@mediturno.com' AND e.codigo IN ('CU8_DEMO', 'CU8_PASE')
  AND NOT EXISTS (SELECT 1 FROM turno_semanal t WHERE t.id_medico = m.id_usuario
    AND t.codigo_especialidad = e.codigo AND t.codigo_consultorio = 'CU8_DEMO');

INSERT INTO ocurrencia_turno (id_turno_semanal, fecha, cupos, hora_inicio, hora_fin, ocurrencia_turno_estado)
SELECT t.id_turno_semanal, f.fecha::date, t.cupo_maximo, t.hora_inicio, t.hora_fin, 'HABILITADA'
FROM turno_semanal t CROSS JOIN LATERAL generate_series(CURRENT_DATE + 1, CURRENT_DATE + 56, INTERVAL '1 day') f(fecha)
WHERE t.codigo_consultorio = 'CU8_DEMO' AND t.codigo_especialidad IN ('CU8_DEMO', 'CU8_PASE')
  AND (ARRAY['LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO'])[EXTRACT(ISODOW FROM f.fecha)::int] = t.dia
  AND f.fecha::date BETWEEN t.vigente_desde AND t.vigente_hasta
ON CONFLICT DO NOTHING;

-- Un número ya reservado permite verificar que la app lo muestre deshabilitado.
INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
SELECT p.id_usuario, o.id_ocurrencia_turno, 2, 'VIGENTE', LOCALTIMESTAMP
FROM paciente p JOIN usuario u USING (id_usuario)
CROSS JOIN LATERAL (
  SELECT o.id_ocurrencia_turno FROM ocurrencia_turno o JOIN turno_semanal t USING (id_turno_semanal)
  WHERE t.codigo_especialidad = 'CU8_DEMO' AND t.codigo_consultorio = 'CU8_DEMO' AND o.fecha > CURRENT_DATE
  ORDER BY o.fecha LIMIT 1
) o WHERE u.correo = 'paciente@mediturno.com'
ON CONFLICT DO NOTHING;
COMMIT;
