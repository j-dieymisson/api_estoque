package com.api.estoque.dto.response;

import java.time.LocalDateTime;

public record DevolucaoResponse(
        Long id, // Id da devolução
        Long solicitacaoId, // Id da solicitação original
        String nomeEquipamento,
        int quantidadeDevolvida,
        LocalDateTime dataDevolucao,
        String observacao
) {}