package com.api.estoque.service.pdf;

import com.api.estoque.model.Equipamento;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListaEquipamentosPdfGenerator implements RelatorioPdfGenerator<List<Equipamento>> {

    @Override
    public void gerar(Document document, List<Equipamento> equipamentos) {
        try {
            // --- CABEÇALHO ---
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Relatório de Inventário de Equipamentos", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n\n"));

            // --- TABELA DE EQUIPAMENTOS ---
            PdfPTable table = new PdfPTable(6); // 6 colunas
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 4f, 3f, 2f, 2f, 1.5f});

            Font fontCabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            table.addCell(new PdfPCell(new Phrase("ID", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Nome", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Categoria", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Qtd. Total", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Qtd. Disp.", fontCabecalho)));
            table.addCell(new PdfPCell(new Phrase("Ativo", fontCabecalho)));

            for (Equipamento eq : equipamentos) {
                table.addCell(String.valueOf(eq.getId()));
                table.addCell(eq.getNome());
                table.addCell(eq.getCategoria().getNome());
                table.addCell(String.valueOf(eq.getQuantidadeTotal()));
                table.addCell(String.valueOf(eq.getQuantidadeDisponivel()));
                table.addCell(eq.isAtivo() ? "Sim" : "Não");
            }
            document.add(table);

        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao construir o PDF de inventário.", e);
        }
    }

    @Override
    public TipoRelatorio getTipo() {
        return TipoRelatorio.LISTA_EQUIPAMENTOS;
    }
}