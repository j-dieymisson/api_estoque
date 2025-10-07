
CREATE TABLE historico_movimentacoes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_movimentacao DATETIME NOT NULL,
    tipo_movimentacao VARCHAR(50) NOT NULL, -- Ex: 'SAIDA', 'DEVOLUCAO', 'AJUSTE'
    quantidade INT NOT NULL,

    -- Referências para dar contexto à movimentação
    equipamento_id BIGINT NOT NULL,
    usuario_responsavel_id BIGINT NOT NULL, -- Usuário que fez a solicitação original
    solicitacao_id BIGINT, -- A qual solicitação essa movimentação pertence
    devolucao_id BIGINT,   -- Se for uma devolução, a qual registro ela pertence

    CONSTRAINT fk_hist_mov_equipamento_id FOREIGN KEY (equipamento_id) REFERENCES equipamentos(id),
    CONSTRAINT fk_hist_mov_usuario_id FOREIGN KEY (usuario_responsavel_id) REFERENCES usuarios(id),
    CONSTRAINT fk_hist_mov_solicitacao_id FOREIGN KEY (solicitacao_id) REFERENCES solicitacoes(id),
    CONSTRAINT fk_hist_mov_devolucao_id FOREIGN KEY (devolucao_id) REFERENCES devolucoes(id)
);