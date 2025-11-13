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

                // 1. Valida a DATA
                validarDatas(request.dataPrevisaoEntrega(), request.dataPrevisaoDevolucao());

                if (!usuarioLogado.isAtivo()) {
                    throw new BusinessException("Utilizador está inativo e não pode criar solicitações.");
                }

                // --- NOVA LÓGICA DE STATUS (BASEADA EM CARGO E SETOR) ---
                // Determina o status inicial baseado na hierarquia
                StatusSolicitacao statusInicial;
                String cargoUsuario = usuarioLogado.getCargo().getNome();

                // SE for Gestor ou Admin, pula a primeira etapa
                if ("GESTOR".equals(cargoUsuario) || "ADMIN".equals(cargoUsuario)) {
                    statusInicial = StatusSolicitacao.PENDENTE_ADMIN;
                } else {
                    // SE for Colaborador, verifica se o seu setor tem gestores
                    Setor setorDoUsuario = usuarioLogado.getSetor();
                    if (setorDoUsuario != null &&
                            usuarioRepository.existsBySetorAndCargoNome(setorDoUsuario, "GESTOR")) {
                        // Se tem gestor no setor, vai para o Gestor
                        statusInicial = StatusSolicitacao.PENDENTE_GESTOR;
                    } else {
                        // Se o setor não tem gestor (ou o colaborador não tem setor), pula para o Admin
                        statusInicial = StatusSolicitacao.PENDENTE_ADMIN;
                    }
                }

                // 2. Cria o objeto principal da solicitação
                Solicitacao novaSolicitacao = new Solicitacao();
                novaSolicitacao.setUsuario(usuarioLogado);
                novaSolicitacao.setDataSolicitacao(LocalDateTime.now());
                novaSolicitacao.setStatus(statusInicial); // <-- MUDANÇA AQUI
                novaSolicitacao.setJustificativa(request.justificativa());
                novaSolicitacao.setDataPrevisaoEntrega(request.dataPrevisaoEntrega());
                novaSolicitacao.setDataPrevisaoDevolucao(request.dataPrevisaoDevolucao());

                // 3. Processa cada item do pedido
                processarEAgruparItens(request.itens(), novaSolicitacao);

                // 4. Salva a solicitação
                Solicitacao solicitacaoSalva = solicitacaoRepository.save(novaSolicitacao);

                // Registar o primeiro status no histórico (com o utilizador correto)
                registrarHistoricoDeStatus(solicitacaoSalva, null, statusInicial, usuarioLogado); // <-- MUDANÇA AQUI

                // 5. Mapeia a entidade salva para o DTO de resposta
                return mapToSolicitacaoResponse(solicitacaoSalva);
            }


            @Transactional
            public SolicitacaoResponse aprovarComoGestor(Long id, Usuario gestorLogado) {
                // 1. Busca a solicitação
                Solicitacao solicitacao = solicitacaoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

                // 2. REGRA DE NEGÓCIO: Só pode aprovar o que está PENDENTE_GESTOR
                if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE_GESTOR) {
                    throw new BusinessException("Esta solicitação não está aguardando aprovação do gestor. Status atual: " + solicitacao.getStatus());
                }

                // 3. REGRA DE SEGURANÇA: Verifica se o gestor logado é o gestor do solicitante
                Setor setorSolicitante = solicitacao.getUsuario().getSetor();
                Setor setorGestor = gestorLogado.getSetor();

                // Verifica se o solicitante tem um setor E se o gestor logado pertence a esse mesmo setor
                if (setorSolicitante == null || !setorSolicitante.equals(setorGestor)) {
                    throw new BusinessException("Você não pertence ao setor do solicitante e não pode aprovar este pedido.");
                }

                // 4. Valida itens (ativo/stock) ANTES de aprovar
                for (SolicitacaoItem item : solicitacao.getItens()) {
                    Equipamento equipamento = item.getEquipamento();
                    if (!equipamento.isAtivo()) {
                        throw new BusinessException("O equipamento '" + equipamento.getNome() + "' está INATIVO. A solicitação não pode ser aprovada.");
                    }
                    if (equipamento.getQuantidadeDisponivel() < item.getQuantidadeSolicitada()) {
                        throw new BusinessException("Estoque insuficiente para '" + equipamento.getNome() + "'. A aprovação não pode ser concluída.");
                    }
                }

                // 5. Altera o status para o próximo nível
                StatusSolicitacao statusAnterior = solicitacao.getStatus();
                solicitacao.setStatus(StatusSolicitacao.PENDENTE_ADMIN); // <-- MUDANÇA AQUI

                // 6. Salva e regista no histórico
                Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);
                registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.PENDENTE_ADMIN, gestorLogado);

                return mapToSolicitacaoResponse(solicitacaoSalva);
            }

            @Transactional
            public SolicitacaoResponse aprovarComoAdmin(Long id, Usuario adminLogado) {
                // 1. Busca a solicitação
                Solicitacao solicitacao = solicitacaoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

                // 2. REGRA DE NEGÓCIO: Só pode aprovar o que está PENDENTE_ADMIN
                if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE_ADMIN) {
                    throw new BusinessException("Esta solicitação não está aguardando aprovação do admin. Status atual: " + solicitacao.getStatus());
                }

                if (solicitacao.getItens().isEmpty()) {
                    throw new BusinessException("Não é possível aprovar uma solicitação sem itens.");
                }

                // 3. --- LÓGICA DE BAIXA DE STOCK (MOVIDA PARA AQUI) ---
                for (SolicitacaoItem item : solicitacao.getItens()) {
                    Equipamento equipamento = item.getEquipamento();

                    // Validação final de segurança (ativo/stock)
                    if (equipamento.getQuantidadeDisponivel() < item.getQuantidadeSolicitada()) {
                        throw new BusinessException("Estoque insuficiente para o equipamento: " + equipamento.getNome() + ". A aprovação não pode ser concluída.");
                    }
                    if (!equipamento.isAtivo()) {
                        throw new BusinessException("O equipamento '" + equipamento.getNome() + "' está INATIVO e não pode ser solicitado.");
                    }

                    int qtdAnterior = equipamento.getQuantidadeDisponivel();
                    int novaQuantidade = equipamento.getQuantidadeDisponivel() - item.getQuantidadeSolicitada();
                    equipamento.setQuantidadeDisponivel(novaQuantidade);
                    int qtdPosterior = equipamento.getQuantidadeDisponivel();

                    HistoricoMovimentacao registroHistorico = HistoricoMovimentacao.builder()
                            .dataMovimentacao(LocalDateTime.now())
                            .tipoMovimentacao(TipoMovimentacao.SAIDA)
                            .quantidade(item.getQuantidadeSolicitada() * -1)
                            .quantidadeAnterior(qtdAnterior)
                            .quantidadePosterior(qtdPosterior)
                            .equipamento(equipamento)
                            .solicitacao(solicitacao)
                            .usuarioResponsavel(adminLogado) // <-- Responsável é o Admin
                            .build();

                    historicoRepository.save(registroHistorico);
                }
                // --- FIM DA LÓGICA DE STOCK ---

                // 4. Altera o status da solicitação
                StatusSolicitacao statusAnterior = solicitacao.getStatus();
                solicitacao.setStatus(StatusSolicitacao.APROVADA);

                // 5. Salva a solicitação
                Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

                // 6. Registar a mudança de status no histórico
                registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.APROVADA, adminLogado);

                return mapToSolicitacaoResponse(solicitacaoSalva);
            }

            @Transactional
            public SolicitacaoResponse recusarSolicitacao(Long id, Usuario usuarioLogado) { // Adicionámos usuarioLogado
                // 1. Busca a solicitação
                Solicitacao solicitacao = solicitacaoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

                // 2. REGRA DE NEGÓCIO: Pode recusar em qualquer etapa PENDENTE
                if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE_GESTOR &&
                        solicitacao.getStatus() != StatusSolicitacao.PENDENTE_ADMIN) {
                    throw new BusinessException("Apenas solicitações pendentes podem ser recusadas. Status atual: " + solicitacao.getStatus());
                }

                // 3. Altera o status. Não mexemos no estoque.
                StatusSolicitacao statusAnterior = solicitacao.getStatus();
                solicitacao.setStatus(StatusSolicitacao.RECUSADA);
                Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

                registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.RECUSADA, usuarioLogado);

                return mapToSolicitacaoResponse(solicitacaoSalva);
            }

            @Transactional
            public SolicitacaoResponse criarRascunho(SolicitacaoRequest request, Usuario usuarioLogado) {

                validarDatas(request.dataPrevisaoEntrega(), request.dataPrevisaoDevolucao());

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
                registrarHistoricoDeStatus(rascunhoSalvo, null, StatusSolicitacao.RASCUNHO, usuarioLogado);

                return mapToSolicitacaoResponse(rascunhoSalvo);
            }

            @Transactional
            public SolicitacaoResponse enviarRascunho(Long id, Usuario usuarioLogado) {
                // 1. Busca a solicitação que está em modo rascunho
                Solicitacao rascunho = solicitacaoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Rascunho de solicitação não encontrado com o ID: " + id));

                // 2. REGRA DE NEGÓCIO: Só podemos enviar solicitações com status RASCUNHO.
                if (rascunho.getStatus() != StatusSolicitacao.RASCUNHO) {
                    throw new BusinessException("Apenas solicitações com status RASCUNHO podem ser enviadas. Status atual: " + rascunho.getStatus());
                }

                // Validação de itens (Stock e Ativo) - O seu código disto já estava correto
                List<String> errosDeValidacao = new ArrayList<>();
                for (SolicitacaoItem item : rascunho.getItens()) {
                    Equipamento equipamento = item.getEquipamento();
                    if (!equipamento.isAtivo()) {
                        errosDeValidacao.add("O equipamento '" + equipamento.getNome() + "' está INATIVO.");
                    }
                    if (equipamento.getQuantidadeDisponivel() < item.getQuantidadeSolicitada()) {
                        errosDeValidacao.add("Stock insuficiente para '" + equipamento.getNome() + "' (Disponível: " + equipamento.getQuantidadeDisponivel() + ").");
                    }
                }

                if (!errosDeValidacao.isEmpty()) {
                    String mensagemCompleta = "A solicitação não pode ser enviada pelos seguintes motivos:\n- " +
                            String.join("\n- ", errosDeValidacao);
                    throw new BusinessException(mensagemCompleta);
                }

                // 1. Valida a DATA
                validarDatas(rascunho.getDataPrevisaoEntrega(), rascunho.getDataPrevisaoDevolucao());

                // --- NOVA LÓGICA DE STATUS (BASEADA EM CARGO E SETOR) ---
                StatusSolicitacao statusNovo;
                Usuario criadorDoRascunho = rascunho.getUsuario();
                String cargoUsuario = criadorDoRascunho.getCargo().getNome();

                // SE for Gestor ou Admin, pula a primeira etapa
                if ("GESTOR".equals(cargoUsuario) || "ADMIN".equals(cargoUsuario)) {
                    statusNovo = StatusSolicitacao.PENDENTE_ADMIN;
                } else {
                    // SE for Colaborador, verifica se o seu setor tem gestores
                    Setor setorDoUsuario = criadorDoRascunho.getSetor();
                    if (setorDoUsuario != null &&
                            usuarioRepository.existsBySetorAndCargoNome(setorDoUsuario, "GESTOR")) {
                        statusNovo = StatusSolicitacao.PENDENTE_GESTOR;
                    } else {
                        statusNovo = StatusSolicitacao.PENDENTE_ADMIN;
                    }
                }

                // 4. Altera o status
                StatusSolicitacao statusAnterior = rascunho.getStatus();
                rascunho.setStatus(statusNovo); // <-- MUDANÇA AQUI

                // 5. Salva a solicitação
                Solicitacao solicitacaoEnviada = solicitacaoRepository.save(rascunho);

                // 6. Regista a mudança de status no histórico
                // O responsável é o utilizador logado que clicou em "enviar"
                registrarHistoricoDeStatus(solicitacaoEnviada, statusAnterior, statusNovo, usuarioLogado); // <-- MUDANÇA AQUI

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

                validarDatas(request.dataPrevisaoEntrega(), request.dataPrevisaoDevolucao());

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
                                                    StatusSolicitacao statusNovo,
                                                    Usuario usuarioResponsavel) { // <-- PARÂMETRO ADICIONADO
                HistoricoStatusSolicitacao registo = HistoricoStatusSolicitacao.builder()
                        .dataAlteracao(LocalDateTime.now())
                        .solicitacao(solicitacao)
                        .statusAnterior(statusAnterior)
                        .statusNovo(statusNovo)
                        .usuarioResponsavel(usuarioResponsavel) // <-- CORRIGIDO
                        .build();
                historicoStatusRepository.save(registo);
            }

            // MÉTODO para a visão de Admin/Gestor
            @Transactional(readOnly = true)
            public Page<SolicitacaoResponse> listarTodasSolicitacoes(
                    Usuario usuarioLogado, // Recebe o objeto Usuario
                    Optional<Long> usuarioId,
                    List<StatusSolicitacao> statuses,
                    Optional<LocalDate> dataInicio,
                    Optional<LocalDate> dataFim,
                    Boolean devolucaoIndeterminada,
                    Pageable pageable) {

                LocalDateTime inicio = dataInicio.map(LocalDate::atStartOfDay).orElse(null);
                LocalDateTime fim = dataFim.map(d -> d.atTime(23, 59, 59)).orElse(null);

                String cargo = usuarioLogado.getCargo().getNome();
                Page<Solicitacao> solicitacoes;

                if ("ADMIN".equals(cargo)) {
                    // 1. ADMIN: Vê tudo (usando a query de Admin)
                    solicitacoes = solicitacaoRepository.findAdminView(
                            usuarioLogado.getId(),
                            usuarioId.orElse(null),
                            statuses,
                            inicio,
                            fim,
                            devolucaoIndeterminada,
                            pageable
                    );
                } else if ("GESTOR".equals(cargo)) {
                    // 2. GESTOR: Vê apenas o seu setor (usando a nova query)
                    Setor setorDoGestor = usuarioLogado.getSetor();
                    if (setorDoGestor == null) {
                        // Se um gestor não tem setor (ou o setor foi removido), ele não vê nada
                        return Page.empty();
                    }

                // A chamada agora inclui o ID do utilizador, correspondendo à nova assinatura
                    solicitacoes = solicitacaoRepository.findGestorView( // <-- CHAMA A NOVA QUERY
                            usuarioLogado.getId(),
                            setorDoGestor, // <-- Filtra pelo setor do gestor
                            usuarioId.orElse(null),
                            statuses,
                            inicio,
                            fim,
                            devolucaoIndeterminada,
                            pageable
                    );
                } else {
                    // 3. COLABORADOR: Não deve usar este endpoint, retorna vazio.
                    solicitacoes = Page.empty();
                }

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
            public SolicitacaoResponse devolverTudo(Long solicitacaoId, Usuario usuarioLogado) {
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
                registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.FINALIZADA,usuarioLogado);

                return mapToSolicitacaoResponse(solicitacaoSalva);
            }

            @Transactional
            public SolicitacaoResponse cancelarSolicitacao(Long id, Usuario usuarioLogado) {
                Solicitacao solicitacao = solicitacaoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

                // Lógica de permissão (Admin pode cancelar tudo, outros só o seu)
                if (!usuarioLogado.getCargo().getNome().equals("ADMIN")) {
                    if (!solicitacao.getUsuario().getId().equals(usuarioLogado.getId())) {
                        throw new BusinessException("Você não tem permissão para cancelar uma solicitação que não é sua.");
                    }
                }

                // REGRA DE NEGÓCIO: Pode cancelar em qualquer etapa PENDENTE
                if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE_GESTOR &&
                        solicitacao.getStatus() != StatusSolicitacao.PENDENTE_ADMIN) {
                    throw new BusinessException("Apenas solicitações pendentes podem ser canceladas. Status atual: " + solicitacao.getStatus());
                }

                StatusSolicitacao statusAnterior = solicitacao.getStatus();
                solicitacao.setStatus(StatusSolicitacao.CANCELADA);
                Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

                registrarHistoricoDeStatus(solicitacaoSalva, statusAnterior, StatusSolicitacao.CANCELADA, usuarioLogado);

                return mapToSolicitacaoResponse(solicitacaoSalva);
            }

            @Transactional(readOnly = true)
            public long contarSolicitacoesPendentes(Usuario usuarioLogado) {
                String cargo = usuarioLogado.getCargo().getNome();

                if ("ADMIN".equals(cargo)) {
                    // Admin vê todas as PENDENTE_ADMIN
                    return solicitacaoRepository.countByStatusIn(List.of(StatusSolicitacao.PENDENTE_ADMIN));

                } else if ("GESTOR".equals(cargo)) {
                    // Gestor vê todas as PENDENTE_GESTOR do seu setor
                    Setor setorDoGestor = usuarioLogado.getSetor();
                    if (setorDoGestor == null) {
                        return 0; // Se o gestor não tem setor, ele não vê nada
                    }
                    return solicitacaoRepository.countByStatusAndUsuarioSetor(
                            StatusSolicitacao.PENDENTE_GESTOR,
                            setorDoGestor
                    );


                }

                return 0;
            }

            @Transactional
            public SolicitacaoResponse atualizarSolicitacaoPendente(Long id, SolicitacaoUpdateRequest request) {
                // 1. Busca a solicitação
                Solicitacao solicitacao = solicitacaoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com o ID: " + id));

                // 2. O ESCUDO PROTETOR: Garante que só se pode editar solicitações pendentes.
                if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE_GESTOR &&
                        solicitacao.getStatus() != StatusSolicitacao.PENDENTE_ADMIN) {
                    throw new BusinessException("Apenas solicitações com status PENDENTE podem ser editadas.");
                }

                    // 3. Verifica se a nova lista de itens está vazia
                    if (request.itens() == null || request.itens().isEmpty()) {
                        throw new BusinessException("Não é possível atualizar uma solicitação para não ter nenhum item.");
                    }
                    // ===================================

                    // 4. Limpa os itens antigos
                    solicitacao.getItens().clear();

                    // 5. Adiciona os novos itens
                    processarEAgruparItens(request.itens(), solicitacao);

                    // 6. Atualiza a justificação e salva.
                    solicitacao.setJustificativa(request.justificativa());
                    solicitacaoRepository.save(solicitacao);

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
                    List<StatusSolicitacao> statuses,
                    Optional<LocalDate> dataInicio,
                    Optional<LocalDate> dataFim,
                    Boolean devolucaoIndeterminada,
                    Pageable pageable) {

                LocalDateTime inicio = dataInicio.map(LocalDate::atStartOfDay).orElse(null);
                LocalDateTime fim = dataFim.map(d -> d.atTime(23, 59, 59)).orElse(null);

                // Agora chama a nova query, que foi feita especificamente para esta visão
                Page<Solicitacao> solicitacoes = solicitacaoRepository.findMyView(
                        usuarioLogado.getId(),
                        statuses,
                        inicio,
                        fim,
                        devolucaoIndeterminada,
                        pageable
                );

                return solicitacoes.map(this::mapToSolicitacaoResponse);
            }
            private void validarDatas(LocalDate entrega, LocalDate devolucao) {
                if (entrega == null) {
                    throw new BusinessException("A data de previsão de entrega é obrigatória.");
                }
                // Se a devolução for informada (não for 'indeterminada'), valida a ordem.
                if (devolucao != null && devolucao.isBefore(entrega)) {
                    throw new BusinessException("A data de devolução não pode ser anterior à data de entrega.");
                }
            }
        }