package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record CargoRequest(
        @NotBlank(message = "O nome do cargo é obrigatório.")
        String nome,

        // Uma lista de IDs das permissões que este cargo terá
        Set<Long> permissoesIds
) {}