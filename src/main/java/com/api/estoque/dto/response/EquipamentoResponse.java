package com.api.estoque.dto.response;

import java.time.LocalDateTime;

public record EquipamentoResponse(
        Long id,
        String nome,
        String descricao,
        int quantidadeTotal,
        int quantidadeDisponivel,
        boolean ativo,
        Long categoriaId,
        String nomeCategoria,
        LocalDateTime dataCriacao
) {}