package com.api.estoque.controller;

import com.api.estoque.dto.request.SolicitacaoRequest;
import com.api.estoque.dto.request.SolicitacaoUpdateRequest;
import com.api.estoque.dto.response.HistoricoStatusSolicitacaoResponse;
import com.api.estoque.dto.response.SolicitacaoResponse;
import com.api.estoque.model.StatusSolicitacao;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.HistoricoService;
import com.api.estoque.service.PdfService;
import com.api.estoque.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;
    private final HistoricoService historicoService;
    private final PdfService pdfService;

    // O construtor DEVE receber os dois serviços
    public SolicitacaoController(SolicitacaoService solicitacaoService,
                                 HistoricoService historicoService,
                                 PdfService pdfService) {
        this.solicitacaoService = solicitacaoService;
        this.historicoService = historicoService;
        this.pdfService = pdfService;
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
            @PageableDefault(size = 10, sort = {"dataSolicitacao"}) Pageable paginacao,
            @RequestParam(required = false) Optional<StatusSolicitacao> status,
            @RequestParam(required = false) Optional<Long> usuarioId
    ) {
        Page<SolicitacaoResponse> paginaDeSolicitacoes = solicitacaoService.listarTodas(status, usuarioId, paginacao);
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

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SolicitacaoResponse> cancelar(@PathVariable Long id) {
        SolicitacaoResponse response = solicitacaoService.cancelarSolicitacao(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolicitacaoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid SolicitacaoUpdateRequest request
    ) {
        SolicitacaoResponse response = solicitacaoService.atualizarSolicitacaoPendente(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> gerarPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        // 3. Chama o serviço para gerar o PDF, passando o utilizador autenticado para a auditoria
        byte[] pdfBytes = pdfService.gerarPdfSolicitacao(id, usuarioLogado);

        // 4. Prepara os cabeçalhos da resposta para indicar que é um ficheiro PDF
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // O nome do ficheiro que será sugerido para download
        headers.setContentDispositionFormData("attachment", "solicitacao_" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}