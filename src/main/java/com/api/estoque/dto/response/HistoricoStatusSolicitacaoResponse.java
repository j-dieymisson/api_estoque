package com.api.estoque.dto.response;

import java.time.LocalDateTime;

public record HistoricoStatusSolicitacaoResponse(
        Long id,
        LocalDateTime dataAlteracao,
        String statusAnterior,
        String statusNovo,
        String usuarioResponsavel
) {}