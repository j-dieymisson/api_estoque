package com.api.estoque.repository;

import com.api.estoque.model.HistoricoMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoMovimentacaoRepository extends JpaRepository<HistoricoMovimentacao, Long> {
    Page<HistoricoMovimentacao> findByEquipamentoIdOrderByDataMovimentacaoDesc(Long equipamentoId, Pageable pageable);
}