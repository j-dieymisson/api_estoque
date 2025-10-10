package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @NotBlank
        @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres.")
        String novaSenha
) {}