package com.api.estoque.repository;

import com.api.estoque.model.HistoricoMovimentacao;
import com.api.estoque.model.TipoMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoricoMovimentacaoRepository extends JpaRepository<HistoricoMovimentacao, Long> {

    // MÉTODO CORRIGIDO: Deve aceitar Pageable e retornar Page
    Page<HistoricoMovimentacao> findAllByEquipamentoIdOrderByDataMovimentacaoDesc(Long equipamentoId, Pageable pageable);

    // MÉTODO PARA O FILTRO DE DATA (que já tínhamos adicionado)
    Page<HistoricoMovimentacao> findByEquipamentoIdAndDataMovimentacaoBetween(
            Long equipamentoId,
            LocalDateTime inicio,
            LocalDateTime fim,
            Pageable pageable
    );

    // Este método pode ser apagado se você não o estiver a usar noutro sítio,
    // pois a versão paginada acima é mais flexível.
    List<HistoricoMovimentacao> findAllByEquipamentoIdOrderByDataMovimentacaoDesc(Long equipamentoId);

    // Busca por Equipamento e Tipo de Movimentação
    Page<HistoricoMovimentacao> findByEquipamentoIdAndTipoMovimentacao(Long equipamentoId, TipoMovimentacao tipo, Pageable pageable);

    // Busca por Equipamento, Tipo e Data (para combinar todos os filtros)
    Page<HistoricoMovimentacao> findByEquipamentoIdAndTipoMovimentacaoAndDataMovimentacaoBetween(
            Long equipamentoId,
            TipoMovimentacao tipo,
            LocalDateTime inicio,
            LocalDateTime fim,
            Pageable pageable
    );

}