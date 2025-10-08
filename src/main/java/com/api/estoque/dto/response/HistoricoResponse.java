package com.api.estoque.dto.response;

import java.time.LocalDateTime;

public record HistoricoResponse(
        Long id,
        LocalDateTime dataMovimentacao,
        String tipoMovimentacao,
        int quantidade, // A diferença (ex: -2 ou +1)
        int quantidadeAnterior, // Novo
        int quantidadePosterior, // Novo
        String nomeEquipamento,
        String usuarioResponsavel,
        Long solicitacaoId
) {}