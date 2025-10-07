-- V1__create-initial-tables.sql

-- Tabela para armazenar as categorias dos equipamentos
CREATE TABLE categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela para armazenar os usuários do sistema
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    login VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela principal de equipamentos
CREATE TABLE equipamentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    quantidade_total INT NOT NULL,
    quantidade_disponivel INT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    categoria_id BIGINT,
    CONSTRAINT fk_equipamentos_categoria_id FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- Tabela para registrar as solicitações feitas pelos usuários
CREATE TABLE solicitacoes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_solicitacao DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL, -- Ex: PENDENTE, APROVADA, RECUSADA, FINALIZADA
    justificativa TEXT,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_solicitacoes_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tabela de ligação para registrar quais equipamentos e quantidades estão em cada solicitação
CREATE TABLE solicitacao_itens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantidade_solicitada INT NOT NULL,
    solicitacao_id BIGINT NOT NULL,
    equipamento_id BIGINT NOT NULL,
    CONSTRAINT fk_solicitacao_itens_solicitacao_id FOREIGN KEY (solicitacao_id) REFERENCES solicitacoes(id),
    CONSTRAINT fk_solicitacao_itens_equipamento_id FOREIGN KEY (equipamento_id) REFERENCES equipamentos(id)
);