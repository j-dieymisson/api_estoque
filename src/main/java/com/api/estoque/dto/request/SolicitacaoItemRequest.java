package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SolicitacaoItemRequest(
        @NotNull Long equipamentoId,
        @NotNull @Positive int quantidade
) {}