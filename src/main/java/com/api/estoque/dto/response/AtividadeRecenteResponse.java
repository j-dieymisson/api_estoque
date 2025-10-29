package com.api.estoque.dto.response;

import java.time.LocalDateTime;

/**
 * Representa um único item no feed de atividades do dashboard.
 */
public record AtividadeRecenteResponse(
        Long idReferencia, // O ID da Solicitação
        String tipo, // Ex: "SOLICITACAO_PENDENTE", "SAIDA_EQUIPAMENTO"
        String descricao, // A frase que será mostrada (ex: "José Silva criou...")
        LocalDateTime data // A data/hora para podermos ordenar o feed
) {}