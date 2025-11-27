-- Insere o administrador padrão apenas se não houver conflito de ID ou Login

INSERT INTO usuarios (id, nome, login, senha, ativo, cargo_id, email, setor_id, funcao)
VALUES (
    1,
    'Sistema',
    'sistema',
    '$2a$12$5cKvSak0CZzUbXGagK0FtexNEjfYaQdghK7ubaD27asT1VZ9gSd2S',
    true,
    1,
    'jdbservico@gmail.com',
    NULL,
    'Sistema'
);