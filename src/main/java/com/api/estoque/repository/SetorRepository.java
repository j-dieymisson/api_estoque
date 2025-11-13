package com.api.estoque.repository;

import com.api.estoque.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SetorRepository extends JpaRepository<Setor, Long> {

    // Busca todos os setores ordenados por nome
    List<Setor> findAllByOrderByNomeAsc();

    // Busca apenas os setores ativos (útil para dropdowns no futuro)
    List<Setor> findAllByAtivoTrueOrderByNomeAsc();
}