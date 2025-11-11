package com.api.estoque.controller;

import com.api.estoque.dto.request.SolicitacaoRequest;
import com.api.estoque.dto.request.SolicitacaoUpdateRequest;
import com.api.estoque.dto.response.ContagemResponse;
import com.api.estoque.dto.response.HistoricoStatusSolicitacaoResponse;
import com.api.estoque.dto.response.SolicitacaoResponse;
import com.api.estoque.model.StatusSolicitacao;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.HistoricoService;
import com.api.estoque.service.PdfService;
import com.api.estoque.service.SolicitacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/solicitacoes")
@Tag(name = "Solicitações", description = "Endpoints para gerir o ciclo de vida completo das solicitações de equipamentos")
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
            @AuthenticationPrincipal Usuario usuarioLogado,
            UriComponentsBuilder uriBuilder
    ) {
        SolicitacaoResponse response = solicitacaoService.criarSolicitacao(request, usuarioLogado);
        URI uri = uriBuilder.path("/solicitacoes/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @PatchMapping("/{id}/aprovar-gestor")
    @Operation(summary = "Aprovação Nível 1 (Gestor)")
    public ResponseEntity<SolicitacaoResponse> aprovarComoGestor(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado) {
        SolicitacaoResponse response = solicitacaoService.aprovarComoGestor(id, usuarioLogado);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/aprovar-admin")
    @Operation(summary = "Aprovação Nível 2 (Admin - Final)")
    public ResponseEntity<SolicitacaoResponse> aprovarComoAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado) {
        SolicitacaoResponse response = solicitacaoService.aprovarComoAdmin(id, usuarioLogado);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/recusar")
    public ResponseEntity<SolicitacaoResponse> recusar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado) { // <-- PARÂMETRO ADICIONADO
        SolicitacaoResponse response = solicitacaoService.recusarSolicitacao(id, usuarioLogado); // <-- PARÂMETRO PASSADO
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
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PageableDefault(size = 5, sort = {"dataSolicitacao"}, direction = Sort.Direction.DESC) Pageable paginacao,
            @RequestParam(required = false) Optional<Long> usuarioId,

            // --- A MUDANÇA ESTÁ AQUI ---
            @RequestParam(required = false) List<StatusSolicitacao> statuses,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dataFim,
            @RequestParam(required = false) Boolean devolucaoIndeterminada
    ) {
        Page<SolicitacaoResponse> paginaDeSolicitacoes = solicitacaoService.listarTodasSolicitacoes(
                usuarioLogado,
                usuarioId,
                // --- E AQUI (para passar null em vez de uma lista vazia) ---
                (statuses == null || statuses.isEmpty()) ? null : statuses,
                dataInicio, dataFim, devolucaoIndeterminada, paginacao
        );
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
    public ResponseEntity<SolicitacaoResponse> devolverTudo(@PathVariable Long id,
                                                            @AuthenticationPrincipal Usuario usuarioLogado) {
        // Pode receber um DTO no body se quiser adicionar uma observação, por exemplo
        SolicitacaoResponse response = solicitacaoService.devolverTudo(id, usuarioLogado);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SolicitacaoResponse> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado) {
        SolicitacaoResponse response = solicitacaoService.cancelarSolicitacao(id,usuarioLogado);
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

    @GetMapping("/minhas")
    @Operation(
            summary = "Listar as minhas solicitações",
            description = "Retorna uma lista paginada de todas as solicitações pertencentes ao utilizador autenticado. Permite filtrar por status e por data.")
    public ResponseEntity<Page<SolicitacaoResponse>> listarMinhas(
            @ParameterObject
            @PageableDefault(size = 10, sort = {"dataSolicitacao"}) Pageable paginacao,

            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestParam(required = false) List<StatusSolicitacao> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dataFim,
            @RequestParam(required = false) Boolean devolucaoIndeterminada
    ) {
        // --- A CORREÇÃO ESTÁ AQUI ---
        // Aplicamos a mesma verificação de 'isEmpty' que o método 'listar' usa
        Page<SolicitacaoResponse> paginaDeSolicitacoes = solicitacaoService.listarMinhasSolicitacoes(
                usuarioLogado,
                (statuses == null || statuses.isEmpty()) ? null : statuses, // <-- CORRIGIDO
                dataInicio,
                dataFim,
                devolucaoIndeterminada,
                paginacao
        );
        // --- FIM DA CORREÇÃO ---

        return ResponseEntity.ok(paginaDeSolicitacoes);
    }

    @GetMapping("/status")
    public ResponseEntity<List<String>> listarStatus() {
        // Pega em todos os valores do Enum StatusSolicitacao, converte-os para String e devolve como uma lista
        List<String> status = Arrays.stream(StatusSolicitacao.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/pendentes/contagem")
    public ResponseEntity<ContagemResponse> getContagemPendentes(
            @AuthenticationPrincipal Usuario usuarioLogado) { // <-- PARÂMETRO ADICIONADO
        long contagem = solicitacaoService.contarSolicitacoesPendentes(usuarioLogado); // <-- PARÂMETRO PASSADO
        return ResponseEntity.ok(new ContagemResponse(contagem));
    }
}