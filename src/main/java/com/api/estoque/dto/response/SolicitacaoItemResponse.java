package com.api.estoque.dto.response;

public record SolicitacaoItemResponse(
        Long id,
        String nomeEquipamento,
        int quantidadeSolicitada,
        int quantidadeDevolvida,
        int quantidadePendente
) {}