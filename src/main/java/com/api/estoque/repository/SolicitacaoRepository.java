package com.api.estoque.repository;

import com.api.estoque.model.Setor;
import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.StatusSolicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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

    // Conta por múltiplos status (ex: PENDENTE_GESTOR e PENDENTE_ADMIN)
    long countByStatusIn(Collection<StatusSolicitacao> statuses);

    // Conta por múltiplos status DEPOIS de uma data
    long countByStatusInAndDataSolicitacaoAfter(Collection<StatusSolicitacao> statuses, LocalDateTime data);

    // Conta todos que NÃO ESTÃO na lista de status (ex: tudo exceto Rascunho)
    long countByStatusNotIn(Collection<StatusSolicitacao> statuses);

    // Busca o Top 5 por múltiplos status
    List<Solicitacao> findTop5ByStatusInOrderByDataSolicitacaoDesc(Collection<StatusSolicitacao> statuses);

    @Query("SELECT COUNT(s) FROM Solicitacao s WHERE s.status = :status AND s.usuario.setor = :setor")
    long countByStatusAndUsuarioSetor(@Param("status") StatusSolicitacao status, @Param("setor") Setor setor);



    // Conta todas as solicitações com um status específico dentro de um intervalo de datas
    long countByStatusAndDataSolicitacaoBetween(StatusSolicitacao status, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT s FROM Solicitacao s WHERE " +
            "((s.status != com.api.estoque.model.StatusSolicitacao.RASCUNHO) OR (s.usuario.id = :usuarioLogadoId)) AND " +
            "(:usuarioId IS NULL OR s.usuario.id = :usuarioId) AND " +
            "(:statuses IS NULL OR s.status IN :statuses) AND " +
            "(:inicio IS NULL OR s.dataSolicitacao >= :inicio) AND " +
            "(:fim IS NULL OR s.dataSolicitacao <= :fim) AND " +
            "(:devolucaoIndeterminada IS NULL OR (:devolucaoIndeterminada = true AND s.dataPrevisaoDevolucao IS NULL))")
    Page<Solicitacao> findAdminView(
            @Param("usuarioLogadoId") Long usuarioLogadoId,
            @Param("usuarioId") Long usuarioId, // <-- PARÂMETRO ADICIONADO
            @Param("statuses") List<StatusSolicitacao> statuses,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("devolucaoIndeterminada") Boolean devolucaoIndeterminada,
            Pageable pageable
    );

    @Query("SELECT s FROM Solicitacao s WHERE " +
            "s.usuario.id = :usuarioId AND " +
            "(:statuses IS NULL OR s.status IN :statuses) AND " +
            "(:inicio IS NULL OR s.dataSolicitacao >= :inicio) AND " +
            "(:fim IS NULL OR s.dataSolicitacao <= :fim) AND " +
            "(:devolucaoIndeterminada IS NULL OR (:devolucaoIndeterminada = true AND s.dataPrevisaoDevolucao IS NULL))")
    Page<Solicitacao> findMyView(
            @Param("usuarioId") Long usuarioId,
            @Param("statuses") List<StatusSolicitacao> statuses,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("devolucaoIndeterminada") Boolean devolucaoIndeterminada,
            Pageable pageable
    );

    // Busca por ID de usuário E uma LISTA de status
    List<Solicitacao> findAllByUsuarioIdAndStatusIn(Long usuarioId, Collection<StatusSolicitacao> statuses);

}