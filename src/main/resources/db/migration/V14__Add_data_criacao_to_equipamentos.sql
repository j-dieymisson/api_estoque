-- Adiciona a coluna data_criacao para registrar quando o equipamento foi cadastrado
ALTER TABLE equipamentos
ADD COLUMN data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;