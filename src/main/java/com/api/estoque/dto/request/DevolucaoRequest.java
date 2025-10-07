package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DevolucaoRequest(
        @NotNull(message = "O ID do item da solicitação é obrigatório.")
        Long solicitacaoItemId,

        @NotNull(message = "A quantidade devolvida é obrigatória.")
        @Positive(message = "A quantidade devolvida deve ser um número positivo.")
        int quantidadeDevolvida,

        String observacao
) {}