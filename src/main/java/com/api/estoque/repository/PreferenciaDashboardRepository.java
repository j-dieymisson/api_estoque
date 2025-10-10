package com.api.estoque.repository;

import com.api.estoque.model.PreferenciaDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreferenciaDashboardRepository extends JpaRepository<PreferenciaDashboard, Long> {
    // Método para apagar todas as preferências de um utilizador de uma só vez
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PreferenciaDashboard p WHERE p.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}