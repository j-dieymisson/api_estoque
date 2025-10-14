package com.api.estoque.dto.response;

public record SolicitacaoItemResponse(
        Long id,
        Long equipamentoId,
        String nomeEquipamento,
        int quantidadeSolicitada,
        int quantidadeDevolvida,
        int quantidadePendente
) {}