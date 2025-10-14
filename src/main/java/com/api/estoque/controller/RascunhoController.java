package com.api.estoque.controller;

import com.api.estoque.dto.request.SolicitacaoRequest;
import com.api.estoque.dto.response.SolicitacaoResponse;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/rascunhos") // Endpoint base para todas as operações de rascunho
public class RascunhoController {

    private final SolicitacaoService solicitacaoService;

    public RascunhoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoResponse> criar(
            @RequestBody @Valid SolicitacaoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado,
            UriComponentsBuilder uriBuilder
    ) {
        // Chama o novo método de serviço para criar um rascunho
        SolicitacaoResponse response = solicitacaoService.criarRascunho(request, usuarioLogado);

        // A URI do novo rascunho ainda pode apontar para o recurso de solicitação, pois é o mesmo objeto
        URI uri = uriBuilder.path("/solicitacoes/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);
    }

    @PatchMapping("/{id}/enviar")
    public ResponseEntity<SolicitacaoResponse> enviar(@PathVariable Long id) {
        // Chama o método para "promover" o rascunho a uma solicitação pendente
        SolicitacaoResponse response = solicitacaoService.enviarRascunho(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoResponse>> listarRascunhos(@RequestParam Long usuarioId) {
        // NOTA: Quando tivermos segurança, não precisaremos de passar o usuarioId.
        // Pegaremos o utilizador do token de autenticação.
        List<SolicitacaoResponse> rascunhos = solicitacaoService.listarRascunhosPorUsuario(usuarioId);
        return ResponseEntity.ok(rascunhos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolicitacaoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid SolicitacaoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        SolicitacaoResponse response = solicitacaoService.atualizarRascunho(id, request,usuarioLogado);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        solicitacaoService.apagarRascunho(id);
        // O status 204 No Content é o retorno padrão para uma operação de delete bem-sucedida.
        return ResponseEntity.noContent().build();
    }
}