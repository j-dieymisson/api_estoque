package com.api.estoque.repository;

import com.api.estoque.model.HistoricoStatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusSolicitacaoRepository extends JpaRepository<HistoricoStatusSolicitacao, Long> {
    List<HistoricoStatusSolicitacao> findBySolicitacaoIdOrderByDataAlteracaoAsc(Long solicitacaoId);
}