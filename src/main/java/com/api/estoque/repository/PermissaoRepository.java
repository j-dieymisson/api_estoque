package com.api.estoque.repository;

import com.api.estoque.model.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoRepository extends JpaRepository<Permissao, Long> {
}