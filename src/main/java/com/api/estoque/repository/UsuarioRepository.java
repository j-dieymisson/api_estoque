package com.api.estoque.repository;

import com.api.estoque.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<UserDetails> findByNome(String nome);

    long countByAtivoTrue();

    // Busca paginada de utilizadores cujo nome contém a string de pesquisa (ignorando maiúsculas/minúsculas)
    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // Busca todos os utilizadores cujo ID não é o que foi passado
    Page<Usuario> findByIdNot(Long id, Pageable pageable);

    // Busca por nome, mas também excluindo o super admin
    Page<Usuario> findByNomeContainingIgnoreCaseAndIdNot(String nome, Long id, Pageable pageable);
}