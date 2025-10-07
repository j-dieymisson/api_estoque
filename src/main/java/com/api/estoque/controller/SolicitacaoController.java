package com.api.estoque.controller;

import com.api.estoque.dto.request.SolicitacaoRequest;
import com.api.estoque.dto.response.HistoricoStatusSolicitacaoResponse;
import com.api.estoque.dto.response.SolicitacaoResponse;
import com.api.estoque.service.HistoricoService;
import com.api.estoque.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;
    private final HistoricoService historicoService;

    // O construtor DEVE receber os dois serviços
    public SolicitacaoController(SolicitacaoService solicitacaoService, HistoricoService historicoService) {
        this.solicitacaoService = solicitacaoService;
        this.historicoService = historicoService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoResponse> criar(
            @RequestBody @Valid SolicitacaoRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        SolicitacaoResponse response = solicitacaoService.criarSolicitacao(request);
        URI uri = uriBuilder.path("/solicitacoes/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @PatchMapping("/{id}/aprovar") // Ex: PATCH http://localhost:8080/solicitacoes/1/aprovar
    public ResponseEntity<SolicitacaoResponse> aprovar(@PathVariable Long id) {
        SolicitacaoResponse response = solicitacaoService.aprovarSolicitacao(id);
        return ResponseEntity.ok(response); // Retorna 200 OK com a solicitação atualizada
    }

    @PatchMapping("/{id}/recusar") // Ex: PATCH http://localhost:8080/solicitacoes/1/recusar
    public ResponseEntity<SolicitacaoResponse> recusar(@PathVariable Long id) {
        SolicitacaoResponse response = solicitacaoService.recusarSolicitacao(id);
        return ResponseEntity.ok(response);
    }
    // O novo endpoint de histórico
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoStatusSolicitacaoResponse>> listarHistoricoDaSolicitacao(@PathVariable Long id) {
        List<HistoricoStatusSolicitacaoResponse> historico = historicoService.buscarPorSolicitacaoId(id);
        return ResponseEntity.ok(historico);
    }

    @GetMapping
    public ResponseEntity<Page<SolicitacaoResponse>> listar(
            @PageableDefault(size = 10, sort = {"dataSolicitacao"}) Pageable paginacao
    ) {
        Page<SolicitacaoResponse> paginaDeSolicitacoes = solicitacaoService.listarTodas(paginacao);
        return ResponseEntity.ok(paginaDeSolicitacoes);
    }
    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoResponse> detalhar(@PathVariable Long id) {
        // A anotação @PathVariable diz ao Spring para pegar o valor {id} da URL
        // e passá-lo como parâmetro para o método.

        SolicitacaoResponse response = solicitacaoService.buscarPorId(id);
        return ResponseEntity.ok(response); // Retorna 200 OK com o corpo da resposta
    }
    @PostMapping("/{id}/devolver-tudo")
    public ResponseEntity<SolicitacaoResponse> devolverTudo(@PathVariable Long id) {
        // Pode receber um DTO no body se quiser adicionar uma observação, por exemplo
        SolicitacaoResponse response = solicitacaoService.devolverTudo(id);
        return ResponseEntity.ok(response);
    }
}