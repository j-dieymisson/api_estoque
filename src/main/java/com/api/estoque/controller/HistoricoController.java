package com.api.estoque.controller;

import com.api.estoque.dto.response.HistoricoResponse;
import com.api.estoque.service.HistoricoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/equipamento/{id}")
    public ResponseEntity<Page<HistoricoResponse>> listarPorEquipamento(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = {"dataMovimentacao"}) Pageable paginacao
    ) {
        Page<HistoricoResponse> historicoPage = historicoService.buscarPorEquipamentoId(id, paginacao);
        return ResponseEntity.ok(historicoPage);
    }

}