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
import java.util.Optional;

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
    public ResponseEntity<List<CategoriaResponse>> listar(
            @RequestParam(required = false) Optional<Boolean> ativa
    ) {
        // A anotação @RequestParam pega o valor do parâmetro da URL (ex: ?ativa=true)
        // 'required = false' indica que o parâmetro é opcional.
        List<CategoriaResponse> lista = categoriaService.listarTodas(ativa);
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid CategoriaRequest request
    ) {
        // A anotação @PathVariable pega o {id} da URL.
        // A anotação @RequestBody pega o JSON do corpo da requisição.

        CategoriaResponse response = categoriaService.atualizarCategoria(id, request);
        return ResponseEntity.ok(response); // Retorna 200 OK com o objeto atualizado.
    }
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> detalhar(@PathVariable Long id) {
        CategoriaResponse response = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    // ENDPOINT PARA DESATIVAR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        categoriaService.desativarCategoria(id);
        // O status 204 No Content é a resposta padrão para uma operação
        // de exclusão (ou desativação) bem-sucedida, pois não há conteúdo para retornar.
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        categoriaService.ativarCategoria(id);
        // Retornamos 200 OK ou 204 No Content. 200 OK com uma resposta vazia é comum.
        return ResponseEntity.ok().build();
    }
}