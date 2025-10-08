package com.api.estoque.controller;

import com.api.estoque.dto.request.AjusteEstoqueRequest;
import com.api.estoque.dto.request.EquipamentoRequest;
import com.api.estoque.dto.response.EquipamentoResponse;
import com.api.estoque.service.EquipamentoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController // Anotação que combina @Controller e @ResponseBody, ideal para APIs REST
@RequestMapping("/equipamentos") // Define o endereço base para todos os métodos neste controller
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @PostMapping // Mapeia este método para requisições HTTP POST para /equipamentos
    public ResponseEntity<EquipamentoResponse> cadastrar(
            @RequestBody @Valid EquipamentoRequest request, // Pega o corpo da requisição e valida
            UriComponentsBuilder uriBuilder
    ) {
        EquipamentoResponse response = equipamentoService.criarEquipamento(request);

        // Cria a URI para o novo recurso criado (boa prática REST)
        URI uri = uriBuilder.path("/equipamentos/{id}").buildAndExpand(response.id()).toUri();

        // Retorna o status 201 Created, a URI no header 'Location', e o objeto criado no corpo
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping // Mapeia este método para requisições HTTP GET para /equipamentos
    public ResponseEntity<Page<EquipamentoResponse>> listar(

            @PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao,
            @RequestParam(required = false) Optional<String> nome
    ) {
        Page<EquipamentoResponse> pageDeEquipamentos = equipamentoService.listarTodos(nome, paginacao);
        return ResponseEntity.ok(pageDeEquipamentos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipamentoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid EquipamentoRequest request
    ) {
        EquipamentoResponse response = equipamentoService.atualizarEquipamento(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        equipamentoService.desativarEquipamento(id);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content
    }

    @GetMapping("/todos")
    public ResponseEntity<Page<EquipamentoResponse>> listarTodosAdmin(
            @PageableDefault(size = 10, sort = {"id"}) Pageable paginacao
    ) {
        Page<EquipamentoResponse> pageDeEquipamentos = equipamentoService.listarTodosAdmin(paginacao);
        return ResponseEntity.ok(pageDeEquipamentos);
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        equipamentoService.ativarEquipamento(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/ajustar-estoque")
    public ResponseEntity<EquipamentoResponse> ajustarEstoque(
            @PathVariable Long id,
            @RequestBody @Valid AjusteEstoqueRequest request
    ) {
        EquipamentoResponse response = equipamentoService.ajustarEstoque(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<Page<EquipamentoResponse>> listarDisponiveis(
            @PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao
    ) {
        Page<EquipamentoResponse> pageDeEquipamentos = equipamentoService.listarDisponiveis(paginacao);
        return ResponseEntity.ok(pageDeEquipamentos);
    }
}