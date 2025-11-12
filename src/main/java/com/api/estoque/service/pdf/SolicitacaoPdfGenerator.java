package com.api.estoque.service.pdf;

import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.SolicitacaoItem;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component // Marca esta classe como um componente Spring para ser injetado no futuro
public class SolicitacaoPdfGenerator implements RelatorioPdfGenerator<Solicitacao> {

    @Override
    public void gerar(Document document, Solicitacao solicitacao) {
        // --- 1. Definição de Fontes ---
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(0x33, 0x33, 0x33)); // Cinza escuro
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontTabelaHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        LineSeparator separador = new LineSeparator(0.5f, 100, Color.BLACK, Element.ALIGN_CENTER, -5);
        try {

            // --- SEÇÃO: DADOS DO SOLICITANTE ---
            document.add(new Paragraph("DADOS DO SOLICITANTE", fontSubtitulo));
            document.add(criarTabelaInfoSolicitante(solicitacao, fontNormal, fontBold));
            document.add(new Paragraph(" ")); // Espaçador

            // --- SEÇÃO: ITENS SOLICITADOS ---
            Paragraph tituloItens = new Paragraph("ITENS SOLICITADOS", fontSubtitulo);
            tituloItens.setSpacingBefore(10f);
            tituloItens.setSpacingAfter(10f);
            document.add(tituloItens);
            document.add(criarTabelaItens(solicitacao, fontTabelaHeader, fontNormal));
            document.add(new Paragraph(" ")); // Espaçador

            // --- SEÇÃO: DATAS E OBSERVAÇÕES ---
            Paragraph tituloDatas = new Paragraph("DATAS PRETENDIDAS", fontSubtitulo);
            tituloDatas.setSpacingBefore(10f);
            tituloDatas.setSpacingAfter(10f);
            document.add(tituloDatas);
            document.add(separador);
            document.add(criarTabelaDatasEJustificativa(solicitacao, fontNormal, fontBold));

            // --- SEÇÃO: ASSINATURAS (Rodapé) ---
            // Adiciona espaço absoluto para empurrar as assinaturas para o fim
            // (Isto pode precisar de ajuste dependendo do seu conteúdo)
            // Vamos deixar isto para o fim, pois o rodapé do TemplatePdfEvent pode ser suficiente.
            // Por agora, vamos adicionar o bloco de assinatura que vimos no exemplo.
            document.add(criarTabelaAssinaturas(solicitacao, fontNormal, fontBold));


        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao construir o conteúdo do PDF da solicitação.", e);
        }
    }

    /**
     * Helper para criar a tabela de "DADOS DO SOLICITANTE".
     */
    private PdfPTable criarTabelaInfoSolicitante(Solicitacao sol, Font fontNormal, Font fontBold) {
        PdfPTable table = new PdfPTable(4); // 4 colunas (Label, Value, Label, Value)
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);

        // --- ALTERAÇÃO AQUI ---
        // Alinhamento dos labels (células 1 e 3) mudado para a ESQUERDA
        addCellSimples(table, "Nome:", fontBold, Element.ALIGN_LEFT);
        addCellSimples(table, sol.getUsuario().getNome(), fontNormal, Element.ALIGN_LEFT);
        addCellSimples(table, "Cargo:", fontBold, Element.ALIGN_LEFT);
        addCellSimples(table, sol.getUsuario().getCargo().getNome(), fontNormal, Element.ALIGN_LEFT);

        addCellSimples(table, "Status:", fontBold, Element.ALIGN_LEFT);
        addCellSimples(table, sol.getStatus().name(), fontNormal, Element.ALIGN_LEFT);
        addCellSimples(table, "Data Solicitação:", fontBold, Element.ALIGN_LEFT);
        addCellSimples(table, sol.getDataSolicitacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontNormal, Element.ALIGN_LEFT);
        // --- FIM DA ALTERAÇÃO ---

        return table;
    }

    /**
     * Helper para formatar as datas (lidando com valores null).
     */
    private String formatarData(LocalDate data) {
        if (data == null) {
            return "Indeterminada"; // Como no PDF de amostra
        }
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Helper para criar a tabela de "DATAS E OBSERVAÇÕES".
     */
    private PdfPTable criarTabelaDatasEJustificativa(Solicitacao sol, Font fontNormal, Font fontBold) {
        PdfPTable table = new PdfPTable(2); // 2 colunas
        table.setWidthPercentage(100);

        // Linha das Datas
        addCellSimples(table, "Recceber: " + formatarData(sol.getDataPrevisaoEntrega()), fontNormal, Element.ALIGN_LEFT);
        addCellSimples(table, "Devolução: " + formatarData(sol.getDataPrevisaoDevolucao()), fontNormal, Element.ALIGN_LEFT);

        // Linha da Justificativa (título)
        PdfPCell cellJustificativaTitulo = new PdfPCell(new Phrase("Observações:", fontBold));
        cellJustificativaTitulo.setColspan(2); // Ocupa 2 colunas
        cellJustificativaTitulo.setBorder(Rectangle.NO_BORDER);
        cellJustificativaTitulo.setPaddingTop(15f);
        table.addCell(cellJustificativaTitulo);

        // Linha da Justificativa (conteúdo)
        PdfPCell cellJustificativa = new PdfPCell(new Phrase(sol.getJustificativa(), fontNormal));
        cellJustificativa.setColspan(2);
        cellJustificativa.setBorder(Rectangle.NO_BORDER);
        table.addCell(cellJustificativa);

        return table;
    }

    /**
     * Helper para criar a tabela de "ITENS SOLICITADOS".
     */
    private PdfPTable criarTabelaItens(Solicitacao sol, Font fontHeader, Font fontNormal) {
        // --- ALTERAÇÃO: Larguras (Descrição maior, UN/Qtd menores) ---
        PdfPTable table = new PdfPTable(new float[]{1.5f, 6f, 1f, 1.5f}); // 4 colunas
        table.setWidthPercentage(100);

        // --- ALTERAÇÃO: Cabeçalho da Tabela ---
        addCellHeader(table, "Código", fontHeader);
        addCellHeader(table, "Descrição", fontHeader);
        addCellHeader(table, "UN", fontHeader);
        addCellHeader(table, "Quant.", fontHeader);
        // --- FIM DA ALTERAÇÃO ---

        // --- ALTERAÇÃO: Itens (Dados) ---
        for (SolicitacaoItem item : sol.getItens()) {
            addCellItem(table, String.valueOf(item.getEquipamento().getId()), fontNormal);
            addCellItem(table, item.getEquipamento().getNome(), fontNormal);
            addCellItem(table, "UN", fontNormal); // "UN" estático
            addCellItem(table, String.valueOf(item.getQuantidadeSolicitada()), fontNormal); // Qtd Solicitada
        }
        // --- FIM DA ALTERAÇÃO ---
        return table;
    }

    /**
     * Helper para criar a tabela de "ASSINATURAS".
     */
    private PdfPTable criarTabelaAssinaturas(Solicitacao sol, Font fontNormal, Font fontBold) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(80f); // Empurra para baixo

        // Assinatura do Solicitante
        String solicitanteStr = "\n\n____________________________\n" + sol.getUsuario().getNome() + "\n(Solicitante)";
        addCellSimples(table, solicitanteStr, fontNormal, Element.ALIGN_CENTER);

        // Assinatura da Autorização
        String autorizacaoStr = "\n\n____________________________\n" + "Autorização";
        addCellSimples(table, autorizacaoStr, fontNormal, Element.ALIGN_CENTER);

        return table;
    }

    // --- MÉTODOS DE CÉLULA REUTILIZÁVEIS ---

    /**
     * Cria uma célula de tabela de cabeçalho (Fundo escuro, texto branco).
     */
    private void addCellHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(0x33, 0x33, 0x33)); // Cinza escuro
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    /**
     * Cria uma célula de tabela de item (com bordas normais).
     */
    private void addCellItem(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    /**
     * Cria uma célula de layout simples (sem bordas).
     */
    private void addCellSimples(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }

    @Override
    public TipoRelatorio getTipo() {
        return TipoRelatorio.SOLICITACAO;
    }
}