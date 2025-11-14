package com.api.estoque.repository;

import com.api.estoque.model.Cargo;
import com.api.estoque.model.Setor;
import com.api.estoque.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo LEFT JOIN FETCH u.setor WHERE u.nome = :nome")
    Optional<UserDetails> findByNome(@Param("nome") String nome);

    long countByAtivoTrue();

    // Busca paginada de utilizadores cujo nome contém a string de pesquisa (ignorando maiúsculas/minúsculas)
    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // Busca todos os utilizadores cujo ID não é o que foi passado
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo LEFT JOIN FETCH u.setor WHERE u.id <> :id")
    Page<Usuario> findByIdNot(@Param("id") Long id, Pageable pageable);

    // Busca por nome, mas também excluindo o super admin
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo LEFT JOIN FETCH u.setor WHERE LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%')) AND u.id <> :id")
    Page<Usuario> findByNomeContainingIgnoreCaseAndIdNot(@Param("nome") String nome, @Param("id") Long id, Pageable pageable);

    List<Usuario> findAllByCargo(Cargo cargo);

    boolean existsBySetorAndCargoNome(Setor setor, String nomeCargo);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo LEFT JOIN FETCH u.setor WHERE u.id = :id")
    Optional<Usuario> findByIdWithCargoAndSetor(@Param("id") Long id);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo LEFT JOIN FETCH u.setor WHERE u.id <> :id AND (u.setor = :setor OR u.setor IS NULL) AND u.cargo.nome <> 'ADMIN'")
    Page<Usuario> findByIdNotAndSetor(@Param("id") Long id, @Param("setor") Setor setor, Pageable pageable);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo LEFT JOIN FETCH u.setor WHERE LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%')) AND u.id <> :id AND (u.setor = :setor OR u.setor IS NULL) AND u.cargo.nome <> 'ADMIN'")
    Page<Usuario> findByNomeContainingIgnoreCaseAndIdNotAndSetor(@Param("nome") String nome, @Param("id") Long id, @Param("setor") Setor setor, Pageable pageable);
}