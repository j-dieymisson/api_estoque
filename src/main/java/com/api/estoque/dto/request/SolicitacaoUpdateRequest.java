package com.api.estoque.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SolicitacaoUpdateRequest(
        String justificativa,

        @NotEmpty(message = "A lista de itens não pode estar vazia.")
        List<@Valid SolicitacaoItemRequest> itens
) {}