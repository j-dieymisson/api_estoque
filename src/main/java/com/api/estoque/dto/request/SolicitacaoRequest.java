package com.api.estoque.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record SolicitacaoRequest(
        // O ID do usuário virá do token de autenticação no futuro,
        // mas para testes iniciais, podemos adicioná-lo aqui.
        @NotNull Long usuarioId,
        String justificativa,
        @Future(message = "A data de previsão de entrega deve ser no futuro.")
        LocalDate dataPrevisaoEntrega,

        @Future(message = "A data de previsão de devolução deve ser no futuro.")
        LocalDate dataPrevisaoDevolucao,

        @NotEmpty(message = "A lista de itens não pode estar vazia.")
        List<@Valid SolicitacaoItemRequest> itens
) {}