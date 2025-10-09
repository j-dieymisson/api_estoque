package com.api.estoque.service.pdf;

import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.SolicitacaoItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component // Marca esta classe como um componente Spring para ser injetado no futuro
public class SolicitacaoPdfGenerator implements RelatorioPdfGenerator<Solicitacao> {

    @Override
    public void gerar(Document document, Solicitacao solicitacao) {
        // Todo o código que desenha o PDF de uma solicitação vem para aqui.

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try {
            // --- CABEÇALHO DO DOCUMENTO ---
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Relatório de Solicitação de Equipamento", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n"));

            // --- INFORMAÇÕES GERAIS DA SOLICITAÇÃO ---
            document.add(new Paragraph("Número da Solicitação: " + solicitacao.getId()));
            document.add(new Paragraph("Status: " + solicitacao.getStatus()));
            document.add(new Paragraph("Solicitante: " + solicitacao.getUsuario().getNome()));
            document.add(new Paragraph("Data da Solicitação: " + solicitacao.getDataSolicitacao().format(formatter)));
            document.add(new Paragraph("Data Prev. Entrega: " + (solicitacao.getDataPrevisaoEntrega() != null ? solicitacao.getDataPrevisaoEntrega().toString() : "N/A")));
            document.add(new Paragraph("Data Prev. Devolução: " + (solicitacao.getDataPrevisaoDevolucao() != null ? solicitacao.getDataPrevisaoDevolucao().toString() : "N/A")));
            document.add(new Paragraph("Justificativa: " + solicitacao.getJustificativa()));
            document.add(new Paragraph("\n\n"));

            // --- TABELA DE ITENS ---
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            Font fontCabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            table.addCell(new PdfPCell(new Phrase("Item (ID)", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Equipamento", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Qtd. Solicitada", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Qtd. Devolvida", fontCabecalho)));

            for (SolicitacaoItem item : solicitacao.getItens()) {
                table.addCell(String.valueOf(item.getId()));
                table.addCell(item.getEquipamento().getNome());
                table.addCell(String.valueOf(item.getQuantidadeSolicitada()));
                table.addCell(String.valueOf(item.getTotalDevolvido()));
            }
            document.add(table);

        } catch (DocumentException e) {
            // A exceção será tratada no PdfService, que é quem chama este método.
            throw new RuntimeException("Erro ao construir o conteúdo do PDF da solicitação.", e);
        }
    }

    @Override
    public TipoRelatorio getTipo() {
        return TipoRelatorio.SOLICITACAO;
    }
}