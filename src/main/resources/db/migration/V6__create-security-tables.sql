-- Cria a tabela de cargos
CREATE TABLE cargos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE
);

-- Modifica a tabela de utilizadores
ALTER TABLE usuarios
ADD COLUMN cargo_id BIGINT,
ADD CONSTRAINT fk_usuarios_cargo_id FOREIGN KEY (cargo_id) REFERENCES cargos(id);
ALTER TABLE usuarios ADD COLUMN email VARCHAR(255);

-- INSERE OS CARGOS FIXOS DO SISTEMA
INSERT INTO cargos (id, nome) VALUES (1, 'ADMIN');
INSERT INTO cargos (id, nome) VALUES (2, 'GESTOR');
INSERT INTO cargos (id, nome) VALUES (3, 'COLABORADOR');





