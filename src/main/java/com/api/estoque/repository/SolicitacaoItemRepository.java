package com.api.estoque.repository;

import com.api.estoque.model.SolicitacaoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoItemRepository extends JpaRepository<SolicitacaoItem, Long> {}
