package com.api.estoque.service;

import com.api.estoque.dto.response.HistoricoResponse;
import com.api.estoque.dto.response.HistoricoStatusSolicitacaoResponse;
import com.api.estoque.model.HistoricoMovimentacao;
import com.api.estoque.model.HistoricoStatusSolicitacao;
import com.api.estoque.repository.HistoricoMovimentacaoRepository;
import com.api.estoque.repository.HistoricoStatusSolicitacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HistoricoService {

    private final HistoricoMovimentacaoRepository historicoRepository;
    private final HistoricoStatusSolicitacaoRepository historicoStatusRepository;

    public HistoricoService(HistoricoMovimentacaoRepository historicoRepository,
                            HistoricoStatusSolicitacaoRepository historicoStatusRepository) {
        this.historicoRepository = historicoRepository;
        this.historicoStatusRepository = historicoStatusRepository;
    }

    @Transactional(readOnly = true)
    public Page<HistoricoResponse> buscarPorEquipamentoId(
            Long equipamentoId,
            Optional<LocalDate> dataInicio,
            Optional<LocalDate> dataFim,
            Pageable pageable) {

        Page<HistoricoMovimentacao> historicoPage;

        // Verifica se ambas as datas foram fornecidas
        if (dataInicio.isPresent() && dataFim.isPresent()) {
            // Converte LocalDate para LocalDateTime para abranger o dia inteiro
            LocalDateTime inicio = dataInicio.get().atStartOfDay(); // Ex: 2025-10-09 -> 2025-10-09T00:00:00
            LocalDateTime fim = dataFim.get().atTime(23, 59, 59);    // Ex: 2025-10-10 -> 2025-10-10T23:59:59

            historicoPage = historicoRepository.findByEquipamentoIdAndDataMovimentacaoBetween(equipamentoId, inicio, fim, pageable);
        } else {
            // Se as datas não forem fornecidas, usa o método antigo
            historicoPage = historicoRepository.findAllByEquipamentoIdOrderByDataMovimentacaoDesc(equipamentoId, pageable);
        }

        return historicoPage.map(this::mapToHistoricoResponse);
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusSolicitacaoResponse> buscarPorSolicitacaoId(Long solicitacaoId) {
        List<HistoricoStatusSolicitacao> historicoList = historicoStatusRepository.findBySolicitacaoIdOrderByDataAlteracaoAsc(solicitacaoId);
        return historicoList.stream()
                .map(this::mapToHistoricoStatusResponse)
                .collect(Collectors.toList());
    }

    private HistoricoResponse mapToHistoricoResponse(HistoricoMovimentacao historico) {
        return new HistoricoResponse(
                historico.getId(),
                historico.getDataMovimentacao(),
                historico.getTipoMovimentacao().name(),
                historico.getQuantidade(),
                historico.getQuantidadeAnterior(), // Novo
                historico.getQuantidadePosterior(), // Novo
                historico.getEquipamento().getNome(),
                historico.getUsuarioResponsavel().getNome(),
                historico.getSolicitacao() != null ? historico.getSolicitacao().getId() : null
        );
    }

    private HistoricoStatusSolicitacaoResponse mapToHistoricoStatusResponse(HistoricoStatusSolicitacao historico) {
        return new HistoricoStatusSolicitacaoResponse(
                historico.getId(),
                historico.getDataAlteracao(),
                historico.getStatusAnterior() != null ? historico.getStatusAnterior().name() : "N/A",
                historico.getStatusNovo().name(),
                historico.getUsuarioResponsavel().getNome()
        );
    }
}