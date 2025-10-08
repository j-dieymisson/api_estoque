package com.api.estoque.repository;

import com.api.estoque.model.Equipamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
