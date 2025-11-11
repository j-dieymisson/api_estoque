package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record CargoRequest(
        @NotBlank(message = "O nome do cargo é obrigatório.")
        String nome
) {}