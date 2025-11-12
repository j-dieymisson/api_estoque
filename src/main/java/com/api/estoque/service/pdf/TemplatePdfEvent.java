package com.api.estoque.service.pdf;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Template reutilizável para PDFs.
 * Adiciona um cabeçalho com o logo e um rodapé com data de geração e paginação
 * em TODAS as páginas do documento.
 */
public class TemplatePdfEvent extends PdfPageEventHelper {

    private Image logo;
    private final String dataGeracao;
    private final Font fonteRodape = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.GRAY);
    private String solicitacaoIdTexto = null;
    private String tituloTexto = "";

    public TemplatePdfEvent() {
        super();
        // 1. Carrega o logo
        try {
            URL logoUrl = getClass().getClassLoader().getResource("images/logo_cepra.png");
            if (logoUrl != null) {
                this.logo = Image.getInstance(logoUrl);
                this.logo.scaleToFit(100, 50);
            }
        } catch (IOException | BadElementException e) {
            System.err.println("Erro ao carregar o logo do PDF: " + e.getMessage());
            this.logo = null;
        }

        // 2. Define a data de geração
        this.dataGeracao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /**
     * Construtor para Solicitações (recebe o ID)
     */
    public TemplatePdfEvent(Long solicitacaoId, String titulo) {
        this(); // Chama o construtor padrão (para carregar o logo e a data)
        this.solicitacaoIdTexto = (solicitacaoId != null) ? "N°: " + solicitacaoId.toString() : "";
        this.tituloTexto = (titulo != null) ? titulo : "";
    }

    /**
     * Chamado quando uma página é iniciada.
     * Usamos isto para adicionar o CABEÇALHO (Logo).
     */
    /**
     * Chamado quando uma página é iniciada.
     * Usamos isto para adicionar o CABEÇALHO (Logo, Título e ID).
     */
    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        try {
            // 1. Criar a tabela do cabeçalho com 3 colunas
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);
            headerTable.setTotalWidth(document.right() - document.left());
            headerTable.setLockedWidth(true);
            headerTable.setWidths(new float[]{25f, 50f, 25f}); // Larguras relativas

            // 2. Célula 1: Logo (Esquerda)
            PdfPCell logoCell = new PdfPCell();
            if (this.logo != null) {
                logoCell.addElement(this.logo);
            }
            logoCell.setBorder(Rectangle.NO_BORDER);
            // --- ALTERAÇÃO: Nivelar logo com o título ---
            logoCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            headerTable.addCell(logoCell);

            // 3. Célula 2: Título (Centro)
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            PdfPCell titleCell = new PdfPCell(new Phrase(this.tituloTexto, fontTitulo));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setVerticalAlignment(Element.ALIGN_BOTTOM); // Nivelar com o logo
            titleCell.setPaddingBottom(5f); // Espaço da linha
            headerTable.addCell(titleCell);

            // 4. Célula 3: ID da Solicitação (Direita)
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            PdfPCell idCell = new PdfPCell(new Phrase(this.solicitacaoIdTexto, fontNormal));
            idCell.setBorder(Rectangle.NO_BORDER);
            idCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            // --- ALTERAÇÃO: "Colado" à linha ---
            idCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            idCell.setPaddingBottom(5f); // Espaço da linha
            headerTable.addCell(idCell);

            // 5. Adicionar a tabela ao documento
            document.add(headerTable);

            // 6. Adicionar a linha separadora (Pedido 1)
            // --- ALTERAÇÃO: Linha mais escura ---
            LineSeparator separador = new LineSeparator(0.5f, 100, Color.BLACK, Element.ALIGN_CENTER, -5);
            document.add(separador);

            // 7. Adicionar um espaço ANTES do conteúdo começar
            document.add(new Paragraph(" "));

        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Chamado quando uma página termina.
     * Usamos isto para adicionar o RODAPÉ (Data e Paginação).
     */
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        try {
            // 1. Cria a tabela do rodapé com 3 colunas
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setTotalWidth(document.right() - document.left());
            footerTable.setLockedWidth(true);
            footerTable.setWidths(new float[]{33f, 34f, 33f}); // 3 colunas iguais

            // 2. Célula Esquerda (ID da Solicitação)
            PdfPCell leftCell = new PdfPCell(new Phrase(this.solicitacaoIdTexto, fonteRodape));
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            leftCell.setBorder(Rectangle.NO_BORDER);
            footerTable.addCell(leftCell);

            // 3. Célula Central (Paginação)
            String textoPagina = String.format("Página %d", writer.getPageNumber());
            PdfPCell centerCell = new PdfPCell(new Phrase(textoPagina, fonteRodape));
            centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            centerCell.setBorder(Rectangle.NO_BORDER);
            footerTable.addCell(centerCell);

            // 4. Célula Direita (Data de Geração)
            PdfPCell rightCell = new PdfPCell(new Phrase("Gerado em: " + this.dataGeracao, fonteRodape));
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setBorder(Rectangle.NO_BORDER);
            footerTable.addCell(rightCell);

            // 5. Escreve a tabela no rodapé
            footerTable.writeSelectedRows(0, -1, document.leftMargin(), document.bottom() - 10, writer.getDirectContent());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}