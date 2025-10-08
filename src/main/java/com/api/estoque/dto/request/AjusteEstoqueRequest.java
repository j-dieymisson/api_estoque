package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AjusteEstoqueRequest(
        @NotNull(message = "A nova quantidade total é obrigatória.")
        @PositiveOrZero(message = "A quantidade deve ser zero ou maior.")
        int novaQuantidade,

        @NotBlank(message = "A justificação é obrigatória.")
        String justificativa
) {}