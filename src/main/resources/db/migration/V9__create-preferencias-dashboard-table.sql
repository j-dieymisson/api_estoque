CREATE TABLE preferencias_dashboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- A qual utilizador esta preferência pertence
    usuario_id BIGINT NOT NULL,

    -- Qual o widget que ele escolheu ver (o nome deve ser igual ao do Enum)
    widget_nome VARCHAR(100) NOT NULL,

    CONSTRAINT fk_pref_dashboard_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    -- Garante que um utilizador não pode ter o mesmo widget duas vezes
    UNIQUE (usuario_id, widget_nome)
);