package com.api.estoque.service.pdf;

import com.api.estoque.model.HistoricoMovimentacao;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class HistoricoEquipamentoPdfGenerator implements RelatorioPdfGenerator<List<HistoricoMovimentacao>> {

    @Override
    public void gerar(Document document, List<HistoricoMovimentacao> dados) {
        if (dados.isEmpty()) {
            document.add(new Paragraph("Nenhuma movimentação encontrada para este equipamento."));
            return;
        }

        // Pega o nome do equipamento a partir do primeiro registo do histórico
        String nomeEquipamento = dados.get(0).getEquipamento().getNome();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try {
            // --- CABEÇALHO ---
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titulo = new Paragraph("Histórico de Movimentação: " + nomeEquipamento, fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n\n"));

            // --- TABELA DE MOVIMENTAÇÕES ---
            PdfPTable table = new PdfPTable(6); // 6 colunas
            table.setWidthPercentage(100);

            Font fontCabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            table.addCell(new PdfPCell(new Phrase("Data", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Tipo", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Qtd.", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Stock Anterior", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Stock Posterior", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Responsável", fontCabecalho)));

            for (HistoricoMovimentacao historico : dados) {
                table.addCell(historico.getDataMovimentacao().format(formatter));
                table.addCell(historico.getTipoMovimentacao().name());
                table.addCell(String.valueOf(historico.getQuantidade()));
                table.addCell(String.valueOf(historico.getQuantidadeAnterior()));
                table.addCell(String.valueOf(historico.getQuantidadePosterior()));
                table.addCell(historico.getUsuarioResponsavel().getNome());
            }
            document.add(table);

        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao construir o conteúdo do PDF de histórico.", e);
        }
    }

    @Override
    public TipoRelatorio getTipo() {
        return TipoRelatorio.HISTORICO_EQUIPAMENTO;
    }
}