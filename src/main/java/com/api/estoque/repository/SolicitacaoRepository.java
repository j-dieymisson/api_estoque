package com.api.estoque.repository;

import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.StatusSolicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

}