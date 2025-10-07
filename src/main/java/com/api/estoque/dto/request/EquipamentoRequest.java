package com.api.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EquipamentoRequest(
        @NotBlank String nome,
        String descricao,
        @NotNull @Positive int quantidadeTotal,
        @NotNull Long categoriaId // Recebemos apenas o ID da categoria
) {}