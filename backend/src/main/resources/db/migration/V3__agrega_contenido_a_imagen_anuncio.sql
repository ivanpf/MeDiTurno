-- CU-17: el archivo de la imagen de un anuncio se guarda en la base (columna "datos").
-- Cada anuncio tiene como máximo una imagen, y ya no hacen falta la ruta ("imagen") ni el orden.
ALTER TABLE imagen_anuncio ADD COLUMN datos BYTEA;
ALTER TABLE imagen_anuncio DROP COLUMN imagen;
ALTER TABLE imagen_anuncio DROP COLUMN orden;
ALTER TABLE imagen_anuncio ADD CONSTRAINT uq_imagen_anuncio_id_anuncio UNIQUE (id_anuncio);
