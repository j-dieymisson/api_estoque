package com.api.estoque.controller;

import com.api.estoque.dto.response.HistoricoResponse;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.HistoricoService;
import com.api.estoque.service.PdfService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/historico")
public class HistoricoController {

    private final HistoricoService historicoService;
    private final PdfService pdfService;

    public HistoricoController(HistoricoService historicoService,
                               PdfService pdfService
    ) {
        this.historicoService = historicoService;
        this.pdfService = pdfService;
    }

    @GetMapping("/equipamento/{id}")
    public ResponseEntity<Page<HistoricoResponse>> listarPorEquipamento(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = {"dataMovimentacao"}) Pageable paginacao,
            // Adicionamos os novos parâmetros opcionais
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dataFim
    ) {
        Page<HistoricoResponse> historicoPage = historicoService.buscarPorEquipamentoId(id, dataInicio, dataFim, paginacao);
        return ResponseEntity.ok(historicoPage);
    }

    @GetMapping("/equipamento/{id}/pdf")
    public ResponseEntity<byte[]> gerarPdfHistoricoEquipamento(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        byte[] pdfBytes = pdfService.gerarPdfHistoricoEquipamento(id, usuarioLogado);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "historico_equipamento_" + id + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

}