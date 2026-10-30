-- CU-47: el consultorio guarda su último cambio: qué administrador lo hizo y cuándo. Vacíos si nunca se modificó.
ALTER TABLE consultorio ADD COLUMN id_administrador_modificacion BIGINT;
ALTER TABLE consultorio ADD COLUMN fecha_modificacion TIMESTAMP;
ALTER TABLE consultorio ADD CONSTRAINT fk_consultorio_administrador_modificacion
    FOREIGN KEY (id_administrador_modificacion) REFERENCES administrador (id_usuario);
