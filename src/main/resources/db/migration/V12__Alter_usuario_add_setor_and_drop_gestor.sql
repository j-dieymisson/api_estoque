-- 1. Remover a coluna antiga (gestor_imediato_id)
-- (Primeiro removemos a constraint/foreign key que o V10 criou)
ALTER TABLE usuarios DROP FOREIGN KEY fk_usuarios_gestor;
ALTER TABLE usuarios DROP COLUMN gestor_imediato_id;

-- 2. Adicionar a nova coluna (setor_id)
ALTER TABLE usuarios ADD COLUMN setor_id BIGINT NULL;

-- 3. Adicionar a nova constraint (ligação com a tabela 'setores' do V11)
ALTER TABLE usuarios
ADD CONSTRAINT fk_usuarios_setor
    FOREIGN KEY (setor_id)
    REFERENCES setores(id);