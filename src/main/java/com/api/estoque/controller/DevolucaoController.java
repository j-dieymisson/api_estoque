package com.api.estoque.controller;

import com.api.estoque.dto.request.DevolucaoRequest;
import com.api.estoque.dto.response.DevolucaoResponse;
import com.api.estoque.service.DevolucaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/devolucoes")
public class DevolucaoController {

    private final DevolucaoService devolucaoService;

    public DevolucaoController(DevolucaoService devolucaoService) {
        this.devolucaoService = devolucaoService;
    }

    @PostMapping
    public ResponseEntity<DevolucaoResponse> registrar(
            @RequestBody @Valid DevolucaoRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        // Chama o serviço que contém toda a lógica de negócio
        DevolucaoResponse response = devolucaoService.registrarDevolucao(request);

        // Cria a URI para o novo recurso 'devolucao' criado
        URI uri = uriBuilder.path("/devolucoes/{id}").buildAndExpand(response.id()).toUri();

        // Retorna o status 201 Created com a localização e o corpo da resposta
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<DevolucaoResponse>> listar(
            @PageableDefault(size = 10, sort = {"dataDevolucao"}) Pageable paginacao
    ) {
        Page<DevolucaoResponse> paginaDeDevolucoes = devolucaoService.listarTodas(paginacao);
        return ResponseEntity.ok(paginaDeDevolucoes);
    }

    // ENDPOINT PARA DETALHAR UMA DEVOLUÇÃO
    @GetMapping("/{id}")
    public ResponseEntity<DevolucaoResponse> detalhar(@PathVariable Long id) {
        DevolucaoResponse response = devolucaoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }
}