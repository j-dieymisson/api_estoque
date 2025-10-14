package com.api.estoque.repository;

import com.api.estoque.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByAtiva(boolean ativa);

    boolean existsByNomeIgnoreCase(String nome);

    // Verifica se uma categoria com o nome existe, mas excluindo um ID específico (útil para a atualização)
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}