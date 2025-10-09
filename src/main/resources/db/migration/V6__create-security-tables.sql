-- Tabela para guardar as permissões atómicas do sistema
CREATE TABLE permissoes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);

-- Tabela para guardar os cargos (grupos de permissões)
CREATE TABLE cargos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE
);

-- Tabela de ligação Muitos-para-Muitos entre Cargos e Permissões
CREATE TABLE cargos_permissoes (
    cargo_id BIGINT NOT NULL,
    permissao_id BIGINT NOT NULL,
    PRIMARY KEY (cargo_id, permissao_id),
    CONSTRAINT fk_cargos_permissoes_cargo FOREIGN KEY (cargo_id) REFERENCES cargos(id),
    CONSTRAINT fk_cargos_permissoes_permissao FOREIGN KEY (permissao_id) REFERENCES permissoes(id)
);

-- Modificar a tabela de utilizadores para adicionar a ligação ao seu cargo e email
ALTER TABLE usuarios
ADD COLUMN cargo_id BIGINT,
ADD CONSTRAINT fk_usuarios_cargo_id FOREIGN KEY (cargo_id) REFERENCES cargos(id);
ALTER TABLE usuarios ADD COLUMN email VARCHAR(255);


