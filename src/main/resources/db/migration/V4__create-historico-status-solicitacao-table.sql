-- V4__create-historico-status-solicitacao-table.sql

CREATE TABLE historico_status_solicitacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_alteracao DATETIME NOT NULL,
    status_anterior VARCHAR(50), -- Pode ser nulo para a criação inicial
    status_novo VARCHAR(50) NOT NULL,

    -- Referência à solicitação que teve seu status alterado
    solicitacao_id BIGINT NOT NULL,

    -- Quem fez a alteração. Por agora, pode ser o próprio solicitante.
    -- No futuro, com perfis de admin, poderia ser o gestor que aprovou/recusou.
    usuario_responsavel_id BIGINT NOT NULL,

    CONSTRAINT fk_hist_stat_solicitacao_id FOREIGN KEY (solicitacao_id) REFERENCES solicitacoes(id),
    CONSTRAINT fk_hist_stat_usuario_id FOREIGN KEY (usuario_responsavel_id) REFERENCES usuarios(id)
);