package com.api.estoque.repository;

import com.api.estoque.model.Devolucao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {}