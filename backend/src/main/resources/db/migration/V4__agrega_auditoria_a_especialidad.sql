-- CU-43: la especialidad guarda su último cambio: qué administrador lo hizo y cuándo. Vacíos si nunca se modificó.
ALTER TABLE especialidad ADD COLUMN id_administrador_modificacion BIGINT;
ALTER TABLE especialidad ADD COLUMN fecha_modificacion TIMESTAMP;
ALTER TABLE especialidad ADD CONSTRAINT fk_especialidad_administrador_modificacion
    FOREIGN KEY (id_administrador_modificacion) REFERENCES administrador (id_usuario);
