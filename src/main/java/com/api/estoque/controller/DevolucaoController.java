package com.api.estoque.controller;

import com.api.estoque.dto.request.DevolucaoRequest;
import com.api.estoque.dto.response.DevolucaoResponse;
import com.api.estoque.service.DevolucaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}