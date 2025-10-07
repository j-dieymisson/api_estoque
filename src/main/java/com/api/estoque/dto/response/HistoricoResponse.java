package com.api.estoque.dto.response;

import java.time.LocalDateTime;

public record HistoricoResponse(
        Long id,
        LocalDateTime dataMovimentacao,
        String tipoMovimentacao, // ENTRADA, SAÍDA, etc.
        int quantidade,
        String nomeEquipamento,
        String usuarioResponsavel,
        Long solicitacaoId,
        Long devolucaoId
) {}