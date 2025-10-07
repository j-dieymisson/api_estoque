package com.api.estoque.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SolicitacaoResponse(
        Long id,
        String nomeUsuario,
        LocalDateTime dataSolicitacao,
        String status,
        String justificativa,
        List<SolicitacaoItemResponse> itens
) {}