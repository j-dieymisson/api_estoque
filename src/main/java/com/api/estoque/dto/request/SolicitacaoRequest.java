package com.api.estoque.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SolicitacaoRequest(
        // O ID do usuário virá do token de autenticação no futuro,
        // mas para testes iniciais, podemos adicioná-lo aqui.
        @NotNull Long usuarioId,
        String justificativa,
        @NotEmpty(message = "A lista de itens não pode estar vazia.")
        List<@Valid SolicitacaoItemRequest> itens
) {}