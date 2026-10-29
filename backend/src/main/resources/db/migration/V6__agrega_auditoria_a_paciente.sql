-- CU-44: el paciente guarda su último cambio hecho por un administrador: quién lo hizo y cuándo. Vacíos si nunca se modificó.
ALTER TABLE paciente ADD COLUMN id_administrador_modificacion BIGINT;
ALTER TABLE paciente ADD COLUMN fecha_modificacion TIMESTAMP;
ALTER TABLE paciente ADD CONSTRAINT fk_paciente_administrador_modificacion
    FOREIGN KEY (id_administrador_modificacion) REFERENCES administrador (id_usuario);
