package com.api.estoque.controller;

import com.api.estoque.dto.request.AlterarSenhaRequest;
import com.api.estoque.dto.request.UsuarioRequest;
import com.api.estoque.dto.request.UsuarioUpdateRequest;
import com.api.estoque.dto.response.UsuarioResponse;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(
            @RequestBody @Valid UsuarioRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        UsuarioResponse response = usuarioService.criarUsuario(request);
        URI uri = uriBuilder.path("/usuarios/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> desativar(@PathVariable Long id,
                                          @AuthenticationPrincipal Usuario adminLogado) {
        usuarioService.desativarUsuario(id,adminLogado);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        usuarioService.ativarUsuario(id);
        return ResponseEntity.ok().build();
    }

    // ENDPOINT PARA LISTAR TODOS OS UTILIZADORES
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao,
            // Adicionamos o novo parâmetro opcional de pesquisa
            @RequestParam(required = false) Optional<String> nome
    ) {
        Page<UsuarioResponse> paginaDeUsuarios = usuarioService.listarTodos(nome, paginacao);
        return ResponseEntity.ok(paginaDeUsuarios);
    }


    // ENDPOINT PARA DETALHAR UM UTILIZADOR
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> detalhar(@PathVariable Long id) {
        UsuarioResponse response = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioUpdateRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        UsuarioResponse response = usuarioService.atualizarUsuario(id, request, usuarioLogado);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/alterar-senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @RequestBody @Valid AlterarSenhaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        usuarioService.alterarSenha(id, request, usuarioLogado);
        return ResponseEntity.noContent().build(); // Retorna 204
    }


}