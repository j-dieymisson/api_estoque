package com.api.estoque.service;

import com.api.estoque.dto.request.SolicitacaoItemRequest;
import com.api.estoque.dto.request.SolicitacaoRequest;
import com.api.estoque.dto.request.SolicitacaoUpdateRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final HistoricoMovimentacaoRepository historicoRepository;
    private final HistoricoStatusSolicitacaoRepository historicoStatusRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final SolicitacaoItemRepository solicitacaoItemRepository;


    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                              UsuarioRepository usuarioRepository,
                              EquipamentoRepository equipamentoRepository,
                              HistoricoMovimentacaoRepository historicoRepository,
                              HistoricoStatusSolicitacaoRepository historicoStatusRepository,
                              DevolucaoRepository devolucaoRepository,
                              SolicitacaoItemRepository solicitacaoItemRepository
    ) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.historicoRepository = historicoRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.solicitacaoItemRepository = solicitacaoItemRepository;
    }

    @Transactional
    public SolicitacaoResponse criarSolicitacao(SolicitacaoRequest request, Usuario usuarioLogado) {
        // Para uma solicitação final, as datas são obrigatórias
        if (request.dataPrevisaoEntrega() == null || request.dataPrevisaoDevolucao() == null) {
            throw new BusinessException("As datas de previsão de entrega e devolução são obrigatórias para submeter uma solicitação.");
        }
        // Validação extra: data de devolução não pode ser antes da de entrega
        if (request.dataPrevisaoDevolucao().isBefore(request.dataPrevisaoEntrega())) {
            throw new BusinessException("A data de previsão de devolução não pode ser anterior à data de entrega.");
        }

        // Já não precisamos de buscar o utilizador, ele já veio como parâmetro
        if (!usuarioLogado.isAtivo()) {
            throw new BusinessException("Utilizador está inativo e não pode criar solicitações.");
        }

        // 2. Cria o objeto principal da solicitação
        Solicitacao novaSolicitacao = new Solicitacao();
        novaSolicitacao.setUsuario(usuarioLogado);
        novaSolicitacao.setDataSolicitacao(LocalDateTime.now());
        novaSolicitacao.setStatus(StatusSolicitacao.PENDENTE); // Toda nova solicitação começa como pendente
        novaSolicitacao.setJustificativa(request.justificativa());
        novaSolicitacao.setDataPrevisaoEntrega(request.dataPrevisaoEntrega());
        novaSolicitacao.setDataPrevisaoDevolucao(request.dataPrevisaoDevolucao());

        // 3. Processa cada item do pedido
        processarEAgruparItens(request.itens(), novaSolicitacao);

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

            // Captura a quantidade ANTES da mudança
            int qtdAnterior = equipamento.getQuantidadeDisponivel();

            // Diminui a quantidade disponível no estoque.
            int novaQuantidade = equipamento.getQuantidadeDisponivel() - item.getQuantidadeSolicitada();
            equipamento.setQuantidadeDisponivel(novaQuantidade);

            // Captura a quantidade DEPOIS da mudança
            int qtdPosterior = equipamento.getQuantidadeDisponivel();

            HistoricoMovimentacao registroHistorico = HistoricoMovimentacao.builder()
                    .dataMovimentacao(LocalDateTime.now())
                    .tipoMovimentacao(TipoMovimentacao.SAIDA)
                    .quantidade(item.getQuantidadeSolicitada() * -1)
                    .quantidadeAnterior(qtdAnterior)
                    .quantidadePosterior(qtdPosterior)
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
    public SolicitacaoResponse criarRascunho(SolicitacaoRequest request, Usuario usuarioLogado) {

        if (!usuarioLogado.isAtivo()) {
            throw new BusinessException("Utilizador está inativo e não pode criar rascunhos.");
        }

        Solicitacao novoRascunho = new Solicitacao();
        novoRascunho.setUsuario(usuarioLogado);
        novoRascunho.setDataSolicitacao(LocalDateTime.now());
        novoRascunho.setStatus(StatusSolicitacao.RASCUNHO);
        novoRascunho.setJustificativa(request.justificativa());
        novoRascunho.setDataPrevisaoEntrega(request.dataPrevisaoEntrega());
        novoRascunho.setDataPrevisaoDevolucao(request.dataPrevisaoDevolucao());

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

        // Antes de se tornar 'PENDENTE', deve ter as datas obrigatórias
        if (rascunho.getDataPrevisaoEntrega() == null || rascunho.getDataPrevisaoDevolucao() == null) {
            throw new BusinessException("Não é possível enviar a solicitação. As datas de previsão de entrega e devolução são obrigatórias.");
        }
        // Também validamos a ordem das datas
        if (rascunho.getDataPrevisaoDevolucao().isBefore(rascunho.getDataPrevisaoEntrega())) {
            throw new BusinessException("A data de devolução não pode ser anterior à data de entrega.");
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
    public SolicitacaoResponse atualizarRascunho(Long rascunhoId, SolicitacaoRequest request, Usuario usuarioLogado) {
        Solicitacao rascunho = solicitacaoRepository.findById(rascunhoId)
                .orElseThrow(() -> new ResourceNotFoundException("Rascunho não encontrado"));

        if (!rascunho.getUsuario().getId().equals(usuarioLogado.getId())) {
            throw new BusinessException("Você não tem permissão para editar este rascunho.");
        }
        if (rascunho.getStatus() != StatusSolicitacao.RASCUNHO) {
            throw new BusinessException("Apenas rascunhos podem ser editados.");
        }

        // Atualiza os dados gerais
        rascunho.setJustificativa(request.justificativa());
        rascunho.setDataPrevisaoEntrega(request.dataPrevisaoEntrega());
        rascunho.setDataPrevisaoDevolucao(request.dataPrevisaoDevolucao());

        // 1. Apaga os itens antigos
        solicitacaoItemRepository.deleteAll(rascunho.getItens());
        rascunho.getItens().clear();

        // 2. Adiciona os novos itens (lógica copiada do 'criarRascunho')
        for (SolicitacaoItemRequest itemRequest : request.itens()) {
            Equipamento equipamento = equipamentoRepository.findById(itemRequest.equipamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + itemRequest.equipamentoId()));

            SolicitacaoItem item = new SolicitacaoItem();
            item.setSolicitacao(rascunho);
            item.setEquipamento(equipamento);
            item.setQuantidadeSolicitada(itemRequest.quantidade());
            rascunho.getItens().add(item);
        }

        solicitacaoRepository.save(rascunho);
        return mapToSolicitacaoResponse(rascunho);
    }


    private SolicitacaoResponse mapToSolicitacaoResponse(Solicitacao solicitacao) {
        List<SolicitacaoItemResponse> itemResponses = new ArrayList<>();
        for (SolicitacaoItem item : solicitacao.getItens()) {

            // 1. Calcula a nova informação
            int pendente = item.getQuantidadeSolicitada() - item.getTotalDevolvido();

            itemResponses.add(new SolicitacaoItemResponse(
                    item.getId(),
                    item.getEquipamento().getId(),
                    item.getEquipamento().getNome(),
                    item.getQuantidadeSolicitada(),
                    item.getTotalDevolvido(),
                    pendente
            ));
        }

        return new SolicitacaoResponse(
                solicitacao.getId(),
                solicitacao.getUsuario().getNome(),
                solicitacao.getDataSolicitacao(),
                solicitacao.getDataPrevisaoEntrega(),
                solicitacao.getDataPrevisaoDevolucao(),
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

    // MÉTODO para a visão de Admin/Gestor
    @Transactional(readOnly = true)
    public Page<SolicitacaoResponse> listarTodasSolicitacoes(
            Usuario usuarioLogado, // Recebe o objeto Usuario
            Optional<Long> usuarioId,
            Optional<StatusSolicitacao> status,
            Optional<LocalDate> dataInicio,
            Optional<LocalDate> dataFim,
            Pageable pageable) {

        LocalDateTime inicio = dataInicio.map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime fim = dataFim.map(d -> d.atTime(23, 59, 59)).orElse(null);

        // A chamada agora inclui o ID do utilizador, correspondendo à nova assinatura
        Page<Solicitacao> solicitacoes = solicitacaoRepository.findAdminView(
                usuarioLogado.getId(),
                usuarioId.orElse(null),
                status.orElse(null),
                inicio,
                fim,
                pageable
        );

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

    // Dentro da classe SolicitacaoService.java

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

            if (quantidadeParaDevolver > 0) {
                Equipamento equipamento = item.getEquipamento();

                // 2a. Captura a quantidade ANTES da mudança
                int qtdAnterior = equipamento.getQuantidadeDisponivel();

                // 2b. Reabastece o stock
                equipamento.setQuantidadeDisponivel(equipamento.getQuantidadeDisponivel() + quantidadeParaDevolver);

                // 2c. Captura a quantidade DEPOIS da mudança
                int qtdPosterior = equipamento.getQuantidadeDisponivel();

                // 2d. Cria o registo da devolução
                Devolucao novaDevolucao = new Devolucao();
                novaDevolucao.setSolicitacaoItem(item);
                item.getDevolucoes().add(novaDevolucao);
                novaDevolucao.setQuantidadeDevolvida(quantidadeParaDevolver);
                novaDevolucao.setDataDevolucao(LocalDateTime.now());
                novaDevolucao.setObservacao("Devolução total automática.");
                devolucaoRepository.save(novaDevolucao);

                // 2e. Regista no histórico de movimentação COM OS DADOS CORRETOS
                HistoricoMovimentacao registoHistorico = HistoricoMovimentacao.builder()
                        .dataMovimentacao(LocalDateTime.now())
                        .tipoMovimentacao(TipoMovimentacao.DEVOLUCAO)
                        .quantidade(quantidadeParaDevolver)
                        .quantidadeAnterior(qtdAnterior)      // <-- CORRIGIDO
                        .quantidadePosterior(qtdPosterior)     // <-- CORRIGIDO
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

    @Transactional
    public SolicitacaoResponse cancelarSolicitacao(Long id,Usuario usuarioLogado) {
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));


        // Um Admin pode cancelar qualquer solicitação, mas um Colaborador/Gestor só pode cancelar as suas.
        if (usuarioLogado.getCargo().getNome().equals("ADMIN") == false) {
            if (!solicitacao.getUsuario().getId().equals(usuarioLogado.getId())) {
                throw new BusinessException("Você não tem permissão para cancelar uma solicitação que não é sua.");
            }
        }

        // REGRA DE NEGÓCIO: Só se pode cancelar um pedido que ainda está pendente.
        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new BusinessException("Apenas solicitações com status PENDENTE podem ser canceladas. Status atual: " + solicitacao.getStatus());
        }

        StatusSolicitacao statusAnterior = solicitacao.getStatus();
        solicitacao.setStatus(StatusSolicitacao.CANCELADA);
        Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        // Regista a alteração no histórico de status
        registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.CANCELADA);

        return mapToSolicitacaoResponse(solicitacaoSalva);
    }

    @Transactional
    public SolicitacaoResponse atualizarSolicitacaoPendente(Long id, SolicitacaoUpdateRequest request) {
        // 1. Busca a solicitação
        Solicitacao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

        // 2. O ESCUDO PROTETOR: Garante que só se pode editar solicitações pendentes.
        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new BusinessException("Apenas solicitações com status PENDENTE podem ser editadas.");
        }

        // 3. Limpa os itens antigos para os substituir. O orphanRemoval apaga-os da BD.
        solicitacao.getItens().clear();

        // 4. Adiciona os novos itens, validando o stock para cada um.
        processarEAgruparItens(request.itens(), solicitacao);

        // 5. Atualiza a justificação e salva.
        solicitacao.setJustificativa(request.justificativa());
        solicitacaoRepository.save(solicitacao);

        // Não há mudança de status, então não é necessário registar no histórico de status.

        return mapToSolicitacaoResponse(solicitacao);
    }

    private void processarEAgruparItens(List<SolicitacaoItemRequest> itensRequest, Solicitacao solicitacao) {
        // Usamos um Map para agrupar itens pelo ID do equipamento
        Map<Long, SolicitacaoItemRequest> itensAgrupados = new HashMap<>();

        for (SolicitacaoItemRequest itemReq : itensRequest) {
            itensAgrupados.merge(
                    itemReq.equipamentoId(),
                    itemReq,
                    (itemExistente, novoItem) -> new SolicitacaoItemRequest(
                            itemExistente.equipamentoId(),
                            itemExistente.quantidade() + novoItem.quantidade() // Soma as quantidades
                    )
            );
        }

        // Agora, processamos a lista já agrupada e sem duplicados
        for (SolicitacaoItemRequest itemAgrupado : itensAgrupados.values()) {
            Equipamento equipamento = equipamentoRepository.findById(itemAgrupado.equipamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + itemAgrupado.equipamentoId()));

            if (!equipamento.isAtivo()) {
                throw new BusinessException("Não é possível solicitar o equipamento '" + equipamento.getNome() + "' porque ele está inativo.");
            }
            
            // A validação de stock continua aqui, agora sobre a quantidade total somada
            if (equipamento.getQuantidadeDisponivel() < itemAgrupado.quantidade()) {
                throw new BusinessException("Stock insuficiente para o equipamento: " + equipamento.getNome());
            }

            SolicitacaoItem novoItem = new SolicitacaoItem();
            novoItem.setEquipamento(equipamento);
            novoItem.setQuantidadeSolicitada(itemAgrupado.quantidade());
            solicitacao.adicionarItem(novoItem);
        }
    }

    @Transactional(readOnly = true)
    public Page<SolicitacaoResponse> listarMinhasSolicitacoes(
            Usuario usuarioLogado,
            Optional<StatusSolicitacao> status,
            Optional<LocalDate> dataInicio,
            Optional<LocalDate> dataFim,
            Pageable pageable) {

        LocalDateTime inicio = dataInicio.map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime fim = dataFim.map(d -> d.atTime(23, 59, 59)).orElse(null);

        // Agora chama a nova query, que foi feita especificamente para esta visão
        Page<Solicitacao> solicitacoes = solicitacaoRepository.findMyView(
                usuarioLogado.getId(),
                status.orElse(null),
                inicio,
                fim,
                pageable
        );

        return solicitacoes.map(this::mapToSolicitacaoResponse);
    }
}