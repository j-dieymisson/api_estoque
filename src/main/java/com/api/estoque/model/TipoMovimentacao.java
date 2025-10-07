package com.api.estoque.model;

public enum TipoMovimentacao {
    SAIDA,      // Representa a saída de estoque por uma solicitação aprovada
    DEVOLUCAO,  // Representa a entrada de estoque por uma devolução
    AJUSTE_MANUAL // Podemos prever outros tipos para o futuro
}