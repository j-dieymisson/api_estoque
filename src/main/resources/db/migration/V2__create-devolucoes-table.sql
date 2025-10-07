-- V2__create-devolucoes-table.sql

CREATE TABLE devolucoes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantidade_devolvida INT NOT NULL,
    data_devolucao DATETIME NOT NULL,
    observacao TEXT,
    -- Chave estrangeira para ligar a devolução diretamente ao item específico da solicitação
    solicitacao_item_id BIGINT NOT NULL,
    CONSTRAINT fk_devolucoes_solicitacao_item_id FOREIGN KEY (solicitacao_item_id) REFERENCES solicitacao_itens(id)
);