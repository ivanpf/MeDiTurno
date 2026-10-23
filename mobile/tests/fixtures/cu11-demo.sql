-- Datos sintéticos, únicamente para la base LOCAL de desarrollo.
-- No es una migración automática. Usa las cuentas de prueba existentes; no crea cuentas en Firebase.
-- Crea cinco atenciones finalizadas (reserva ATENDIDA, orden UTILIZADA y consulta registrada) de
-- paciente@mediturno.com con medico@mediturno.com, en dos especialidades y en fechas pasadas, para probar CU-11.
-- Si el consultorio CU11_DEMO ya tiene turnos, no hace nada (se puede ejecutar más de una vez).
BEGIN;
DO $$
DECLARE
  v_paciente BIGINT;
  v_medico BIGINT;
  v_funcionario BIGINT;
  v_historia BIGINT;
  v_turno BIGINT;
  v_ocurrencia BIGINT;
  v_reserva BIGINT;
  v_orden BIGINT;
  v_dia VARCHAR(10);
  v_fecha DATE;
  a RECORD;
BEGIN
  SELECT p.id_usuario INTO v_paciente FROM paciente p JOIN usuario u USING (id_usuario) WHERE u.correo = 'paciente@mediturno.com';
  SELECT m.id_usuario INTO v_medico FROM medico m JOIN usuario u USING (id_usuario) WHERE u.correo = 'medico@mediturno.com';
  SELECT f.id_usuario INTO v_funcionario FROM funcionario f JOIN usuario u USING (id_usuario) WHERE u.correo = 'funcionario@mediturno.com';
  IF v_paciente IS NULL OR v_medico IS NULL OR v_funcionario IS NULL THEN
    RAISE EXCEPTION 'Primero deben existir los usuarios de prueba de MeDiTurno';
  END IF;
  IF EXISTS (SELECT 1 FROM turno_semanal WHERE codigo_consultorio = 'CU11_DEMO') THEN
    RAISE NOTICE 'Los datos de CU-11 ya estaban cargados';
    RETURN;
  END IF;

  INSERT INTO especialidad (codigo, nombre, descripcion, requiere_pase, estado) VALUES
    ('CU11_CLIN', 'Clínica general (demo CU-11)', 'Datos sintéticos para probar la historia clínica', FALSE, 'ACTIVO'),
    ('CU11_CARD', 'Cardiología (demo CU-11)', 'Datos sintéticos para probar la historia clínica', FALSE, 'ACTIVO')
  ON CONFLICT DO NOTHING;
  INSERT INTO consultorio (codigo, nombre, ubicacion, descripcion, estado)
  VALUES ('CU11_DEMO', 'Consultorio de demostración CU-11', 'Primer piso (demo)', 'Solo pruebas locales', 'ACTIVO')
  ON CONFLICT DO NOTHING;
  INSERT INTO medico_especialidad (id_medico, codigo_especialidad)
  VALUES (v_medico, 'CU11_CLIN'), (v_medico, 'CU11_CARD')
  ON CONFLICT DO NOTHING;

  -- Una historia clínica por paciente: se reutiliza si ya la abrió CU-23.
  SELECT id_historia_clinica INTO v_historia FROM historia_clinica WHERE id_paciente = v_paciente ORDER BY id_historia_clinica LIMIT 1;
  IF v_historia IS NULL THEN
    INSERT INTO historia_clinica (id_paciente, fecha_apertura) VALUES (v_paciente, LOCALTIMESTAMP - INTERVAL '120 days')
    RETURNING id_historia_clinica INTO v_historia;
  END IF;

  -- Todas las fechas son múltiplos de 7 días hacia atrás: caen en el mismo día de la semana que hoy.
  v_dia := (ARRAY['LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO'])[EXTRACT(ISODOW FROM CURRENT_DATE)::int];

  FOR a IN SELECT * FROM (VALUES
    ('CU11_CLIN', TIME '09:00', 84, 'Control anual de salud.',
      'Paciente asintomático. Presión arterial 120/80. Examen físico sin particularidades.',
      'Solicitar hemograma y perfil lipídico. Control en 12 meses.'),
    ('CU11_CARD', TIME '10:00', 63, 'Palpitaciones ocasionales.',
      'Electrocardiograma en ritmo sinusal, sin alteraciones. Auscultación normal.',
      'Reducir el consumo de cafeína. Volver a consultar si las palpitaciones persisten.'),
    ('CU11_CLIN', TIME '09:00', 42, 'Dolor de garganta y fiebre de 38 °C desde hace dos días.',
      'Faringe congestiva, sin placas. Sin adenopatías.',
      'Paracetamol 1 g cada 8 horas si hay fiebre. Abundante líquido. Reposo por 48 horas.'),
    ('CU11_CARD', TIME '10:00', 21, 'Control de resultados del Holter.',
      'Holter de 24 horas sin arritmias significativas.',
      NULL),
    ('CU11_CLIN', TIME '09:00', 7, 'Control de resultados de laboratorio.',
      'Hemograma normal. Colesterol LDL levemente elevado.',
      'Dieta baja en grasas saturadas y actividad física 30 minutos diarios. Repetir el perfil lipídico en 6 meses.')
  ) AS t(especialidad, hora_inicio, dias_atras, motivo, observaciones, indicaciones)
  LOOP
    v_fecha := CURRENT_DATE - a.dias_atras;

    SELECT id_turno_semanal INTO v_turno FROM turno_semanal
    WHERE codigo_consultorio = 'CU11_DEMO' AND codigo_especialidad = a.especialidad;
    IF v_turno IS NULL THEN
      INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin,
        cupo_maximo, vigente_desde, vigente_hasta, estado)
      VALUES (v_medico, a.especialidad, 'CU11_DEMO', v_dia, a.hora_inicio, a.hora_inicio + INTERVAL '1 hour',
        4, CURRENT_DATE - 90, CURRENT_DATE - 1, 'ACTIVO')
      RETURNING id_turno_semanal INTO v_turno;
    END IF;

    INSERT INTO ocurrencia_turno (id_turno_semanal, fecha, cupos, hora_inicio, hora_fin, ocurrencia_turno_estado)
    VALUES (v_turno, v_fecha, 4, a.hora_inicio, a.hora_inicio + INTERVAL '1 hour', 'FINALIZADA')
    RETURNING id_ocurrencia_turno INTO v_ocurrencia;

    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_paciente, v_ocurrencia, 1, 'ATENDIDA', v_fecha - 5 + TIME '12:00')
    RETURNING id_reserva INTO v_reserva;

    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_paciente, v_funcionario, v_reserva, v_fecha + a.hora_inicio - INTERVAL '30 minutes', 'UTILIZADA')
    RETURNING numero_orden INTO v_orden;

    INSERT INTO consulta (id_paciente, id_medico, id_historia_clinica, id_reserva, numero_orden, fecha,
      motivo, observaciones, indicaciones, fecha_registro)
    VALUES (v_paciente, v_medico, v_historia, v_reserva, v_orden, v_fecha + a.hora_inicio,
      a.motivo, a.observaciones, a.indicaciones, v_fecha + a.hora_inicio + INTERVAL '20 minutes');
  END LOOP;
END $$;
COMMIT;
