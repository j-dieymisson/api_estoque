package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RecusaRequest(
        @NotBlank(message = "O motivo da recusa é obrigatório.")
        String motivo
) {}