package com.api.estoque.repository;

import com.api.estoque.model.Equipamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    // Exemplo de query customizada para buscar equipamentos por nome da categoria
    @Query("SELECT e FROM Equipamento e WHERE e.categoria.nome = :nomeCategoria")
    Page<Equipamento> findByCategoriaNome(String nomeCategoria, Pageable pageable);

    // Spring Data JPA é inteligente! Só por escrever o método com esse nome,
    // ele automaticamente cria uma query para buscar equipamentos ativos com paginação.
    Page<Equipamento> findAllByAtivoTrue(Pageable pageable);

    // Conta quantos equipamentos ativos existem numa determinada categoria
    long countByCategoriaIdAndAtivoTrue(Long categoriaId);

    Page<Equipamento> findAllByAtivoTrueAndQuantidadeDisponivelGreaterThan(int quantidade, Pageable pageable);

    Page<Equipamento> findAllByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);

    // Busca equipamentos ativos por nome e por categoria
    Page<Equipamento> findAllByAtivoTrueAndCategoriaIdAndNomeContainingIgnoreCase(Long categoriaId, String nome, Pageable pageable);

    // Busca equipamentos ativos apenas por categoria (quando o nome não é fornecido)
    Page<Equipamento> findAllByAtivoTrueAndCategoriaId(Long categoriaId, Pageable pageable);

    // Adicione este novo método
    Page<Equipamento> findByIdAndAtivoTrue(Long id, Pageable pageable);

    // Query customizada para somar a diferença entre total e disponível
    @Query("SELECT SUM(e.quantidadeTotal - e.quantidadeDisponivel) FROM Equipamento e WHERE e.ativo = true")
    Long sumEquipamentosEmUso();

    // Conta todos os equipamentos cujo campo 'ativo' é true
    long countByAtivoTrue();

    // Query customizada para somar a quantidade total de todas as unidades de equipamento
    @Query("SELECT SUM(e.quantidadeTotal) FROM Equipamento e")
    Long sumQuantidadeTotal();

    @Query("SELECT e FROM Equipamento e WHERE " +
            "e.ativo = true AND " +
            "(:id IS NULL OR e.id = :id) AND " +
            "(:nome IS NULL OR LOWER(e.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:categoriaId IS NULL OR e.categoria.id = :categoriaId) AND " +
            "(:inicioCriacao IS NULL OR e.dataCriacao >= :inicioCriacao) AND " +
            "(:fimCriacao IS NULL OR e.dataCriacao <= :fimCriacao)")
    Page<Equipamento> findWithFilters(
            @Param("id") Long id,
            @Param("nome") String nome,
            @Param("categoriaId") Long categoriaId,
            @Param("inicioCriacao") LocalDateTime inicioCriacao,
            @Param("fimCriacao") LocalDateTime fimCriacao,
            Pageable pageable
    );
}
