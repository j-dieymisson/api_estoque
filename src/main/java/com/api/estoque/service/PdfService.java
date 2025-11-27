package com.api.estoque.service;

import com.api.estoque.exception.BusinessException;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.*;
import com.api.estoque.repository.EquipamentoRepository;
import com.api.estoque.repository.HistoricoGeracaoPdfRepository;
import com.api.estoque.repository.HistoricoMovimentacaoRepository;
import com.api.estoque.repository.SolicitacaoRepository;
import com.api.estoque.service.pdf.RelatorioPdfGenerator;
import com.api.estoque.service.pdf.TemplatePdfEvent;
import com.api.estoque.service.pdf.TipoRelatorio;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final HistoricoGeracaoPdfRepository historicoRepository;
    private final HistoricoMovimentacaoRepository historicoMovimentacaoRepository;
    private final EquipamentoRepository equipamentoRepository;

    // Um mapa para encontrar rapidamente o gerador de PDF correto pelo seu tipo
    private final Map<TipoRelatorio, RelatorioPdfGenerator<?>> geradores;

    // O Spring injeta aqui uma LISTA de todos os beans que implementam a nossa interface
    public PdfService(SolicitacaoRepository solicitacaoRepository,
                      HistoricoGeracaoPdfRepository historicoRepository,
                      HistoricoMovimentacaoRepository historicoMovimentacaoRepository,
                      EquipamentoRepository equipamentoRepository,
                      List<RelatorioPdfGenerator<?>> geradoresList) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.historicoRepository = historicoRepository;
        this.historicoMovimentacaoRepository = historicoMovimentacaoRepository;
        this.equipamentoRepository = equipamentoRepository;

        // Transformamos a lista num mapa para acesso rápido (Tipo -> Gerador)
        this.geradores = geradoresList.stream()
                .collect(Collectors.toMap(RelatorioPdfGenerator::getTipo, Function.identity()));
    }

    @Transactional
    public byte[] gerarPdfSolicitacao(Long solicitacaoId, Usuario usuarioLogado) {
        // 1. Busca os dados da solicitação
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + solicitacaoId));

        // 2. Encontra o gerador de PDF específico para o tipo SOLICITACAO
        RelatorioPdfGenerator<Solicitacao> gerador = (RelatorioPdfGenerator<Solicitacao>) getGerador(TipoRelatorio.SOLICITACAO);

        // 3. Gera o PDF em memória usando o gerador encontrado
        byte[] pdfBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new TemplatePdfEvent(solicitacao.getId(), "Solicitação de Equipamentos"));
            document.open();

            // Delega a criação do CONTEÚDO para a classe especialista
            gerador.gerar(document, solicitacao);

            document.close();
            pdfBytes = baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erro ao gerar o PDF da solicitação.", e);
        }

        // 4. Regista a auditoria (continua a ser responsabilidade do serviço principal)
        HistoricoGeracaoPdf historico = new HistoricoGeracaoPdf();
        historico.setSolicitacao(solicitacao);
        historico.setUsuario(usuarioLogado);
        historico.setDataGeracao(LocalDateTime.now());
        historicoRepository.save(historico);

        return pdfBytes;
    }

    // Método auxiliar para encontrar o gerador no nosso mapa
    private RelatorioPdfGenerator<?> getGerador(TipoRelatorio tipo) {
        RelatorioPdfGenerator<?> gerador = geradores.get(tipo);
        if (gerador == null) {
            throw new BusinessException("Nenhum gerador de PDF encontrado para o tipo: " + tipo);
        }
        return gerador;
    }

    public byte[] gerarPdfListaEquipamentos() {
        // 1. Busca os dados: todos os equipamentos
        List<Equipamento> equipamentos = equipamentoRepository.findAll();

        // 2. Encontra o gerador de PDF específico
        RelatorioPdfGenerator<List<Equipamento>> gerador =
                (RelatorioPdfGenerator<List<Equipamento>>) getGerador(TipoRelatorio.LISTA_EQUIPAMENTOS);

        // 3. Gera o PDF em memória
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new TemplatePdfEvent());
            document.open();
            gerador.gerar(document, equipamentos); // Delega para o especialista
            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erro ao gerar o PDF de inventário.", e);
        }
    }

    @Transactional
    public byte[] gerarPdfHistoricoEquipamento(Long equipamentoId, Usuario usuarioLogado) {
        // 1. Busca os dados: a lista de todas as movimentações para o equipamento
        List<HistoricoMovimentacao> historico = historicoMovimentacaoRepository
                .findAllByEquipamentoIdOrderByDataMovimentacaoDesc(equipamentoId);

        // 2. Encontra o gerador de PDF específico
        RelatorioPdfGenerator<List<HistoricoMovimentacao>> gerador =
                (RelatorioPdfGenerator<List<HistoricoMovimentacao>>) getGerador(TipoRelatorio.HISTORICO_EQUIPAMENTO);

        // 3. Gera o PDF em memória (a lógica de criar o Document, etc. é a mesma)
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new TemplatePdfEvent());
            document.open();
            gerador.gerar(document, historico); // Delega para o especialista
            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erro ao gerar o PDF de histórico.", e);
        }

    }
}