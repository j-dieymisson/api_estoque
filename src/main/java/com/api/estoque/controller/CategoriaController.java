package com.api.estoque.controller;

import com.api.estoque.dto.request.CategoriaRequest;
import com.api.estoque.dto.response.CategoriaResponse;
import com.api.estoque.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    // Injeção de dependência do service
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> cadastrar(
            @RequestBody @Valid CategoriaRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        // Chama o serviço para criar a categoria
        CategoriaResponse response = categoriaService.criarCategoria(request);

        // Constrói a URI para o novo recurso
        URI uri = uriBuilder.path("/categorias/{id}").buildAndExpand(response.id()).toUri();

        // Retorna 201 Created com a URI e o objeto criado
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar() {
        // Chama o serviço para obter a lista de todas as categorias
        List<CategoriaResponse> lista = categoriaService.listarTodas();

        // Retorna 200 OK com a lista no corpo da resposta
        return ResponseEntity.ok(lista);
    }
}