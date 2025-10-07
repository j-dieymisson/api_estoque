package com.api.estoque.dto.response;

public record CategoriaResponse(
        Long id,
        String nome,
        boolean ativa
) {}