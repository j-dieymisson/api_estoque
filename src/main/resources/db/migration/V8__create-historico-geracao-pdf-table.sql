
CREATE TABLE historico_geracao_pdf (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_geracao DATETIME NOT NULL,

    -- Chave estrangeira para saber de qual solicitação o PDF foi gerado
    solicitacao_id BIGINT NOT NULL,

    -- Chave estrangeira para saber qual utilizador gerou o PDF
    usuario_id BIGINT NOT NULL,

    CONSTRAINT fk_hist_pdf_solicitacao_id FOREIGN KEY (solicitacao_id) REFERENCES solicitacoes(id),
    CONSTRAINT fk_hist_pdf_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);