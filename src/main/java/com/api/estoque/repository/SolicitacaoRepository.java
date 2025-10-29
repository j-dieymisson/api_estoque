package com.api.estoque.repository;

import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.StatusSolicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    // Exemplo de como usar a anotação @Query para escrever uma consulta customizada em JPQL
    // Isso será útil para o histórico de movimentações do usuário.
    @Query("SELECT s FROM Solicitacao s WHERE s.usuario.id = :idUsuario ORDER BY s.dataSolicitacao DESC")
    Page<Solicitacao> findByUsuarioId(Long idUsuario, Pageable pageable);

    List<Solicitacao> findAllByUsuarioIdAndStatus(Long usuarioId, StatusSolicitacao status);

    Page<Solicitacao> findAllByStatus(StatusSolicitacao status, Pageable pageable);

    // Busca todas as solicitações de um utilizador, com paginação
    Page<Solicitacao> findAllByUsuarioId(Long usuarioId, Pageable pageable);

    // Busca todas as solicitações de um utilizador COM um status específico, com paginação
    Page<Solicitacao> findAllByUsuarioIdAndStatus(Long usuarioId, StatusSolicitacao status, Pageable pageable);

    // 1. Busca Pura por Data
    Page<Solicitacao> findAllByDataSolicitacaoBetween(LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    // 2. Busca por Utilizador E Data
    Page<Solicitacao> findAllByUsuarioIdAndDataSolicitacaoBetween(Long usuarioId, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    // 3. Busca por Status E Data
    Page<Solicitacao> findAllByStatusAndDataSolicitacaoBetween(StatusSolicitacao status, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    // 4. Busca por Utilizador, Status E Data (a mais completa)
    Page<Solicitacao> findAllByUsuarioIdAndStatusAndDataSolicitacaoBetween(Long usuarioId, StatusSolicitacao status, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    long countByStatus(StatusSolicitacao status);

    long countByStatusAndDataSolicitacaoAfter(StatusSolicitacao status, LocalDateTime data);

    // Conta todas as solicitações cujo status NÃO SEJA o fornecido
    long countByStatusNot(StatusSolicitacao status);

    // Conta todas as solicitações com um status específico dentro de um intervalo de datas
    long countByStatusAndDataSolicitacaoBetween(StatusSolicitacao status, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT s FROM Solicitacao s WHERE " +
            "((s.status != com.api.estoque.model.StatusSolicitacao.RASCUNHO) OR (s.usuario.id = :usuarioLogadoId)) AND " +
            "(:usuarioId IS NULL OR s.usuario.id = :usuarioId) AND " + // <-- FILTRO ADICIONADO
            "(:status IS NULL OR s.status = :status) AND " +
            "(:inicio IS NULL OR s.dataSolicitacao >= :inicio) AND " +
            "(:fim IS NULL OR s.dataSolicitacao <= :fim)")
    Page<Solicitacao> findAdminView(
            @Param("usuarioLogadoId") Long usuarioLogadoId,
            @Param("usuarioId") Long usuarioId, // <-- PARÂMETRO ADICIONADO
            @Param("status") StatusSolicitacao status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable
    );

    // Substitua o seu método findMyView por este:
    @Query("SELECT s FROM Solicitacao s WHERE " +
            "s.usuario.id = :usuarioId AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:inicio IS NULL OR s.dataSolicitacao >= :inicio) AND " +
            "(:fim IS NULL OR s.dataSolicitacao <= :fim)")
    Page<Solicitacao> findMyView(
            @Param("usuarioId") Long usuarioId,
            @Param("status") StatusSolicitacao status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable
    );

    // Busca as 5 solicitações mais recentes que estão com status PENDENTE
    List<Solicitacao> findTop5ByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao status);

}