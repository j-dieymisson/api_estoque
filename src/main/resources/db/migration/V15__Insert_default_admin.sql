-- Insere o administrador padrão apenas se não houver conflito de ID ou Login
-- (Assumindo que o cargo_id 1 é ADMIN, conforme suas migrações anteriores)

INSERT INTO usuarios (nome, login, senha, ativo, cargo_id, email)
VALUES ('admin', 'admin', '$2a$12$3sLaIbrXeeyFQDy1UdW6D.e4my1td7yKadPe23hxnySfQB73a3Jje', true, 1, 'jose12234@email.com');