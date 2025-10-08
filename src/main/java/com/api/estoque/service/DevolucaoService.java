package com.api.estoque.service;

import com.api.estoque.dto.request.DevolucaoRequest;
import com.api.estoque.dto.response.DevolucaoResponse;
import com.api.estoque.exception.BusinessException;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.*;
import com.api.estoque.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DevolucaoService {

    private final DevolucaoRepository devolucaoRepository;
    // Precisamos do SolicitacaoItemRepository para buscar o item que está sendo devolvido
    private final SolicitacaoItemRepository solicitacaoItemRepository;
    private final HistoricoMovimentacaoRepository historicoRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final HistoricoStatusSolicitacaoRepository historicoStatusRepository;

    public DevolucaoService(DevolucaoRepository devolucaoRepository,
                            SolicitacaoItemRepository solicitacaoItemRepository,
                            HistoricoMovimentacaoRepository historicoRepository,
                            SolicitacaoRepository solicitacaoRepository,
                            HistoricoStatusSolicitacaoRepository historicoStatusRepository) {
        this.devolucaoRepository = devolucaoRepository;
        this.solicitacaoItemRepository = solicitacaoItemRepository;
        this.historicoRepository = historicoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.historicoStatusRepository = historicoStatusRepository;
    }

    @Transactional
    public DevolucaoResponse registrarDevolucao(DevolucaoRequest request) {
        // 1. Busca o item da solicitação original que está sendo devolvido.
        SolicitacaoItem itemSolicitado = solicitacaoItemRepository.findById(request.solicitacaoItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item de solicitação não encontrado com o ID: " + request.solicitacaoItemId()));

        // 2. Validações / Regras de Negócio
        validarDevolucao(itemSolicitado, request.quantidadeDevolvida());

        // 3. Cria a nova entidade Devolucao
        Devolucao novaDevolucao = new Devolucao();
        novaDevolucao.setSolicitacaoItem(itemSolicitado);
        itemSolicitado.getDevolucoes().add(novaDevolucao); // Sincroniza o objeto em memória
        novaDevolucao.setQuantidadeDevolvida(request.quantidadeDevolvida());
        novaDevolucao.setDataDevolucao(LocalDateTime.now());
        novaDevolucao.setObservacao(request.observacao());

        // 4. ATUALIZA O ESTOQUE e captura os valores
        Equipamento equipamento = itemSolicitado.getEquipamento();
        int qtdAnterior = equipamento.getQuantidadeDisponivel(); // Captura ANTES

        int novaQuantidadeDisponivel = equipamento.getQuantidadeDisponivel() + request.quantidadeDevolvida();
        equipamento.setQuantidadeDisponivel(novaQuantidadeDisponivel);

        int qtdPosterior = equipamento.getQuantidadeDisponivel(); // Captura DEPOIS

        // 5. Salva a devolução no banco.
        Devolucao devolucaoSalva = devolucaoRepository.save(novaDevolucao);

        // 6. Regista o histórico com os valores corretos
        HistoricoMovimentacao registroHistorico = HistoricoMovimentacao.builder()
                .dataMovimentacao(LocalDateTime.now())
                .tipoMovimentacao(TipoMovimentacao.DEVOLUCAO)
                .quantidade(devolucaoSalva.getQuantidadeDevolvida())
                .quantidadeAnterior(qtdAnterior)             // <-- Usa o valor capturado
                .quantidadePosterior(qtdPosterior)            // <-- Usa o valor capturado
                .equipamento(equipamento)
                .solicitacao(itemSolicitado.getSolicitacao())
                .devolucao(devolucaoSalva)
                .usuarioResponsavel(itemSolicitado.getSolicitacao().getUsuario())
                .build();
        historicoRepository.save(registroHistorico);

        // 7. Verifica se a solicitação pode ser finalizada
        verificarEFinalizarSolicitacaoSeCompleta(itemSolicitado.getSolicitacao());

        return mapToDevolucaoResponse(devolucaoSalva);
    }

    private void verificarEFinalizarSolicitacaoSeCompleta(Solicitacao solicitacao) {
        // Recarrega a solicitação para garantir que temos todos os itens e devoluções mais recentes
        Solicitacao solicitacaoAtualizada = solicitacaoRepository.findById(solicitacao.getId()).get();

        boolean todosItensDevolvidos = true;
        for (SolicitacaoItem item : solicitacaoAtualizada.getItens()) {
            if (item.getQuantidadeSolicitada() != item.getTotalDevolvido()) {
                todosItensDevolvidos = false;
                break;
            }
        }

        if (todosItensDevolvidos) {
            StatusSolicitacao statusAnterior = solicitacaoAtualizada.getStatus();
            solicitacaoAtualizada.setStatus(StatusSolicitacao.FINALIZADA);
            solicitacaoRepository.save(solicitacaoAtualizada);

            // Regista esta importante mudança no histórico de status
            registrarHistoricoDeStatus(solicitacaoAtualizada, statusAnterior, StatusSolicitacao.FINALIZADA);
        }
    }

    // Adicione também o método privado para registar o histórico de status, que podemos copiar do SolicitacaoService
    private void registrarHistoricoDeStatus(Solicitacao solicitacao, StatusSolicitacao statusAnterior, StatusSolicitacao statusNovo) {
        HistoricoStatusSolicitacao registo = HistoricoStatusSolicitacao.builder()
                .dataAlteracao(LocalDateTime.now())
                .solicitacao(solicitacao)
                .statusAnterior(statusAnterior)
                .statusNovo(statusNovo)
                .usuarioResponsavel(solicitacao.getUsuario())
                .build();
        historicoStatusRepository.save(registo);
    }

    private void validarDevolucao(SolicitacaoItem itemSolicitado, int quantidadeADevolver) {
        // REGRA 1: A solicitação original precisa ter sido APROVADA.
        if (itemSolicitado.getSolicitacao().getStatus() != StatusSolicitacao.APROVADA) {
            throw new BusinessException("Só é possível devolver itens de solicitações com status APROVADA.");
        }

        // REGRA 2: O usuário não pode devolver mais itens do que solicitou.
        int totalJaDevolvido = itemSolicitado.getTotalDevolvido(); // Usa nosso método auxiliar
        int quantidadeSolicitada = itemSolicitado.getQuantidadeSolicitada();

        if ((totalJaDevolvido + quantidadeADevolver) > quantidadeSolicitada) {
            throw new BusinessException(
                    "Devolução excede a quantidade solicitada. " +
                            "Solicitado: " + quantidadeSolicitada +
                            ", Já devolvido: " + totalJaDevolvido +
                            ", Tentando devolver: " + quantidadeADevolver
            );
        }
    }

    private DevolucaoResponse mapToDevolucaoResponse(Devolucao devolucao) {
        SolicitacaoItem item = devolucao.getSolicitacaoItem();
        Solicitacao solicitacao = item.getSolicitacao();
        Equipamento equipamento = item.getEquipamento();

        return new DevolucaoResponse(
                devolucao.getId(),
                solicitacao.getId(),
                equipamento.getNome(),
                devolucao.getQuantidadeDevolvida(),
                devolucao.getDataDevolucao(),
                devolucao.getObservacao()
        );
    }

    @Transactional(readOnly = true)
    public Page<DevolucaoResponse> listarTodas(Pageable pageable) {
        // Busca todas as devoluções de forma paginada
        Page<Devolucao> devolucoes = devolucaoRepository.findAll(pageable);

        // Reutiliza o nosso método de mapeamento para converter para DTOs
        return devolucoes.map(this::mapToDevolucaoResponse);
    }

    // MÉTODO PARA BUSCAR UMA DEVOLUÇÃO POR ID
    @Transactional(readOnly = true)
    public DevolucaoResponse buscarPorId(Long id) {
        Devolucao devolucao = devolucaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolução não encontrada com o ID: " + id));

        return mapToDevolucaoResponse(devolucao);
    }
}