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

import java.util.List;
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

    @Transactional(readOnly = true) // Boa prática para métodos de apenas leitura
    public Page<HistoricoResponse> buscarPorEquipamentoId(Long equipamentoId, Pageable pageable) {
        // Futuramente, poderíamos criar uma query customizada no repositório para otimizar isso,
        // mas por enquanto, vamos buscar a entidade e mapear.
        Page<HistoricoMovimentacao> historicoPage = historicoRepository.findByEquipamentoIdOrderByDataMovimentacaoDesc(equipamentoId, pageable);
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