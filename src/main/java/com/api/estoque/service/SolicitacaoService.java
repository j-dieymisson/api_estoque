package com.api.estoque.service;

import com.api.estoque.dto.request.SolicitacaoItemRequest;
import com.api.estoque.dto.request.SolicitacaoRequest;
import com.api.estoque.dto.response.SolicitacaoItemResponse;
import com.api.estoque.dto.response.SolicitacaoResponse;
import com.api.estoque.exception.BusinessException;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.*;
import com.api.estoque.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final HistoricoMovimentacaoRepository historicoRepository;
    private final HistoricoStatusSolicitacaoRepository historicoStatusRepository;
    private final DevolucaoRepository devolucaoRepository;


    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                              UsuarioRepository usuarioRepository,
                              EquipamentoRepository equipamentoRepository,
                              HistoricoMovimentacaoRepository historicoRepository,
                              HistoricoStatusSolicitacaoRepository historicoStatusRepository,
                              DevolucaoRepository devolucaoRepository
    ) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.historicoRepository = historicoRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.devolucaoRepository = devolucaoRepository;
    }

    @Transactional
    public SolicitacaoResponse criarSolicitacao(SolicitacaoRequest request) {
        // 1. Busca o usuário que está fazendo a solicitação
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + request.usuarioId()));

        if (!usuario.isAtivo()) {
            throw new BusinessException("Usuário está inativo e não pode criar solicitações.");
        }

        // 2. Cria o objeto principal da solicitação
        Solicitacao novaSolicitacao = new Solicitacao();
        novaSolicitacao.setUsuario(usuario);
        novaSolicitacao.setDataSolicitacao(LocalDateTime.now());
        novaSolicitacao.setStatus(StatusSolicitacao.PENDENTE); // Toda nova solicitação começa como pendente
        novaSolicitacao.setJustificativa(request.justificativa());

        // 3. Processa cada item do pedido
        for (SolicitacaoItemRequest itemRequest : request.itens()) {
            Equipamento equipamento = equipamentoRepository.findById(itemRequest.equipamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + itemRequest.equipamentoId()));

            // ===== REGRA DE NEGÓCIO MAIS IMPORTANTE =====
            // Verifica se a quantidade solicitada está disponível no estoque
            if (equipamento.getQuantidadeDisponivel() < itemRequest.quantidade()) {
                throw new BusinessException("Estoque insuficiente para o equipamento: " + equipamento.getNome());
            }

            // Cria o item da solicitação e o associa ao equipamento e à solicitação principal
            SolicitacaoItem novoItem = new SolicitacaoItem();
            novoItem.setEquipamento(equipamento);
            novoItem.setQuantidadeSolicitada(itemRequest.quantidade());

            novaSolicitacao.adicionarItem(novoItem); // Usa o método auxiliar que criamos na entidade
        }

        // 4. Salva a solicitação (e seus itens, graças ao CascadeType.ALL)
        Solicitacao solicitacaoSalva = solicitacaoRepository.save(novaSolicitacao);

        // Registar o primeiro status no histórico
        registrarHistoricoDeStatus(solicitacaoSalva, null, StatusSolicitacao.PENDENTE);

        // 5. Mapeia a entidade salva para o DTO de resposta
        return mapToSolicitacaoResponse(solicitacaoSalva);
    }


    @Transactional
    public SolicitacaoResponse aprovarSolicitacao(Long id) {
        // 1. Busca a solicitação no banco de dados
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

        // 2. REGRA DE NEGÓCIO: Só podemos aprovar solicitações que estão pendentes.
        if (solicitacao.getItens().isEmpty()) {
            throw new BusinessException("Não é possível aprovar uma solicitação sem itens.");
        }

        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new BusinessException("Apenas solicitações com status PENDENTE podem ser aprovadas. Status atual: " + solicitacao.getStatus());
        }

        // 3. Itera sobre os itens para dar baixa no estoque
        for (SolicitacaoItem item : solicitacao.getItens()) {
            Equipamento equipamento = item.getEquipamento();

            // Validação extra de segurança, caso o estoque tenha mudado desde a criação.
            if (equipamento.getQuantidadeDisponivel() < item.getQuantidadeSolicitada()) {
                throw new BusinessException("Estoque insuficiente para o equipamento: " + equipamento.getNome() + ". A aprovação não pode ser concluída.");
            }

            // Diminui a quantidade disponível no estoque.
            int novaQuantidade = equipamento.getQuantidadeDisponivel() - item.getQuantidadeSolicitada();
            equipamento.setQuantidadeDisponivel(novaQuantidade);

            HistoricoMovimentacao registroHistorico = HistoricoMovimentacao.builder()
                    .dataMovimentacao(LocalDateTime.now())
                    .tipoMovimentacao(TipoMovimentacao.SAIDA)
                    .quantidade(item.getQuantidadeSolicitada())
                    .equipamento(equipamento)
                    .solicitacao(solicitacao)
                    .usuarioResponsavel(solicitacao.getUsuario())
                    .build();

            historicoRepository.save(registroHistorico);
        }

        // 4. Altera o status da solicitação
        StatusSolicitacao statusAnterior = solicitacao.getStatus();
        solicitacao.setStatus(StatusSolicitacao.APROVADA);

        // 5. Salva a solicitação (agora com o novo status)
        Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        // Registar a mudança de status no histórico
        registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.APROVADA);

        return mapToSolicitacaoResponse(solicitacaoSalva);
    }

    @Transactional
    public SolicitacaoResponse recusarSolicitacao(Long id) {
        // 1. Busca a solicitação
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

        // 2. REGRA DE NEGÓCIO: Valida o status
        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new BusinessException("Apenas solicitações com status PENDENTE podem ser recusadas. Status atual: " + solicitacao.getStatus());
        }

        // 3. Altera o status. Note que aqui não mexemos no estoque.
        StatusSolicitacao statusAnterior = solicitacao.getStatus();
        solicitacao.setStatus(StatusSolicitacao.RECUSADA);
        Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.RECUSADA);

        return mapToSolicitacaoResponse(solicitacaoSalva);
    }

    @Transactional
    public SolicitacaoResponse criarRascunho(SolicitacaoRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com o ID: " + request.usuarioId()));

        if (!usuario.isAtivo()) {
            throw new BusinessException("Utilizador está inativo e não pode criar rascunhos.");
        }

        Solicitacao novoRascunho = new Solicitacao();
        novoRascunho.setUsuario(usuario);
        novoRascunho.setDataSolicitacao(LocalDateTime.now());
        novoRascunho.setStatus(StatusSolicitacao.RASCUNHO);
        novoRascunho.setJustificativa(request.justificativa());

        // Adiciona os itens JÁ VALIDANDO O STOCK
        for (SolicitacaoItemRequest itemRequest : request.itens()) {
            Equipamento equipamento = equipamentoRepository.findById(itemRequest.equipamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + itemRequest.equipamentoId()));


            if (equipamento.getQuantidadeDisponivel() < itemRequest.quantidade()) {
                throw new BusinessException("Stock insuficiente para o equipamento: " + equipamento.getNome());
            }
            // =================================================================

            SolicitacaoItem novoItem = new SolicitacaoItem();
            novoItem.setEquipamento(equipamento);
            novoItem.setQuantidadeSolicitada(itemRequest.quantidade());

            novoRascunho.adicionarItem(novoItem);
        }

        Solicitacao rascunhoSalvo = solicitacaoRepository.save(novoRascunho);
        registrarHistoricoDeStatus(rascunhoSalvo, null, StatusSolicitacao.RASCUNHO);

        return mapToSolicitacaoResponse(rascunhoSalvo);
    }

    @Transactional
    public SolicitacaoResponse enviarRascunho(Long id) {
        // 1. Busca a solicitação que está em modo rascunho
        Solicitacao rascunho = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rascunho de solicitação não encontrado com o ID: " + id));

        // 2. REGRA DE NEGÓCIO: Só podemos enviar solicitações com status RASCUNHO.
        if (rascunho.getStatus() != StatusSolicitacao.RASCUNHO) {
            throw new BusinessException("Apenas solicitações com status RASCUNHO podem ser enviadas. Status atual: " + rascunho.getStatus());
        }

        // 3. Itera sobre os itens para VALIDAR O STOCK (lógica que foi pulada na criação do rascunho)
        for (SolicitacaoItem item : rascunho.getItens()) {
            Equipamento equipamento = item.getEquipamento();

            // A mesma validação de stock do método criarSolicitacao
            if (equipamento.getQuantidadeDisponivel() < item.getQuantidadeSolicitada()) {
                throw new BusinessException("Stock insuficiente para o equipamento: " + equipamento.getNome() + ". Não é possível enviar a solicitação.");
            }
        }

        // 4. Altera o status de RASCUNHO para PENDENTE
        StatusSolicitacao statusAnterior = rascunho.getStatus();
        rascunho.setStatus(StatusSolicitacao.PENDENTE);

        // 5. Salva a solicitação com o novo status
        Solicitacao solicitacaoEnviada = solicitacaoRepository.save(rascunho);

        // 6. Regista a mudança de status no histórico
        registrarHistoricoDeStatus(solicitacaoEnviada, statusAnterior, StatusSolicitacao.PENDENTE);

        return mapToSolicitacaoResponse(solicitacaoEnviada);
    }

    @Transactional
    public void apagarRascunho(Long id) {
        Solicitacao rascunho = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rascunho não encontrado com o ID: " + id));

        if (rascunho.getStatus() != StatusSolicitacao.RASCUNHO) {
            throw new BusinessException("Apenas solicitações com status RASCUNHO podem ser apagadas.");
        }

        // Apaga o registo do histórico de status primeiro para evitar problemas de constraint
        historicoStatusRepository.deleteAll(rascunho.getHistoricoStatus());

        solicitacaoRepository.delete(rascunho);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoResponse> listarRascunhosPorUsuario(Long usuarioId) {
        List<Solicitacao> rascunhos = solicitacaoRepository.findAllByUsuarioIdAndStatus(usuarioId, StatusSolicitacao.RASCUNHO);
        return rascunhos.stream()
                .map(this::mapToSolicitacaoResponse)
                .toList();
    }

    // MÉTODO PARA ATUALIZAR
    @Transactional
    public SolicitacaoResponse atualizarRascunho(Long id, SolicitacaoRequest request) {
        Solicitacao rascunho = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rascunho não encontrado com o ID: " + id));

        if (rascunho.getStatus() != StatusSolicitacao.RASCUNHO) {
            throw new BusinessException("Apenas solicitações com status RASCUNHO podem ser editadas.");
        }

        rascunho.getItens().clear();

        for (SolicitacaoItemRequest itemRequest : request.itens()) {
            Equipamento equipamento = equipamentoRepository.findById(itemRequest.equipamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + itemRequest.equipamentoId()));


            if (equipamento.getQuantidadeDisponivel() < itemRequest.quantidade()) {
                throw new BusinessException("Stock insuficiente para o equipamento: " + equipamento.getNome());
            }


            SolicitacaoItem novoItem = new SolicitacaoItem();
            novoItem.setEquipamento(equipamento);
            novoItem.setQuantidadeSolicitada(itemRequest.quantidade());
            rascunho.adicionarItem(novoItem);
        }

        rascunho.setJustificativa(request.justificativa());

        Solicitacao rascunhoSalvo = solicitacaoRepository.save(rascunho);
        // Não precisamos de registar histórico aqui, pois o status não mudou.

        return mapToSolicitacaoResponse(rascunhoSalvo);
    }


    private SolicitacaoResponse mapToSolicitacaoResponse(Solicitacao solicitacao) {
        List<SolicitacaoItemResponse> itemResponses = new ArrayList<>();
        for (SolicitacaoItem item : solicitacao.getItens()) {
            itemResponses.add(new SolicitacaoItemResponse(
                    item.getId(),
                    item.getEquipamento().getNome(),
                    item.getQuantidadeSolicitada(),
                    item.getTotalDevolvido()
            ));
        }

        return new SolicitacaoResponse(
                solicitacao.getId(),
                solicitacao.getUsuario().getNome(),
                solicitacao.getDataSolicitacao(),
                solicitacao.getStatus().name(), // .name() converte o Enum para String
                solicitacao.getJustificativa(),
                itemResponses
        );
    }

    private void registrarHistoricoDeStatus(Solicitacao solicitacao,
                                            StatusSolicitacao statusAnterior,
                                            StatusSolicitacao statusNovo) {
        HistoricoStatusSolicitacao registo = HistoricoStatusSolicitacao.builder()
                .dataAlteracao(LocalDateTime.now())
                .solicitacao(solicitacao)
                .statusAnterior(statusAnterior)
                .statusNovo(statusNovo)
                .usuarioResponsavel(solicitacao.getUsuario()) // Por agora, usamos o próprio solicitante
                .build();
        historicoStatusRepository.save(registo);
    }

    @Transactional(readOnly = true)
    public Page<SolicitacaoResponse> listarTodas(Pageable pageable) {
        // Busca todas as solicitações do banco de forma paginada
        Page<Solicitacao> solicitacoes = solicitacaoRepository.findAll(pageable);

        // Mapeia a página de entidades para uma página de DTOs
        return solicitacoes.map(this::mapToSolicitacaoResponse);
    }

    @Transactional(readOnly = true)
    public SolicitacaoResponse buscarPorId(Long id) {
        // Usa o método findById do repositório, que retorna um Optional
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                // Se não encontrar, lança a nossa exceção customizada de 'Não Encontrado'
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

        // Reutiliza o nosso método privado de mapeamento para converter a entidade para DTO
        return mapToSolicitacaoResponse(solicitacao);
    }

    @Transactional
    public SolicitacaoResponse devolverTudo(Long solicitacaoId) {
        // 1. Busca a solicitação e valida o seu estado
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + solicitacaoId));

        if (solicitacao.getStatus() != StatusSolicitacao.APROVADA) {
            throw new BusinessException("Apenas solicitações com status APROVADA podem ser devolvidas em massa.");
        }

        // 2. Itera sobre cada item da solicitação para processar a devolução
        for (SolicitacaoItem item : solicitacao.getItens()) {
            int quantidadeJaDevolvida = item.getTotalDevolvido();
            int quantidadeParaDevolver = item.getQuantidadeSolicitada() - quantidadeJaDevolvida;

            // Se ainda houver itens para devolver nesta linha
            if (quantidadeParaDevolver > 0) {
                Equipamento equipamento = item.getEquipamento();

                // 2a. Cria o registo da devolução
                Devolucao novaDevolucao = new Devolucao();
                novaDevolucao.setSolicitacaoItem(item);
                item.getDevolucoes().add(novaDevolucao); // Sincroniza o objeto em memória
                novaDevolucao.setQuantidadeDevolvida(quantidadeParaDevolver);
                novaDevolucao.setDataDevolucao(LocalDateTime.now());
                novaDevolucao.setObservacao("Devolução total automática.");
                devolucaoRepository.save(novaDevolucao); // Salva a devolução

                // 2b. Reabastece o stock
                equipamento.setQuantidadeDisponivel(equipamento.getQuantidadeDisponivel() + quantidadeParaDevolver);

                // 2c. Regista no histórico de movimentação de equipamento
                HistoricoMovimentacao registoHistorico = HistoricoMovimentacao.builder()
                        .dataMovimentacao(LocalDateTime.now())
                        .tipoMovimentacao(TipoMovimentacao.DEVOLUCAO)
                        .quantidade(quantidadeParaDevolver)
                        .equipamento(equipamento)
                        .solicitacao(solicitacao)
                        .devolucao(novaDevolucao)
                        .usuarioResponsavel(solicitacao.getUsuario())
                        .build();
                historicoRepository.save(registoHistorico);
            }
        }

        // 3. Finaliza a solicitação
        StatusSolicitacao statusAnterior = solicitacao.getStatus();
        solicitacao.setStatus(StatusSolicitacao.FINALIZADA);
        Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        // 4. Regista a finalização no histórico de status
        registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.FINALIZADA);

        return mapToSolicitacaoResponse(solicitacaoSalva);
    }
}