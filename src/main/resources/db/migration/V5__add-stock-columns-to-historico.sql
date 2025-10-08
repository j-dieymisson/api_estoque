ALTER TABLE historico_movimentacoes
ADD COLUMN quantidade_anterior INT NOT NULL,
ADD COLUMN quantidade_posterior INT NOT NULL;