-- CU-45 y CU-46: los usuarios internos guardan su último cambio hecho por un administrador (modificación de datos o activación
-- e inactivación): quién lo hizo y cuándo. Vacíos si nunca se modificó. Va en la tabla de cada rol, como en paciente (V6).
ALTER TABLE administrador ADD COLUMN id_administrador_modificacion BIGINT;
ALTER TABLE administrador ADD COLUMN fecha_modificacion TIMESTAMP;
ALTER TABLE administrador ADD CONSTRAINT fk_administrador_administrador_modificacion
    FOREIGN KEY (id_administrador_modificacion) REFERENCES administrador (id_usuario);

ALTER TABLE funcionario ADD COLUMN id_administrador_modificacion BIGINT;
ALTER TABLE funcionario ADD COLUMN fecha_modificacion TIMESTAMP;
ALTER TABLE funcionario ADD CONSTRAINT fk_funcionario_administrador_modificacion
    FOREIGN KEY (id_administrador_modificacion) REFERENCES administrador (id_usuario);

ALTER TABLE medico ADD COLUMN id_administrador_modificacion BIGINT;
ALTER TABLE medico ADD COLUMN fecha_modificacion TIMESTAMP;
ALTER TABLE medico ADD CONSTRAINT fk_medico_administrador_modificacion
    FOREIGN KEY (id_administrador_modificacion) REFERENCES administrador (id_usuario);
