CREATE TABLE setores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- (Opcional) Podemos já inserir alguns setores padrão
INSERT INTO setores (nome, ativo) VALUES ('TI', TRUE);
INSERT INTO setores (nome, ativo) VALUES ('Financeiro', TRUE);
INSERT INTO setores (nome, ativo) VALUES ('Logística', TRUE);