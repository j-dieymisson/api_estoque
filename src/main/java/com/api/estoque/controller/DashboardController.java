package com.api.estoque.controller;

import com.api.estoque.model.DashboardWidget;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retorna os dados do dashboard com base nas preferências do admin autenticado.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        Map<String, Object> dados = dashboardService.getDashboardData(usuarioLogado);
        return ResponseEntity.ok(dados);
    }

    /**
     * Retorna a lista de todos os widgets de dashboard disponíveis para escolha.
     */
    @GetMapping("/widgets-disponiveis")
    public ResponseEntity<List<String>> listarWidgetsDisponiveis() {
        List<String> widgets = dashboardService.listarWidgetsDisponiveis();
        return ResponseEntity.ok(widgets);
    }

    /**
     * Atualiza a lista de widgets preferidos para o admin autenticado.
     */
    @PutMapping("/preferencias")
    public ResponseEntity<Void> atualizarPreferencias(
            @RequestBody List<DashboardWidget> widgets,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        dashboardService.atualizarPreferencias(usuarioLogado, widgets);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferencias")
    public ResponseEntity<List<String>> listarPreferencias(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        List<String> preferencias = usuarioLogado.getPreferenciasDashboard().stream()
                .map(pref -> pref.getWidgetNome().name())
                .collect(Collectors.toList());

        return ResponseEntity.ok(preferencias);
    }
}