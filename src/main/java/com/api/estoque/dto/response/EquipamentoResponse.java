package com.api.estoque.dto.response;

public record EquipamentoResponse(
        Long id,
        String nome,
        String descricao,
        int quantidadeTotal,
        int quantidadeDisponivel,
        boolean ativo,
        Long categoriaId,
        String nomeCategoria// Enviamos o nome da categoria para facilitar a exibição no front-end
) {}