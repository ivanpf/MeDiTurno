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
        ('TRAU', TRUE, 'Traumatología', 'Lesiones de huesos, articulaciones y músculos.', 'ACTIVO'),
        ('DERM', TRUE, 'Dermatología', 'Enfermedades de la piel.', 'ACTIVO'),
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
    -- Tres anuncios vigentes (dos con imagen y uno sin; «Traiga su cédula» no tiene fecha de fin y no vence), uno programado (todavía no empezó), uno vencido y uno eliminado.
    INSERT INTO anuncio (id_funcionario, titulo, contenido, fecha_creacion, fecha_publicacion, fecha_inicio, fecha_fin, anuncio_estado) VALUES
        (v_func1, 'Horario de verano',
         'Durante el verano la atención en ventanilla será de 8 a 14 horas. Los turnos de la tarde se mantienen.',
         v_ahora - INTERVAL '3 days', v_ahora - INTERVAL '3 days', v_hoy - 3, v_hoy + 60, 'PUBLICADO'),
        (v_func1, 'Campaña de vacunación',
         'Está abierta la campaña de vacunación antigripal. Consulte en su turno con su médico.',
         v_ahora - INTERVAL '10 days', v_ahora - INTERVAL '10 days', v_hoy - 10, v_hoy + 30, 'PUBLICADO'),
        (v_func2, 'Traiga su cédula',
         'Recuerde presentar su cédula de identidad en ventanilla el día de la consulta para que le emitan la orden.',
         v_ahora - INTERVAL '5 days', v_ahora - INTERVAL '5 days', v_hoy - 5, NULL, 'PUBLICADO'),
        (v_func1, 'Nuevos turnos de Dermatología',
         'La Dra. Valeria Núñez atiende los martes de 14 a 17 horas. Recuerde que Dermatología requiere un pase de otra especialidad.',
         v_ahora - INTERVAL '1 day', v_ahora - INTERVAL '1 day', v_hoy + 3, v_hoy + 45, 'PUBLICADO'),
        (v_func2, 'Cierre por feriado',
         'La institución permanecerá cerrada el día del feriado. Las reservas de ese día fueron reprogramadas.',
         v_ahora - INTERVAL '25 days', v_ahora - INTERVAL '25 days', v_hoy - 20, v_hoy - 15, 'PUBLICADO'),
        (v_func2, 'Mantenimiento del consultorio 4',
         'El consultorio 4 permanecerá cerrado por reformas.',
         v_ahora - INTERVAL '40 days', v_ahora - INTERVAL '40 days', v_hoy - 40, v_hoy - 10, 'ELIMINADO');

    -- Imágenes (ilustraciones JPG de 1000 px) en dos anuncios; los demás quedan sin imagen.
    INSERT INTO imagen_anuncio (id_anuncio, datos, nombre_archivo, tipo_contenido, texto_alternativo)
    SELECT id_anuncio, decode('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', 'base64'), 'horario-verano.jpg', 'image/jpeg', 'Ventanilla de atención con un reloj y un sol de verano'
    FROM anuncio WHERE titulo = 'Horario de verano';
    INSERT INTO imagen_anuncio (id_anuncio, datos, nombre_archivo, tipo_contenido, texto_alternativo)
    SELECT id_anuncio, decode('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', 'base64'), 'vacunacion.jpg', 'image/jpeg', 'Jeringa y frasco de vacuna junto a una cruz médica'
    FROM anuncio WHERE titulo = 'Campaña de vacunación';

    RAISE NOTICE 'Datos de prueba cargados.';
END $$;
