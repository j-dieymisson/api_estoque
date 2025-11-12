package com.api.estoque.repository;

import com.api.estoque.model.HistoricoStatusSolicitacao;
import com.api.estoque.model.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoricoStatusSolicitacaoRepository extends JpaRepository<HistoricoStatusSolicitacao, Long> {
    List<HistoricoStatusSolicitacao> findBySolicitacaoIdOrderByDataAlteracaoAsc(Long solicitacaoId);
    long countByStatusNovoAndDataAlteracaoAfter(StatusSolicitacao status, LocalDateTime data);
}