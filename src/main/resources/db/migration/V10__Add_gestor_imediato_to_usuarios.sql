ALTER TABLE usuarios
ADD COLUMN gestor_imediato_id BIGINT,
ADD CONSTRAINT fk_usuarios_gestor
    FOREIGN KEY (gestor_imediato_id)
    REFERENCES usuarios(id);