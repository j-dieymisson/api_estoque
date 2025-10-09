package com.api.estoque.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SolicitacaoResponse(
        Long id,
        String nomeUsuario,
        LocalDateTime dataSolicitacao,
        LocalDate dataPrevisaoEntrega,
        LocalDate dataPrevisaoDevolucao,
        String status,
        String justificativa,
        List<SolicitacaoItemResponse> itens
) {}