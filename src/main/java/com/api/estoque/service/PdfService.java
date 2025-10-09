package com.api.estoque.service;

import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.HistoricoGeracaoPdf;
import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.SolicitacaoItem;
import com.api.estoque.model.Usuario;
import com.api.estoque.repository.HistoricoGeracaoPdfRepository;
import com.api.estoque.repository.SolicitacaoRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final HistoricoGeracaoPdfRepository historicoRepository;

    public PdfService(SolicitacaoRepository solicitacaoRepository, HistoricoGeracaoPdfRepository historicoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional
    public byte[] gerarPdfSolicitacao(Long solicitacaoId, Usuario usuarioLogado) {
        // 1. Busca os dados da solicitação que queremos imprimir
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + solicitacaoId));

        // Formatação de datas para o relatório
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // --- CABEÇALHO DO DOCUMENTO ---
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Relatório de Solicitação de Equipamento", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n")); // Linha em branco

            // --- INFORMAÇÕES GERAIS DA SOLICITAÇÃO ---
            document.add(new Paragraph("Número da Solicitação: " + solicitacao.getId()));
            document.add(new Paragraph("Status: " + solicitacao.getStatus()));
            document.add(new Paragraph("Solicitante: " + solicitacao.getUsuario().getNome()));
            document.add(new Paragraph("Data da Solicitação: " + solicitacao.getDataSolicitacao().format(formatter)));
            document.add(new Paragraph("Justificativa: " + solicitacao.getJustificativa()));
            document.add(new Paragraph("\n\n"));

            // --- TABELA DE ITENS ---
            PdfPTable table = new PdfPTable(4); // 4 colunas
            table.setWidthPercentage(100);

            // Cabeçalhos da tabela
            Font fontCabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            table.addCell(new PdfPCell(new Phrase("Item (ID)", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Equipamento", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Qtd. Solicitada", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Qtd. Devolvida", fontCabecalho)));

            // Preenche a tabela com os itens da solicitação
            for (SolicitacaoItem item : solicitacao.getItens()) {
                table.addCell(String.valueOf(item.getId()));
                table.addCell(item.getEquipamento().getNome());
                table.addCell(String.valueOf(item.getQuantidadeSolicitada()));
                table.addCell(String.valueOf(item.getTotalDevolvido()));
            }
            document.add(table);
            document.close();

            // --- AUDITORIA: Regista que o PDF foi gerado ---
            HistoricoGeracaoPdf historico = new HistoricoGeracaoPdf();
            historico.setSolicitacao(solicitacao);
            historico.setUsuario(usuarioLogado);
            historico.setDataGeracao(LocalDateTime.now());
            historicoRepository.save(historico);

            return baos.toByteArray();

        } catch (DocumentException | IOException e) {
            // Lança uma exceção se a geração do PDF ou o fecho do stream falhar
            throw new RuntimeException("Erro ao gerar o PDF da solicitação.", e);
        }
    }
}