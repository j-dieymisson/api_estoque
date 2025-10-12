package com.api.estoque.service;

import com.api.estoque.dto.response.HistoricoResponse;
import com.api.estoque.dto.response.HistoricoStatusSolicitacaoResponse;
import com.api.estoque.model.HistoricoMovimentacao;
import com.api.estoque.model.HistoricoStatusSolicitacao;
import com.api.estoque.model.TipoMovimentacao;
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
            Optional<TipoMovimentacao> tipo,
            Pageable pageable) {

        Page<HistoricoMovimentacao> historicoPage;

        LocalDateTime inicio = dataInicio.map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime fim = dataFim.map(d -> d.atTime(23, 59, 59)).orElse(null);
        boolean datasPresentes = inicio != null && fim != null;

        // Lógica de decisão ainda mais completa
        if (tipo.isPresent() && datasPresentes) {
            // Filtro por: TIPO e DATA
            historicoPage = historicoRepository.findByEquipamentoIdAndTipoMovimentacaoAndDataMovimentacaoBetween(equipamentoId, tipo.get(), inicio, fim, pageable);
        } else if (tipo.isPresent()) {
            // Filtro por: Apenas TIPO
            historicoPage = historicoRepository.findByEquipamentoIdAndTipoMovimentacao(equipamentoId, tipo.get(), pageable);
        } else if (datasPresentes) {
            // Filtro por: Apenas DATA (já existia)
            historicoPage = historicoRepository.findByEquipamentoIdAndDataMovimentacaoBetween(equipamentoId, inicio, fim, pageable);
        } else {
            // Sem filtros
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

    @Transactional(readOnly = true)
    public Page<HistoricoResponse> listarTodasMovimentacoes(
            Optional<TipoMovimentacao> tipo,
            Optional<Long> equipamentoId,
            Optional<Long> usuarioId,
            Optional<LocalDate> dataInicio,
            Optional<LocalDate> dataFim,
            Pageable pageable) {

        // Prepara as datas para a query, abrangendo o dia inteiro
        LocalDateTime inicio = dataInicio.map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime fim = dataFim.map(d -> d.atTime(23, 59, 59)).orElse(null);

        // Chama o nosso novo método de busca dinâmica do repositório
        Page<HistoricoMovimentacao> movimentacoes = historicoRepository.findByFilters(
                tipo.orElse(null),
                equipamentoId.orElse(null),
                usuarioId.orElse(null),
                inicio,
                fim,
                pageable
        );

        // Mapeia o resultado para o DTO de resposta
        return movimentacoes.map(this::mapToHistoricoResponse);
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