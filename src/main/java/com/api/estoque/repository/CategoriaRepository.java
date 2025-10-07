package com.api.estoque.repository;

import com.api.estoque.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

// A interface estende JpaRepository<TipoDaEntidade, TipoDoIdDaEntidade>
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // Pronto! Nenhum código a mais é necessário para as operações básicas de CRUD.
}