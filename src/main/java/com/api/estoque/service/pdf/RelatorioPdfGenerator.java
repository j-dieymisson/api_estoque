package com.api.estoque.service.pdf;

import com.lowagie.text.Document;

// A interface usa Generics (<T>) para ser flexível.
// Ela pode gerar um PDF a partir de qualquer tipo de dados (uma Solicitação, uma Lista, etc.)
public interface RelatorioPdfGenerator<T> {

    /**
     * Adiciona o conteúdo específico do relatório ao documento PDF.
     * @param document O documento PDF onde o conteúdo será adicionado.
     * @param dados Os dados (ex: a Solicitação) a serem impressos no relatório.
     */
    void gerar(Document document, T dados);

    /**
     * Retorna o tipo de relatório que esta classe sabe gerar.
     */
    TipoRelatorio getTipo();
}