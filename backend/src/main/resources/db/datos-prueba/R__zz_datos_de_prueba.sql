-- MeDiTurno: datos de prueba para DESARROLLO. Nunca se cargan en producción.
--
-- Solo se ejecuta con el perfil `dev` de Spring (ver application-dev.yaml; compose.yml ya lo activa).
-- Es una migración "repetible" de Flyway (R__). Se llama R__zz_... para que corra DESPUÉS de
-- R__usuarios_de_prueba.sql (Flyway ordena las repetibles por nombre): este script reutiliza esas cuatro personas
-- con cuenta de Firebase (paciente, médico, funcionario y administrador @mediturno.com) y les agrega todo lo demás.
--
-- Es seguro en cualquier base: si ya hay otros usuarios, especialidades, turnos o reservas (alguien probó a mano), no hace nada
-- y avisa en el log. Para empezar con todos los datos:  podman compose down -v  y volver a levantar.
--
-- Las fechas son relativas a HOY en Montevideo (el día en que se cargan), igual que el reloj de la aplicación: los turnos de hoy
-- y de mañana, las fechas de las próximas 8 semanas y la historia de las últimas 13 semanas se arman a partir de esa fecha.
-- Las demás personas (@mediturno.test) no tienen firebase_uid: están "en el padrón, sin cuenta".
--
-- Qué hay y para qué sirve cada cosa: sección «Datos de prueba» de CLAUDE.md. Lo pensado para probar a mano está hecho para las
-- cuatro cuentas de Firebase (sobre todo Ana López y la Dra. Laura Gómez). El resto (el historial de las otras personas y las
-- reservas de las próximas semanas) se genera con una regla fija, para que los listados y las estadísticas tengan volumen.

DO $$
DECLARE
    v_dias CONSTANT text[] := ARRAY['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];
    -- La base puede estar en UTC (el contenedor lo está): a las 22:00 de Montevideo, CURRENT_DATE ya sería mañana.
    v_ahora CONSTANT timestamp := date_trunc('second', now() AT TIME ZONE 'America/Montevideo');
    v_hoy CONSTANT date := (now() AT TIME ZONE 'America/Montevideo')::date;

    v_admin bigint;
    v_func1 bigint;
    v_func2 bigint;
    v_laura bigint;
    v_rossi bigint;
    v_acosta bigint;
    v_cabrera bigint;
    v_nunez bigint;
    v_pereira bigint;
    v_ana bigint;
    v_luis bigint;
    v_maria bigint;
    v_pedro bigint;
    v_lucia bigint;
    v_valentina bigint;
    v_gabriela bigint;
    v_joaquin bigint;
    v_camila bigint;
    v_adultos bigint[];   -- pacientes con los que se llena el historial y las reservas futuras (todos menos Ana, Joaquín y Rosa)
    v_ninos bigint[];     -- los de Pediatría

    v_t_card bigint;
    v_t_clin bigint;
    v_t_trau bigint;
    v_t_pedi bigint;
    v_t_trau_suspendido bigint;
    v_t_pedi_suspendido bigint;
    v_t_derm bigint;
    v_t_hoy_manana bigint;
    v_t_hoy_tarde bigint;
    v_t_hoy_laura bigint;
    v_t_manana bigint;
    v_suspension_temporal bigint;
    v_suspension_permanente bigint;

    v_oc bigint;
    v_hora timestamp;   -- fecha y hora estimada de atención del número
    v_numero int;
    v_reserva bigint;
    v_orden bigint;
    v_hist bigint;
    v_hist_ana bigint;
    v_consulta bigint;
    v_pase bigint;
    v_pase_trau bigint;

    r record;
    v_pool bigint[];
    v_ocupacion int;
    v_paciente bigint;
    v_caso int;
    v_plantilla text[];
    v_k int;

    -- Contenido de las consultas que se generan: motivo, observaciones e indicaciones (puede no haberlas).
    v_consultas_card CONSTANT text[] := ARRAY[
        ARRAY['Control de presión arterial.', 'Presión arterial 135/85. Ritmo regular, sin soplos.',
              'Continuar con la medicación habitual. Dieta baja en sal. Control en tres meses.'],
        ARRAY['Palpitaciones ocasionales.', 'Electrocardiograma normal. Sin signos de arritmia durante la consulta.',
              'Holter de 24 horas. Reducir el consumo de café.'],
        ARRAY['Dolor en el pecho al hacer esfuerzos.', 'Examen físico sin alteraciones. Electrocardiograma en reposo normal.',
              'Ergometría. Consultar en emergencia si el dolor aparece en reposo.'],
        ARRAY['Control después de la ergometría.', 'Ergometría sin cambios isquémicos. Buena capacidad funcional.', NULL]];
    v_consultas_clin CONSTANT text[] := ARRAY[
        ARRAY['Control general anual.', 'Buen estado general. Peso estable. Presión arterial 120/80.',
              'Análisis de sangre de rutina. Control en un año.'],
        ARRAY['Fiebre y tos de tres días.', 'Cuadro respiratorio alto de origen viral. Pulmones sin ruidos agregados.',
              'Paracetamol si hay fiebre. Abundante líquido. Consultar si no mejora en cinco días.'],
        ARRAY['Dolor lumbar.', 'Contractura muscular paravertebral. Sin signos neurológicos.',
              'Calor local y antiinflamatorio por cinco días. Evitar levantar peso.'],
        ARRAY['Renovación de medicación crónica.', 'Paciente estable con el tratamiento actual.', 'Se renueva la receta por tres meses.'],
        ARRAY['Dolor de cabeza frecuente.', 'Examen neurológico normal. Cefalea tensional.',
              'Registrar los episodios. Analgésico común si hace falta. Control en un mes.']];
    v_consultas_pedi CONSTANT text[] := ARRAY[
        ARRAY['Control de crecimiento y desarrollo.', 'Peso y talla acordes a la edad. Desarrollo adecuado.',
              'Próximo control en seis meses.'],
        ARRAY['Fiebre y dolor de oído.', 'Otitis media aguda derecha.', 'Antibiótico por siete días. Control al terminar el tratamiento.'],
        ARRAY['Vacunación y control.', 'Esquema de vacunación al día.', NULL],
        ARRAY['Tos y mocos de una semana.', 'Resfrío común. Pulmones limpios.',
              'Lavados nasales con suero. Consultar si aparece fiebre alta o dificultad para respirar.']];
BEGIN
    -- Las cuatro personas de R__usuarios_de_prueba.sql siempre existen a esta altura; cualquier otro usuario (o dato) significa
    -- que la base ya se usó.
    IF EXISTS (SELECT 1 FROM usuario WHERE correo NOT IN ('paciente@mediturno.com', 'medico@mediturno.com',
                                                           'funcionario@mediturno.com', 'admin@mediturno.com'))
       OR EXISTS (SELECT 1 FROM especialidad) OR EXISTS (SELECT 1 FROM turno_semanal) OR EXISTS (SELECT 1 FROM reserva) THEN
        RAISE NOTICE 'La base ya tiene datos: no se cargan los datos de prueba. Para empezar de cero: podman compose down -v';
        RETURN;
    END IF;

    SELECT id_usuario INTO v_admin FROM usuario WHERE correo = 'admin@mediturno.com';
    SELECT id_usuario INTO v_func1 FROM usuario WHERE correo = 'funcionario@mediturno.com';
    SELECT id_usuario INTO v_laura FROM usuario WHERE correo = 'medico@mediturno.com';
    SELECT id_usuario INTO v_ana FROM usuario WHERE correo = 'paciente@mediturno.com';
    IF v_admin IS NULL OR v_func1 IS NULL OR v_laura IS NULL OR v_ana IS NULL THEN
        RAISE NOTICE 'Faltan los usuarios de R__usuarios_de_prueba.sql: no se cargan los datos de prueba.';
        RETURN;
    END IF;

    -- ------------------------------------------------------------------ especialidades y consultorios
    INSERT INTO especialidad (codigo, requiere_pase, nombre, descripcion, estado) VALUES
        ('CARD', FALSE, 'Cardiología', 'Diagnóstico y tratamiento de enfermedades del corazón.', 'ACTIVO'),
        ('CLIN', FALSE, 'Clínica Médica', 'Atención general de adultos.', 'ACTIVO'),
        ('PEDI', FALSE, 'Pediatría', 'Atención de niños y adolescentes.', 'ACTIVO'),
        ('TRAU', TRUE, 'Traumatología', 'Lesiones de huesos, articulaciones y músculos. Requiere pase.', 'ACTIVO'),
        ('DERM', TRUE, 'Dermatología', 'Enfermedades de la piel. Requiere pase.', 'ACTIVO'),
        ('NEUR', FALSE, 'Neurología', 'Especialidad inactiva (sirve para probar los casos de especialidad inactiva).', 'INACTIVO');

    INSERT INTO consultorio (codigo, ubicacion, nombre, descripcion, estado) VALUES
        ('C01', 'Planta baja, ala norte', 'Consultorio 1', NULL, 'ACTIVO'),
        ('C02', 'Planta baja, ala norte', 'Consultorio 2', NULL, 'ACTIVO'),
        ('C03', 'Primer piso', 'Consultorio 3', NULL, 'ACTIVO'),
        ('C04', 'Primer piso', 'Consultorio 4', 'Consultorio inactivo (en reformas).', 'INACTIVO'),
        ('C05', 'Segundo piso', 'Consultorio 5', 'Turnos de la tarde.', 'ACTIVO');

    -- ------------------------------------------------------------------ usuarios internos
    -- El administrador (Carlos Méndez), el funcionario Juan Pérez y la médica Laura Gómez ya los crea R__usuarios_de_prueba.sql.
    INSERT INTO usuario (correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, telefono)
    VALUES ('funcionario2@mediturno.test', 'ACTIVO', v_ahora - INTERVAL '1 year', '9.000.005-5', 'Marta', 'Silva', '1990-11-02', '099 000 003')
    RETURNING id_usuario INTO v_func2;
    INSERT INTO funcionario (id_usuario) VALUES (v_func2);

    INSERT INTO medico_especialidad (id_medico, codigo_especialidad) VALUES (v_laura, 'CARD'), (v_laura, 'CLIN');

    INSERT INTO usuario (correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, telefono)
    VALUES ('diego.rossi@mediturno.test', 'ACTIVO', v_ahora - INTERVAL '1 year', '9.000.012-2', 'Diego', 'Rossi', '1979-09-30', '099 000 012')
    RETURNING id_usuario INTO v_rossi;
    INSERT INTO medico (id_usuario, matricula) VALUES (v_rossi, 'MAT-1002');
    INSERT INTO medico_especialidad (id_medico, codigo_especialidad) VALUES (v_rossi, 'TRAU');

    INSERT INTO usuario (correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, telefono)
    VALUES ('sofia.acosta@mediturno.test', 'ACTIVO', v_ahora - INTERVAL '1 year', '9.000.013-3', 'Sofía', 'Acosta', '1983-01-18', '099 000 013')
    RETURNING id_usuario INTO v_acosta;
    INSERT INTO medico (id_usuario, matricula) VALUES (v_acosta, 'MAT-1003');
    INSERT INTO medico_especialidad (id_medico, codigo_especialidad) VALUES (v_acosta, 'PEDI');

    INSERT INTO usuario (correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, telefono)
    VALUES ('andres.cabrera@mediturno.test', 'ACTIVO', v_ahora - INTERVAL '1 year', '9.000.014-4', 'Andrés', 'Cabrera', '1981-12-05', '099 000 014')
    RETURNING id_usuario INTO v_cabrera;
    INSERT INTO medico (id_usuario, matricula) VALUES (v_cabrera, 'MAT-1004');
    INSERT INTO medico_especialidad (id_medico, codigo_especialidad) VALUES (v_cabrera, 'CLIN');

    INSERT INTO usuario (correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, telefono)
    VALUES ('valeria.nunez@mediturno.test', 'ACTIVO', v_ahora - INTERVAL '1 month', '9.000.015-5', 'Valeria', 'Núñez', '1986-04-27', '099 000 015')
    RETURNING id_usuario INTO v_nunez;
    INSERT INTO medico (id_usuario, matricula) VALUES (v_nunez, 'MAT-1005');
    INSERT INTO medico_especialidad (id_medico, codigo_especialidad) VALUES (v_nunez, 'DERM');

    -- Médico INACTIVO (ya no trabaja en la institución): no figura en las listas de médicos ni se le pueden dar turnos.
    INSERT INTO usuario (correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, telefono)
    VALUES ('ricardo.pereira@mediturno.test', 'INACTIVO', v_ahora - INTERVAL '3 years', '9.000.016-6', 'Ricardo', 'Pereira', '1962-07-14', '099 000 016')
    RETURNING id_usuario INTO v_pereira;
    INSERT INTO medico (id_usuario, matricula) VALUES (v_pereira, 'MAT-1006');
    INSERT INTO medico_especialidad (id_medico, codigo_especialidad) VALUES (v_pereira, 'CARD');

    -- ------------------------------------------------------------------ pacientes (padrón, sin cuenta)
    -- Ana López ya existe (R__usuarios_de_prueba.sql, paciente@mediturno.com): se completan sus datos de contacto.
    UPDATE usuario SET telefono = '099 000 021' WHERE id_usuario = v_ana;
    UPDATE paciente SET domicilio = 'Av. Italia 1234' WHERE id_usuario = v_ana;

    -- Joaquín Viera no tiene historia clínica (CU-22 y CU-23 con un paciente sin antecedentes); Rosa Álvarez está INACTIVA;
    -- Pedro Castro no tiene teléfono ni domicilio (son opcionales).
    WITH datos (correo, estado, cedula, nombre, apellido, nacimiento, telefono, domicilio) AS (VALUES
        ('luis.diaz@mediturno.test',          'ACTIVO',   '9.000.022-2', 'Luis',      'Díaz',      '1992-08-25', '099 000 022', '18 de Julio 567'),
        ('maria.fernandez@mediturno.test',    'ACTIVO',   '9.000.023-3', 'María',     'Fernández', '1970-02-03', '099 000 023', 'Bulevar Artigas 890'),
        ('pedro.castro@mediturno.test',       'ACTIVO',   '9.000.024-4', 'Pedro',     'Castro',    '1965-10-19', NULL,          NULL),
        ('lucia.ramos@mediturno.test',        'ACTIVO',   '9.000.025-5', 'Lucía',     'Ramos',     '2001-06-30', '099 000 025', 'Rivera 2020'),
        ('jorge.benitez@mediturno.test',      'ACTIVO',   '9.000.026-6', 'Jorge',     'Benítez',   '1958-03-11', '099 000 026', 'Av. Millán 3456'),
        ('valentina.olivera@mediturno.test',  'ACTIVO',   '9.000.027-7', 'Valentina', 'Olivera',   '1995-12-01', '099 000 027', 'Colonia 1122'),
        ('martin.sosa@mediturno.test',        'ACTIVO',   '9.000.028-8', 'Martín',    'Sosa',      '1987-05-09', '099 000 028', 'Mercedes 845'),
        ('carolina.techera@mediturno.test',   'ACTIVO',   '9.000.029-9', 'Carolina',  'Techera',   '1979-09-17', '099 000 029', 'Av. Brasil 2750'),
        ('federico.pintos@mediturno.test',    'ACTIVO',   '9.000.030-0', 'Federico',  'Pintos',    '1990-01-23', '099 000 030', 'Garibaldi 1580'),
        ('gabriela.rodriguez@mediturno.test', 'ACTIVO',   '9.000.031-1', 'Gabriela',  'Rodríguez', '1968-11-30', '099 000 031', 'Agraciada 2210'),
        ('tomas.ferreira@mediturno.test',     'ACTIVO',   '9.000.032-2', 'Tomás',     'Ferreira',  '2014-08-04', '099 000 032', 'Canelones 980'),
        ('camila.ferreira@mediturno.test',    'ACTIVO',   '9.000.033-3', 'Camila',    'Ferreira',  '2018-02-19', '099 000 032', 'Canelones 980'),
        ('joaquin.viera@mediturno.test',      'ACTIVO',   '9.000.034-4', 'Joaquín',   'Viera',     '1999-10-06', '099 000 034', 'Maldonado 1345'),
        ('rosa.alvarez@mediturno.test',       'INACTIVO', '9.000.035-5', 'Rosa',      'Álvarez',   '1950-04-22', '099 000 035', 'Soriano 760')),
    nuevos AS (
        INSERT INTO usuario (correo, estado, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, telefono)
        SELECT correo, estado, v_ahora - INTERVAL '400 days', cedula, nombre, apellido, nacimiento::date, telefono FROM datos
        RETURNING id_usuario, correo)
    INSERT INTO paciente (id_usuario, domicilio, fecha_alta)
    SELECT n.id_usuario, d.domicilio, v_ahora - INTERVAL '400 days' FROM nuevos n JOIN datos d ON d.correo = n.correo;

    SELECT id_usuario INTO v_luis FROM usuario WHERE correo = 'luis.diaz@mediturno.test';
    SELECT id_usuario INTO v_maria FROM usuario WHERE correo = 'maria.fernandez@mediturno.test';
    SELECT id_usuario INTO v_pedro FROM usuario WHERE correo = 'pedro.castro@mediturno.test';
    SELECT id_usuario INTO v_lucia FROM usuario WHERE correo = 'lucia.ramos@mediturno.test';
    SELECT id_usuario INTO v_valentina FROM usuario WHERE correo = 'valentina.olivera@mediturno.test';
    SELECT id_usuario INTO v_gabriela FROM usuario WHERE correo = 'gabriela.rodriguez@mediturno.test';
    SELECT id_usuario INTO v_joaquin FROM usuario WHERE correo = 'joaquin.viera@mediturno.test';
    SELECT id_usuario INTO v_camila FROM usuario WHERE correo = 'camila.ferreira@mediturno.test';
    -- En un orden fijo (por cédula): así los datos generados salen siempre iguales.
    SELECT array_agg(u.id_usuario ORDER BY u.cedula) INTO v_adultos
    FROM usuario u JOIN paciente p ON p.id_usuario = u.id_usuario
    WHERE u.estado = 'ACTIVO' AND u.fecha_nacimiento < '2005-01-01' AND u.id_usuario NOT IN (v_ana, v_joaquin);
    SELECT array_agg(u.id_usuario ORDER BY u.cedula) INTO v_ninos
    FROM usuario u JOIN paciente p ON p.id_usuario = u.id_usuario
    WHERE u.fecha_nacimiento >= '2005-01-01';

    -- ------------------------------------------------------------------ turnos semanales
    -- Los turnos fijos atienden siempre el mismo día y rigen desde hace 13 semanas (tienen historia). Ningún médico ni consultorio
    -- tiene dos turnos superpuestos (CU-12 y CU-14 lo validan).
    -- Cardiología, lunes 9 a 12, 9 cupos (20 minutos por consulta)
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_laura, 'CARD', 'C01', 'LUNES', '09:00', '12:00', 9, v_hoy - 91, 'ACTIVO') RETURNING id_turno_semanal INTO v_t_card;
    -- Clínica Médica, miércoles 14 a 17, 12 cupos
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_laura, 'CLIN', 'C02', 'MIERCOLES', '14:00', '17:00', 12, v_hoy - 91, 'ACTIVO') RETURNING id_turno_semanal INTO v_t_clin;
    -- Traumatología (requiere pase), jueves 14 a 17, 8 cupos
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_rossi, 'TRAU', 'C03', 'JUEVES', '14:00', '17:00', 8, v_hoy - 91, 'ACTIVO') RETURNING id_turno_semanal INTO v_t_trau;
    -- Pediatría, martes 8 a 11, 6 cupos
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_acosta, 'PEDI', 'C01', 'MARTES', '08:00', '11:00', 6, v_hoy - 91, 'ACTIVO') RETURNING id_turno_semanal INTO v_t_pedi;
    -- Traumatología, viernes 9 a 12: SUSPENDIDO temporalmente (CU-13, CU-14, CU-20)
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_rossi, 'TRAU', 'C03', 'VIERNES', '09:00', '12:00', 10, v_hoy - 91, 'SUSPENDIDO') RETURNING id_turno_semanal INTO v_t_trau_suspendido;
    -- Pediatría, sábado 9 a 12: SUSPENDIDO de forma permanente
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_acosta, 'PEDI', 'C02', 'SABADO', '09:00', '12:00', 6, v_hoy - 91, 'SUSPENDIDO') RETURNING id_turno_semanal INTO v_t_pedi_suspendido;
    -- Dermatología (requiere pase), martes 14 a 17: turno nuevo, vigente desde hoy y solo por 4 semanas (vigente_hasta)
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, vigente_hasta, estado)
    VALUES (v_nunez, 'DERM', 'C03', 'MARTES', '14:00', '17:00', 8, v_hoy, v_hoy + 27, 'ACTIVO') RETURNING id_turno_semanal INTO v_t_derm;

    -- Turnos que caen HOY y MAÑANA (su día de la semana depende del día de carga). Los tres de hoy cubren de corrido la ventana
    -- para emitir órdenes (CU-16: desde una hora antes de que empieza el turno hasta que termina): de 7 a 21, a cualquier hora que
    -- se pruebe hay una reserva vigente a la que se le puede emitir la orden.
    -- Clínica Médica, hoy de 8 a 12 (Dr. Cabrera)
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_cabrera, 'CLIN', 'C05', v_dias[EXTRACT(ISODOW FROM v_hoy)::int], '08:00', '12:00', 10, v_hoy - 28, 'ACTIVO')
    RETURNING id_turno_semanal INTO v_t_hoy_manana;
    -- Clínica Médica, hoy de 13 a 17 (Dr. Cabrera)
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_cabrera, 'CLIN', 'C05', v_dias[EXTRACT(ISODOW FROM v_hoy)::int], '13:00', '17:00', 10, v_hoy - 28, 'ACTIVO')
    RETURNING id_turno_semanal INTO v_t_hoy_tarde;
    -- Cardiología, hoy de 18 a 21 (Dra. Gómez, la médica con cuenta): la atención del día de CU-21, CU-22, CU-23 y CU-24
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_laura, 'CARD', 'C02', v_dias[EXTRACT(ISODOW FROM v_hoy)::int], '18:00', '21:00', 9, v_hoy - 28, 'ACTIVO')
    RETURNING id_turno_semanal INTO v_t_hoy_laura;
    -- Clínica Médica, mañana de 18 a 20 (faltan menos de 48 horas: sus reservas no se pueden cancelar, CU-9)
    INSERT INTO turno_semanal (id_medico, codigo_especialidad, codigo_consultorio, dia, hora_inicio, hora_fin, cupo_maximo, vigente_desde, estado)
    VALUES (v_cabrera, 'CLIN', 'C05', v_dias[EXTRACT(ISODOW FROM v_hoy + 1)::int], '18:00', '20:00', 10, v_hoy - 28, 'ACTIVO')
    RETURNING id_turno_semanal INTO v_t_manana;

    -- Suspensión temporal: rige desde hace una semana y termina dentro de dos. Las fechas posteriores se pueden reservar.
    INSERT INTO suspension_turno (id_turno_semanal, id_funcionario, periodo, motivo, suspension_tipo, fecha_inicio, fecha_fin, fecha_registro)
    VALUES (v_t_trau_suspendido, v_func1,
            'Temporal: del ' || to_char(v_hoy - 7, 'DD/MM/YYYY') || ' al ' || to_char(v_hoy + 14, 'DD/MM/YYYY'),
            'Congreso médico', 'TEMPORAL', v_hoy - 7, v_hoy + 14, v_ahora - INTERVAL '7 days')
    RETURNING id_suspension_turno INTO v_suspension_temporal;
    -- Suspensión permanente: se registró hace 20 días y rige desde hace 14.
    INSERT INTO suspension_turno (id_turno_semanal, id_funcionario, periodo, motivo, suspension_tipo, fecha_inicio, fecha_fin, fecha_registro)
    VALUES (v_t_pedi_suspendido, v_func2, 'Permanente desde el ' || to_char(v_hoy - 14, 'DD/MM/YYYY'),
            'La médica dejó de atender los sábados', 'PERMANENTE', v_hoy - 14, NULL, v_ahora - INTERVAL '20 days')
    RETURNING id_suspension_turno INTO v_suspension_permanente;

    -- ------------------------------------------------------------------ ocurrencias (las fechas de cada turno)
    -- Desde que rige el turno hasta las próximas 8 semanas (lo que se puede reservar, igual que la tarea programada): las pasadas
    -- quedan FINALIZADA, las cubiertas por una suspensión SUSPENDIDA y las demás HABILITADA. De una suspensión solo existen las
    -- fechas que ya estaban generadas al registrarla (las 8 semanas siguientes); las posteriores no se crean.
    INSERT INTO ocurrencia_turno (id_turno_semanal, fecha, cupos, hora_inicio, hora_fin, ocurrencia_turno_estado)
    SELECT t.id_turno_semanal, d.fecha, t.cupo_maximo, t.hora_inicio, t.hora_fin,
           CASE WHEN s.id_suspension_turno IS NOT NULL THEN 'SUSPENDIDA'
                WHEN d.fecha < v_hoy THEN 'FINALIZADA'
                ELSE 'HABILITADA' END
    FROM turno_semanal t
    CROSS JOIN LATERAL (SELECT g::date AS fecha
                        FROM generate_series(t.vigente_desde, LEAST(COALESCE(t.vigente_hasta, v_hoy + 56), v_hoy + 56), INTERVAL '1 day') AS g) AS d
    LEFT JOIN suspension_turno s ON s.id_turno_semanal = t.id_turno_semanal AND s.fecha_rehabilitacion IS NULL
                                AND d.fecha >= s.fecha_inicio AND (s.fecha_fin IS NULL OR d.fecha <= s.fecha_fin)
    WHERE t.dia = v_dias[EXTRACT(ISODOW FROM d.fecha)::int]
      AND (s.id_suspension_turno IS NULL OR d.fecha <= s.fecha_registro::date + 56);

    -- ------------------------------------------------------------------ historia clínica de Ana López (paciente@mediturno.com)
    -- Cinco atenciones con tres médicos y tres especialidades (CU-11 y CU-22: filtros por período, especialidad y médico), una
    -- reserva cancelada y otra vencida (CU-10), y tres pases: uno ya usado, uno vencido y uno vigente a Traumatología.
    INSERT INTO historia_clinica (id_paciente, antecedentes, fecha_apertura, fecha_actualizacion)
    VALUES (v_ana, 'Sin alergias conocidas. Hipertensión arterial controlada con enalapril 10 mg.', v_ahora - INTERVAL '1 year', v_ahora)
    RETURNING id_historia_clinica INTO v_hist_ana;

    -- Hace unas 13 semanas, Clínica Médica con la Dra. Gómez: la deriva a Traumatología y a Dermatología.
    v_numero := 1;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_clin AND fecha BETWEEN v_hoy - 91 AND v_hoy - 85;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, v_numero, 'ATENDIDA', v_hora - INTERVAL '6 days') RETURNING id_reserva INTO v_reserva;
    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_ana, v_func1, v_reserva, v_hora - INTERVAL '20 minutes', 'UTILIZADA') RETURNING numero_orden INTO v_orden;
    INSERT INTO consulta (id_paciente, id_medico, id_historia_clinica, id_reserva, numero_orden, fecha, motivo, observaciones, indicaciones, fecha_registro)
    VALUES (v_ana, v_laura, v_hist_ana, v_reserva, v_orden, v_hora,
            'Dolor e inflamación en el tobillo izquierdo después de una torcedura. Manchas que pican en los antebrazos.',
            'Tobillo con edema y dolor a la palpación del ligamento externo. Lesiones eritematosas descamativas en ambos antebrazos.',
            'Hielo y reposo relativo. Se deriva a Traumatología y a Dermatología.', v_hora + INTERVAL '15 minutes')
    RETURNING id_consulta INTO v_consulta;
    INSERT INTO pase (id_paciente, id_consulta, especialidad_origen, especialidad_destino, fecha_emision, pase_estado)
    VALUES (v_ana, v_consulta, 'CLIN', 'TRAU', v_hora + INTERVAL '15 minutes', 'USADO') RETURNING id_pase INTO v_pase;
    -- No lo usó y pasaron los 60 días: con él no puede reservar Dermatología.
    INSERT INTO pase (id_paciente, id_consulta, especialidad_origen, especialidad_destino, fecha_emision, pase_estado)
    VALUES (v_ana, v_consulta, 'CLIN', 'DERM', v_hora + INTERVAL '15 minutes', 'VENCIDO');

    -- Unas dos semanas después, Traumatología con el Dr. Rossi, reservada con el pase anterior.
    v_numero := 3;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_trau AND fecha BETWEEN v_hoy - 77 AND v_hoy - 71;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, id_pase)
    VALUES (v_ana, v_oc, v_numero, 'ATENDIDA', v_hora - INTERVAL '5 days', v_pase) RETURNING id_reserva INTO v_reserva;
    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_ana, v_func2, v_reserva, v_hora - INTERVAL '30 minutes', 'UTILIZADA') RETURNING numero_orden INTO v_orden;
    INSERT INTO consulta (id_paciente, id_medico, id_historia_clinica, id_reserva, numero_orden, fecha, motivo, observaciones, indicaciones, fecha_registro)
    VALUES (v_ana, v_rossi, v_hist_ana, v_reserva, v_orden, v_hora,
            'Dolor en el tobillo izquierdo dos semanas después de una torcedura.',
            'Esguince de tobillo grado I. Radiografía sin lesiones óseas.',
            'Tobillera por tres semanas y ejercicios de movilidad. Control si el dolor no mejora.', v_hora + INTERVAL '15 minutes');

    -- Hace unas 6 semanas, Cardiología: no se presentó (VENCIDA).
    SELECT id_ocurrencia_turno, fecha + hora_inicio INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_card AND fecha BETWEEN v_hoy - 42 AND v_hoy - 36;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, 4, 'VENCIDA', v_hora - INTERVAL '9 days');

    -- Hace unas 5 semanas, Clínica Médica: la canceló ella con tiempo.
    SELECT id_ocurrencia_turno, fecha + hora_inicio INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_clin AND fecha BETWEEN v_hoy - 35 AND v_hoy - 29;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, fecha_cancelacion, motivo_cancelacion)
    VALUES (v_ana, v_oc, 3, 'CANCELADA', v_hora - INTERVAL '10 days', v_hora - INTERVAL '4 days', 'Cancelada por el paciente');

    -- Hace unas 3 semanas, Clínica Médica con la Dra. Gómez: pase VIGENTE a Traumatología (vale 60 días).
    v_numero := 2;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_clin AND fecha BETWEEN v_hoy - 27 AND v_hoy - 21;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, v_numero, 'ATENDIDA', v_hora - INTERVAL '8 days') RETURNING id_reserva INTO v_reserva;
    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_ana, v_func1, v_reserva, v_hora - INTERVAL '25 minutes', 'UTILIZADA') RETURNING numero_orden INTO v_orden;
    INSERT INTO consulta (id_paciente, id_medico, id_historia_clinica, id_reserva, numero_orden, fecha, motivo, observaciones, indicaciones, fecha_registro)
    VALUES (v_ana, v_laura, v_hist_ana, v_reserva, v_orden, v_hora,
            'Dolor en la rodilla derecha al subir escaleras.',
            'Dolor a la palpación en la cara interna de la rodilla. Sin derrame. Movilidad conservada.',
            'Antiinflamatorio por cinco días. Se deriva a Traumatología.', v_hora + INTERVAL '15 minutes')
    RETURNING id_consulta INTO v_consulta;
    INSERT INTO pase (id_paciente, id_consulta, especialidad_origen, especialidad_destino, fecha_emision, pase_estado)
    VALUES (v_ana, v_consulta, 'CLIN', 'TRAU', v_hora + INTERVAL '15 minutes', 'VIGENTE') RETURNING id_pase INTO v_pase_trau;

    -- Hace unas 2 semanas, Cardiología con la Dra. Gómez.
    v_numero := 2;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_card AND fecha BETWEEN v_hoy - 14 AND v_hoy - 8;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, v_numero, 'ATENDIDA', v_hora - INTERVAL '5 days') RETURNING id_reserva INTO v_reserva;
    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_ana, v_func1, v_reserva, v_hora - INTERVAL '20 minutes', 'UTILIZADA') RETURNING numero_orden INTO v_orden;
    INSERT INTO consulta (id_paciente, id_medico, id_historia_clinica, id_reserva, numero_orden, fecha, motivo, observaciones, indicaciones, fecha_registro)
    VALUES (v_ana, v_laura, v_hist_ana, v_reserva, v_orden, v_hora,
            'Control cardiológico anual y palpitaciones ocasionales al subir escaleras.',
            'Electrocardiograma: ritmo sinusal, sin alteraciones. Presión arterial 130/85. Se mantiene la medicación.',
            'Realizar ergometría y ecocardiograma. Dieta baja en sal. Caminatas de 30 minutos, cinco veces por semana.',
            v_hora + INTERVAL '15 minutes')
    RETURNING id_consulta INTO v_consulta;
    INSERT INTO notificacion (id_usuario, id_consulta, fecha, estado, tipo, mensaje, canal, fecha_envio)
    SELECT v_ana, v_consulta, v_hora + INTERVAL '15 minutes', estado, 'CONSULTA_REGISTRADA',
           'Se incorporó nueva información a su historia clínica a partir de su consulta del ' || to_char(v_hora, 'DD/MM/YYYY')
           || '. Puede consultarla desde MeDiTurno.', canal, v_hora + INTERVAL '15 minutes'
    FROM (VALUES ('LEIDA', 'APLICACION'), ('ENVIADA', 'EMAIL')) AS c(estado, canal);

    -- Hace una semana, Clínica Médica con el Dr. Cabrera (otro médico).
    v_numero := 1;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_hoy_manana AND fecha = v_hoy - 7;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, v_numero, 'ATENDIDA', v_hora - INTERVAL '3 days') RETURNING id_reserva INTO v_reserva;
    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_ana, v_func2, v_reserva, v_hora - INTERVAL '30 minutes', 'UTILIZADA') RETURNING numero_orden INTO v_orden;
    INSERT INTO consulta (id_paciente, id_medico, id_historia_clinica, id_reserva, numero_orden, fecha, motivo, observaciones, indicaciones, fecha_registro)
    VALUES (v_ana, v_cabrera, v_hist_ana, v_reserva, v_orden, v_hora,
            'Fiebre, dolor de garganta y tos de tres días.',
            'Faringitis viral. Temperatura 38,2 °C. Pulmones limpios.',
            'Paracetamol 1 g cada 8 horas si hay fiebre. Abundante líquido. Consultar si persiste más de cinco días.',
            v_hora + INTERVAL '15 minutes')
    RETURNING id_consulta INTO v_consulta;
    INSERT INTO notificacion (id_usuario, id_consulta, fecha, estado, tipo, mensaje, canal, fecha_envio)
    SELECT v_ana, v_consulta, v_hora + INTERVAL '15 minutes', estado, 'CONSULTA_REGISTRADA',
           'Se incorporó nueva información a su historia clínica a partir de su consulta del ' || to_char(v_hora, 'DD/MM/YYYY')
           || '. Puede consultarla desde MeDiTurno.', canal, v_hora + INTERVAL '15 minutes'
    FROM (VALUES ('LEIDA', 'APLICACION'), ('ENVIADA', 'EMAIL')) AS c(estado, canal);

    -- ------------------------------------------------------------------ reservas de HOY
    -- Turno de la Dra. Gómez (18 a 21): Ana con la orden emitida (CU-23 y, después, CU-24), Joaquín con la orden emitida y sin
    -- historia clínica (CU-22 vacía; CU-23 le abre la historia), Pedro vigente (falta emitir la orden, CU-16) y una cancelada.
    SELECT id_ocurrencia_turno INTO v_oc FROM ocurrencia_turno WHERE id_turno_semanal = v_t_hoy_laura AND fecha = v_hoy;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, 1, 'ORDEN_EMITIDA', v_ahora - INTERVAL '6 days') RETURNING id_reserva INTO v_reserva;
    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_ana, v_func1, v_reserva, v_ahora, 'EMITIDA');
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_joaquin, v_oc, 3, 'ORDEN_EMITIDA', v_ahora - INTERVAL '4 days') RETURNING id_reserva INTO v_reserva;
    INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
    VALUES (v_joaquin, v_func2, v_reserva, v_ahora, 'EMITIDA');
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, fecha_cancelacion, motivo_cancelacion) VALUES
        (v_pedro, v_oc, 5, 'VIGENTE', v_ahora - INTERVAL '3 days', NULL, NULL),
        (v_valentina, v_oc, 7, 'CANCELADA', v_ahora - INTERVAL '10 days', v_ahora - INTERVAL '3 days', 'Cancelada por el paciente');

    -- Turnos del Dr. Cabrera (8 a 12 y 13 a 17): reservas vigentes para emitir la orden en la mañana y en la tarde (CU-15, CU-16).
    SELECT id_ocurrencia_turno INTO v_oc FROM ocurrencia_turno WHERE id_turno_semanal = v_t_hoy_manana AND fecha = v_hoy;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva) VALUES
        (v_lucia, v_oc, 3, 'VIGENTE', v_ahora - INTERVAL '2 days'),
        (v_maria, v_oc, 6, 'VIGENTE', v_ahora - INTERVAL '5 days');
    SELECT id_ocurrencia_turno INTO v_oc FROM ocurrencia_turno WHERE id_turno_semanal = v_t_hoy_tarde AND fecha = v_hoy;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva) VALUES
        (v_luis, v_oc, 2, 'VIGENTE', v_ahora - INTERVAL '1 day'),
        (v_gabriela, v_oc, 5, 'VIGENTE', v_ahora - INTERVAL '6 days');

    -- ------------------------------------------------------------------ reservas próximas
    -- MAÑANA: Ana y Lucía a menos de 48 horas (no se pueden cancelar, CU-9).
    v_numero := 2;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_manana AND fecha = v_hoy + 1;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, v_numero, 'VIGENTE', v_ahora - INTERVAL '4 days') RETURNING id_reserva INTO v_reserva;
    INSERT INTO notificacion (id_usuario, id_reserva, fecha, estado, tipo, mensaje, canal, fecha_envio)
    VALUES (v_ana, v_reserva, v_ahora - INTERVAL '4 days', 'ENVIADA', 'RESERVA_CONFIRMADA',
            'Su reserva ' || v_reserva || ' quedó confirmada: Clínica Médica el ' || to_char(v_hora, 'DD/MM/YYYY') || ', número '
            || v_numero || ', hora estimada de atención ~' || to_char(v_hora, 'HH24:MI') || '.', 'APLICACION', v_ahora - INTERVAL '4 days');
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_lucia, v_oc, 3, 'VIGENTE', v_ahora - INTERVAL '5 days');

    -- Cardiología, la primera fecha a 3 días o más (siempre se puede cancelar): Luis, Ana y María.
    v_numero := 5;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_card AND fecha >= v_hoy + 3 ORDER BY fecha LIMIT 1;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    VALUES (v_ana, v_oc, v_numero, 'VIGENTE', v_ahora - INTERVAL '1 day') RETURNING id_reserva INTO v_reserva;
    INSERT INTO notificacion (id_usuario, id_reserva, fecha, estado, tipo, mensaje, canal, fecha_envio)
    VALUES (v_ana, v_reserva, v_ahora - INTERVAL '1 day', 'ENVIADA', 'RESERVA_CONFIRMADA',
            'Su reserva ' || v_reserva || ' quedó confirmada: Cardiología el ' || to_char(v_hora, 'DD/MM/YYYY') || ', número '
            || v_numero || ', hora estimada de atención ~' || to_char(v_hora, 'HH24:MI') || '.', 'APLICACION', v_ahora - INTERVAL '1 day');
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva) VALUES
        (v_luis, v_oc, 1, 'VIGENTE', v_ahora - INTERVAL '2 days'),
        (v_maria, v_oc, 7, 'VIGENTE', v_ahora - INTERVAL '1 day');

    -- Traumatología, la primera fecha a 3 días o más: Ana, con su pase vigente.
    v_numero := 3;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_trau AND fecha >= v_hoy + 3 ORDER BY fecha LIMIT 1;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, id_pase)
    VALUES (v_ana, v_oc, v_numero, 'VIGENTE', v_ahora - INTERVAL '2 days', v_pase_trau) RETURNING id_reserva INTO v_reserva;
    INSERT INTO notificacion (id_usuario, id_reserva, fecha, estado, tipo, mensaje, canal, fecha_envio)
    VALUES (v_ana, v_reserva, v_ahora - INTERVAL '2 days', 'LEIDA', 'RESERVA_CONFIRMADA',
            'Su reserva ' || v_reserva || ' quedó confirmada: Traumatología el ' || to_char(v_hora, 'DD/MM/YYYY') || ', número '
            || v_numero || ', hora estimada de atención ~' || to_char(v_hora, 'HH24:MI') || '.', 'APLICACION', v_ahora - INTERVAL '2 days');

    -- Clínica Médica, la primera fecha a 3 días o más: una reserva cancelada (su número quedó libre).
    SELECT id_ocurrencia_turno INTO v_oc FROM ocurrencia_turno
    WHERE id_turno_semanal = v_t_clin AND fecha >= v_hoy + 3 ORDER BY fecha LIMIT 1;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, fecha_cancelacion, motivo_cancelacion)
    VALUES (v_maria, v_oc, 4, 'CANCELADA', v_ahora - INTERVAL '6 days', v_ahora - INTERVAL '4 days', 'Cancelada por el paciente');

    -- Turno del Dr. Cabrera de 13 a 17 de la semana que viene: SIN CUPOS (CU-8 no lo ofrece).
    SELECT id_ocurrencia_turno INTO v_oc FROM ocurrencia_turno WHERE id_turno_semanal = v_t_hoy_tarde AND fecha = v_hoy + 7;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
    SELECT v_adultos[n], v_oc, n, 'VIGENTE', v_ahora - make_interval(days => n % 5, hours => n)
    FROM generate_series(1, 10) AS n;

    -- Anuladas por las suspensiones, con su aviso. La de Ana se había reservado con el pase vigente.
    v_numero := 4;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_trau_suspendido AND fecha >= v_hoy ORDER BY fecha LIMIT 1;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, fecha_cancelacion, motivo_cancelacion, id_pase)
    VALUES (v_ana, v_oc, v_numero, 'ANULADA', v_ahora - INTERVAL '15 days', v_ahora - INTERVAL '7 days',
            'Turno suspendido (suspensión #' || v_suspension_temporal || ')', v_pase_trau)
    RETURNING id_reserva INTO v_reserva;
    INSERT INTO notificacion (id_usuario, id_reserva, fecha, estado, tipo, mensaje, canal, fecha_envio)
    SELECT v_ana, v_reserva, v_ahora - INTERVAL '7 days', 'ENVIADA', 'SUSPENSION_TURNO',
           'Su reserva de Traumatología con Diego Rossi para el ' || to_char(v_hora, 'DD/MM/YYYY')
           || ' (turno de 09:00 a 12:00, número ' || v_numero || ') fue anulada porque el turno fue suspendido. Motivo: Congreso médico.'
           || ' Puede reservar otro turno desde MeDiTurno.', canal, v_ahora - INTERVAL '7 days'
    FROM (VALUES ('APLICACION'), ('EMAIL')) AS c(canal);

    v_numero := 2;
    SELECT id_ocurrencia_turno, fecha + hora_inicio + (v_numero - 1) * (hora_fin - hora_inicio) / cupos INTO v_oc, v_hora
    FROM ocurrencia_turno WHERE id_turno_semanal = v_t_pedi_suspendido AND fecha >= v_hoy ORDER BY fecha LIMIT 1;
    INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, fecha_cancelacion, motivo_cancelacion)
    VALUES (v_camila, v_oc, v_numero, 'ANULADA', v_ahora - INTERVAL '25 days', v_ahora - INTERVAL '20 days',
            'Turno suspendido (suspensión #' || v_suspension_permanente || ')')
    RETURNING id_reserva INTO v_reserva;
    -- El correo falló (FALLIDA): así se ve ese estado.
    INSERT INTO notificacion (id_usuario, id_reserva, fecha, estado, tipo, mensaje, canal, fecha_envio)
    SELECT v_camila, v_reserva, v_ahora - INTERVAL '20 days', estado, 'SUSPENSION_TURNO',
           'Su reserva de Pediatría con Sofía Acosta para el ' || to_char(v_hora, 'DD/MM/YYYY')
           || ' (turno de 09:00 a 12:00, número ' || v_numero || ') fue anulada porque el turno fue suspendido. Motivo: '
           || 'La médica dejó de atender los sábados. Puede reservar otro turno desde MeDiTurno.', canal,
           CASE WHEN estado = 'ENVIADA' THEN v_ahora - INTERVAL '20 days' END
    FROM (VALUES ('ENVIADA', 'APLICACION'), ('FALLIDA', 'EMAIL')) AS c(estado, canal);

    -- ------------------------------------------------------------------ historial de los demás pacientes (generado)
    -- Llena las fechas pasadas de las especialidades sin pase con una regla fija: Cardiología con más demanda que Clínica Médica,
    -- y Pediatría solo con los dos niños. De cada reserva, la mayoría ATENDIDA (con su orden y su consulta), algunas VENCIDA (no
    -- se presentó) y pocas CANCELADA. Sirve para CU-10, CU-26, las estadísticas y los antecedentes de CU-22.
    FOR r IN
        SELECT o.id_ocurrencia_turno AS id, o.fecha, o.hora_inicio, o.hora_fin, o.cupos, t.id_medico, t.codigo_especialidad AS especialidad,
               (v_hoy - o.fecha) / 7 AS semana
        FROM ocurrencia_turno o
        JOIN turno_semanal t ON t.id_turno_semanal = o.id_turno_semanal
        JOIN especialidad e ON e.codigo = t.codigo_especialidad
        WHERE o.ocurrencia_turno_estado = 'FINALIZADA' AND NOT e.requiere_pase
        ORDER BY o.fecha, o.id_ocurrencia_turno
    LOOP
        v_pool := CASE WHEN r.especialidad = 'PEDI' THEN v_ninos ELSE v_adultos END;
        v_ocupacion := CASE r.especialidad WHEN 'CARD' THEN 8 WHEN 'CLIN' THEN 6 ELSE 10 END;   -- de cada 10 números
        v_plantilla := CASE r.especialidad WHEN 'CARD' THEN v_consultas_card WHEN 'PEDI' THEN v_consultas_pedi ELSE v_consultas_clin END;
        FOR n IN 1 .. LEAST(r.cupos, array_length(v_pool, 1)) LOOP
            CONTINUE WHEN (n * 7 + r.semana * 3) % 10 >= v_ocupacion;
            v_paciente := v_pool[1 + (n - 1 + r.semana) % array_length(v_pool, 1)];
            CONTINUE WHEN EXISTS (SELECT 1 FROM reserva WHERE id_ocurrencia_turno = r.id AND (numero = n OR id_paciente = v_paciente));
            v_hora := r.fecha + r.hora_inicio + (n - 1) * (r.hora_fin - r.hora_inicio) / r.cupos;
            v_caso := (n * 3 + r.semana * 7 + (r.id % 5)::int) % 20;
            IF v_caso < 2 THEN
                INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva, fecha_cancelacion, motivo_cancelacion)
                VALUES (v_paciente, r.id, n, 'CANCELADA', v_hora - INTERVAL '10 days', v_hora - INTERVAL '3 days', 'Cancelada por el paciente');
            ELSIF v_caso < 5 THEN
                INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
                VALUES (v_paciente, r.id, n, 'VENCIDA', v_hora - INTERVAL '8 days');
            ELSE
                INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
                VALUES (v_paciente, r.id, n, 'ATENDIDA', v_hora - make_interval(days => 2 + n % 12)) RETURNING id_reserva INTO v_reserva;
                INSERT INTO orden (id_paciente, id_funcionario, id_reserva, fecha_emision, orden_estado)
                VALUES (v_paciente, CASE WHEN n % 2 = 0 THEN v_func1 ELSE v_func2 END, v_reserva, v_hora - INTERVAL '25 minutes', 'UTILIZADA')
                RETURNING numero_orden INTO v_orden;
                SELECT id_historia_clinica INTO v_hist FROM historia_clinica WHERE id_paciente = v_paciente;
                IF NOT FOUND THEN
                    INSERT INTO historia_clinica (id_paciente, fecha_apertura) VALUES (v_paciente, v_hora) RETURNING id_historia_clinica INTO v_hist;
                END IF;
                v_k := 1 + (n + r.semana) % array_length(v_plantilla, 1);
                INSERT INTO consulta (id_paciente, id_medico, id_historia_clinica, id_reserva, numero_orden, fecha, motivo, observaciones, indicaciones, fecha_registro)
                VALUES (v_paciente, r.id_medico, v_hist, v_reserva, v_orden, v_hora,
                        v_plantilla[v_k][1], v_plantilla[v_k][2], v_plantilla[v_k][3], v_hora + INTERVAL '15 minutes');
            END IF;
        END LOOP;
    END LOOP;

    UPDATE historia_clinica h SET antecedentes = d.antecedentes
    FROM (VALUES
        ('luis.diaz@mediturno.test', 'Asma leve en la infancia, sin crisis en los últimos años.'),
        ('maria.fernandez@mediturno.test', 'Diabetes tipo 2 en tratamiento con metformina. Alergia a la penicilina.'),
        ('pedro.castro@mediturno.test', 'Fumador de 20 cigarrillos por día. Colesterol alto.'),
        ('jorge.benitez@mediturno.test', 'Infarto agudo de miocardio hace 8 años. Hipertensión arterial.'),
        ('gabriela.rodriguez@mediturno.test', 'Hipotiroidismo en tratamiento con levotiroxina.'),
        ('tomas.ferreira@mediturno.test', 'Vacunas al día. Sin antecedentes de importancia.')) AS d(correo, antecedentes)
    JOIN usuario u ON u.correo = d.correo
    WHERE h.id_paciente = u.id_usuario;

    UPDATE historia_clinica h SET fecha_actualizacion = c.ultima
    FROM (SELECT id_historia_clinica, max(fecha_registro) AS ultima FROM consulta GROUP BY id_historia_clinica) AS c
    WHERE c.id_historia_clinica = h.id_historia_clinica;

    -- ------------------------------------------------------------------ reservas de las próximas semanas (generadas)
    -- Vigentes, cada vez menos a medida que la fecha se aleja: CU-8 muestra números ocupados y libres, y CU-13 encuentra
    -- reservas para anular en cualquier turno. No toca hoy ni mañana (ya están armados arriba) ni las especialidades con pase.
    FOR r IN
        SELECT o.id_ocurrencia_turno AS id, o.fecha, o.cupos, t.codigo_especialidad AS especialidad, (o.fecha - v_hoy) / 7 AS semana
        FROM ocurrencia_turno o
        JOIN turno_semanal t ON t.id_turno_semanal = o.id_turno_semanal
        JOIN especialidad e ON e.codigo = t.codigo_especialidad
        WHERE o.ocurrencia_turno_estado = 'HABILITADA' AND o.fecha >= v_hoy + 2 AND NOT e.requiere_pase
        ORDER BY o.fecha, o.id_ocurrencia_turno
    LOOP
        v_pool := CASE WHEN r.especialidad = 'PEDI' THEN v_ninos ELSE v_adultos END;
        v_ocupacion := GREATEST(1, 5 - r.semana / 2);
        FOR n IN 1 .. LEAST(r.cupos, array_length(v_pool, 1)) LOOP
            CONTINUE WHEN (n * 7 + r.semana * 3 + (r.id % 3)::int) % 10 >= v_ocupacion;
            v_paciente := v_pool[1 + (n + r.semana) % array_length(v_pool, 1)];
            CONTINUE WHEN EXISTS (SELECT 1 FROM reserva WHERE id_ocurrencia_turno = r.id AND (numero = n OR id_paciente = v_paciente));
            -- Reservada hace pocos días, y nunca antes de que la fecha entrara en la ventana de 8 semanas.
            INSERT INTO reserva (id_paciente, id_ocurrencia_turno, numero, reserva_estado, fecha_reserva)
            VALUES (v_paciente, r.id, n, 'VIGENTE',
                    v_ahora - make_interval(days => (n * 3) % LEAST(10, 57 - (r.fecha - v_hoy)), mins => n * 7));
        END LOOP;
    END LOOP;

    -- ------------------------------------------------------------------ cartelera
    -- Dos anuncios vigentes, uno programado (todavía no empezó), uno vencido y uno eliminado.
    INSERT INTO anuncio (id_funcionario, titulo, contenido, fecha_creacion, fecha_publicacion, fecha_inicio, fecha_fin, anuncio_estado) VALUES
        (v_func1, 'Horario de verano',
         'Durante el verano la atención en ventanilla será de 8 a 14 horas. Los turnos de la tarde se mantienen.',
         v_ahora - INTERVAL '3 days', v_ahora - INTERVAL '3 days', v_hoy - 3, v_hoy + 60, 'PUBLICADO'),
        (v_func1, 'Campaña de vacunación',
         'Está abierta la campaña de vacunación antigripal. Consulte en su turno de Clínica Médica.',
         v_ahora - INTERVAL '10 days', v_ahora - INTERVAL '10 days', v_hoy - 10, v_hoy + 30, 'PUBLICADO'),
        (v_func1, 'Nuevos turnos de Dermatología',
         'La Dra. Valeria Núñez atiende los martes de 14 a 17 horas. Recuerde que Dermatología requiere un pase de otra especialidad.',
         v_ahora - INTERVAL '1 day', v_ahora - INTERVAL '1 day', v_hoy + 3, v_hoy + 45, 'PUBLICADO'),
        (v_func2, 'Cierre por feriado',
         'La institución permanecerá cerrada el día del feriado. Las reservas de ese día fueron reprogramadas.',
         v_ahora - INTERVAL '25 days', v_ahora - INTERVAL '25 days', v_hoy - 20, v_hoy - 15, 'PUBLICADO'),
        (v_func2, 'Mantenimiento del consultorio 4',
         'El consultorio 4 permanecerá cerrado por reformas.',
         v_ahora - INTERVAL '40 days', v_ahora - INTERVAL '40 days', v_hoy - 40, v_hoy - 10, 'ELIMINADO');

    -- Imágenes (PNG de 480x200 generados para la prueba) en dos anuncios; los demás quedan sin imagen.
    INSERT INTO imagen_anuncio (id_anuncio, datos, nombre_archivo, tipo_contenido, texto_alternativo)
    SELECT id_anuncio, decode('iVBORw0KGgoAAAANSUhEUgAAAeAAAADICAIAAAC/PqUtAAACQ0lEQVR42u3dQQ0AIAwEwSIETdWGEEThBBUkLZlkFNxjn23MXAAUFCYAEGgABBpAoAEQaACBBkCgARBoAIEGQKABBBoAgQZAoAEEGgCBBhBoAAQaQKABEGgABBpAoAEQaACBBkCgARBoAIEGQKABBBoAgQYQaAAEGgCBBhBoAAQaQKABEGgABBpAoAEQaACBBkCgAQTaCgACDYBAAwg0AAININAACDQAAg0g0AAINIBAAyDQAAg0gEADINAAAg2AQAMINAACDYBAAwg0AAININAACDQAAg0g0AAINIBAAyDQAAINgEADINAAAg2AQAMINAACDYBAAwg0AAININAACDSAQFsBQKABEGgAgQZAoAEEGgCBBkCgAQQaAIEGEGgABBoAgQYQaAAEGkCgARBoAIEGQKABEGgAgQZAoAEEGgCBBkCgAQQaAIEGEGgABBpAoAEQaAAEGkCgARBoAIEGQKABEGgAgQZAoAEEGgCBBhBoAAQaAIEGEGgABBpAoAEQaAAEGkCgARBoAIEGQKABEGgAgQZAoAEEGoCHgT57AFCQQAMINAACDSDQAAg0gEADINAACDSAQAMg0AACDYBAAyDQAAINgEAD/BNoJ7EBfFQBQKABBBoAgQYQaAAEGgCBBhBoAAQaQKABEGgABBpAoAEQaACBBkCgAQQaAIEGQKABBBoAgQYQaAAEGgCBBhBoAAQaQKABEGgAgQZAoAEQaACBBkCgAQQaAIEGQKABBBoAgQYQaAAEGkCgrQAg0AAINEB7F5afza762MgnAAAAAElFTkSuQmCC', 'base64'), 'horario-verano.png', 'image/png', 'Banner azul con una franja mostaza'
    FROM anuncio WHERE titulo = 'Horario de verano';
    INSERT INTO imagen_anuncio (id_anuncio, datos, nombre_archivo, tipo_contenido, texto_alternativo)
    SELECT id_anuncio, decode('iVBORw0KGgoAAAANSUhEUgAAAeAAAADICAIAAAC/PqUtAAACQ0lEQVR42u3dMQ0AIAxFwRpBE9rqkpGZCRUkLbnkFPzhjW2sfQAoKEwAINAACDSAQAMg0AACDYBAAyDQAAINgEADCDQAAg2AQAMINAACDSDQAAg0gEADINAACDSAQAMg0AACDYBAAyDQAAINgEADCDQAAg0g0AAINAACDSDQAAg0gEADINAACDSAQAMg0AACDYBAAwi0FQAEGgCBBhBoAAQaQKABEGgABBpAoAEQaACBBkCgARBoAIEGQKABBBoAgQYQaAAEGgCBBhBoAAQaQKABEGgABBpAoAEQaACBBkCgAQQaAIEGQKABBBoAgQYQaAAEGgCBBhBoAAQaQKABEGgAgbYCgEADINAAAg2AQAMINAACDYBAAwg0AAININAACDQAAg0g0AAINIBAAyDQAAINgEADINAAAg2AQAMINAACDYBAAwg0AAININAACDSAQAMg0AAINIBAAyDQAAINgEADINAAAg2AQAMINAACDSDQAAg0AAININAACDSAQAMg0AAINIBAAyDQAAINgEADINAAAg2AQAMINAAPAz1mAlCQQAMINAACDSDQAAg0gEADINAACDSAQAMg0AACDYBAAyDQAAINgEAD/BNoJ7EBfFQBQKABBBoAgQYQaAAEGgCBBhBoAAQaQKABEGgABBpAoAEQaACBBkCgAQQaAIEGQKABBBoAgQYQaAAEGgCBBhBoAAQaQKABEGgAgQZAoAEQaACBBkCgAQQaAIEGQKABBBoAgQYQaAAEGkCgrQAg0AAINEB7F6RP4rd/WsExAAAAAElFTkSuQmCC', 'base64'), 'vacunacion.png', 'image/png', 'Banner celeste con una franja azul'
    FROM anuncio WHERE titulo = 'Campaña de vacunación';

    RAISE NOTICE 'Datos de prueba cargados.';
END $$;
