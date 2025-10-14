package com.api.estoque.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record SolicitacaoRequest(

        String justificativa,
        @FutureOrPresent(message = "A data de previsão de entrega deve ser hoje ou no futuro.")
        LocalDate dataPrevisaoEntrega,

        @FutureOrPresent(message = "A data de previsão de devolução deve ser hoje ou no futuro.")
        LocalDate dataPrevisaoDevolucao,

        @NotEmpty(message = "A lista de itens não pode estar vazia.")
        List<@Valid SolicitacaoItemRequest> itens
) {}