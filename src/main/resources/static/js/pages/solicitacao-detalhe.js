// solicitacao-detalhe.js - Versão final com "botões inteligentes"
setTimeout(() => {
    (async function() {
        console.log("A executar o script da página de detalhe da solicitação...");

        const solicitacaoId = window.pageContext?.id;

        if (!solicitacaoId) {
            document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID da solicitação não encontrado.</div>';
            return;
        }

        // --- Seletores ---
        const detalheIdSpan = document.getElementById('detalhe-id');
        const statusBadge = document.getElementById('detalhe-status-badge');
        const solicitanteSpan = document.getElementById('detalhe-solicitante');
        const dataSolicitacaoSpan = document.getElementById('detalhe-data-solicitacao');
        const dataEntregaSpan = document.getElementById('detalhe-data-entrega');
        const dataDevolucaoSpan = document.getElementById('detalhe-data-devolucao');
        const justificativaP = document.getElementById('detalhe-justificativa');
        const corpoTabela = document.getElementById('corpo-tabela-itens-detalhe');
        const acoesContainer = document.getElementById('acoes-solicitacao');
       const btnVoltar = document.getElementById('btn-voltar');
        const btnImprimir = document.getElementById('btn-imprimir-solicitacao');

        const modalDevolucaoEl = document.getElementById('modal-devolucao');
        const modalDevolucao = new bootstrap.Modal(modalDevolucaoEl);
        const formDevolucao = document.getElementById('form-devolucao');
        const devolucaoItemIdInput = document.getElementById('devolucao-item-id');
        const devolucaoNomeEquipamentoSpan = document.getElementById('devolucao-nome-equipamento');
        const devolucaoQtdPendenteSpan = document.getElementById('devolucao-qtd-pendente');
        const devolucaoQtdInput = document.getElementById('devolucao-qtd');
        const devolucaoObsTextarea = document.getElementById('devolucao-obs');

        let solicitacaoAtual = null;

        // --- Funções ---
        async function carregarDetalhes() {
            try {
                const [solicitacaoRes, perfilRes] = await Promise.all([
                    apiClient.get(`/solicitacoes/${solicitacaoId}`),
                    apiClient.get('/perfil')
                ]);

                solicitacaoAtual = solicitacaoRes.data;
                const usuarioLogado = perfilRes.data;

                detalheIdSpan.textContent = solicitacaoAtual.id;
                solicitanteSpan.textContent = solicitacaoAtual.nomeUsuario;
                dataSolicitacaoSpan.textContent = new Date(solicitacaoAtual.dataSolicitacao).toLocaleString('pt-BR');
                dataEntregaSpan.textContent = solicitacaoAtual.dataPrevisaoEntrega ? new Date(solicitacaoAtual.dataPrevisaoEntrega).toLocaleDateString('pt-BR') : 'N/A';
                dataDevolucaoSpan.innerHTML = solicitacaoAtual.dataPrevisaoDevolucao
                    ? new Date(solicitacaoAtual.dataPrevisaoDevolucao).toLocaleDateString('pt-BR')
                    : '<span class="text-muted fst-italic">Indeterminada</span>';
                justificativaP.textContent = solicitacaoAtual.justificativa;

                let statusVisual = solicitacaoAtual.status;
                                let badgeClass = getBadgeClassForStatus(solicitacaoAtual.status); // Pega a cor correta

                                if (solicitacaoAtual.status === 'PENDENTE_GESTOR') {
                                    statusVisual = 'Pendente <span class="badge bg-light text-dark ms-1">0/2</span>';
                                } else if (solicitacaoAtual.status === 'PENDENTE_ADMIN') {
                                    statusVisual = 'Pendente <span class="badge bg-light text-dark ms-1">1/2</span>';
                                }

                                statusBadge.innerHTML = statusVisual; // Usamos innerHTML para o contador
                                statusBadge.className = `badge ${badgeClass}`;

                corpoTabela.innerHTML = '';
                solicitacaoAtual.itens.forEach(item => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${item.equipamentoId}</td>
                        <td>${item.nomeEquipamento}</td>
                        <td>${item.quantidadeSolicitada}</td>
                        <td>${item.quantidadeDevolvida}</td>
                        <td>${item.quantidadePendente}</td>
                        <td>
                            ${solicitacaoAtual.status === 'APROVADA' && item.quantidadePendente > 0 ?
                                `<button class="btn btn-sm btn-outline-primary btn-devolver-item" data-item-id="${item.id}" title="Devolver este item">Devolver</button>` : ''
                            }
                        </td>
                    `;
                    corpoTabela.appendChild(tr);
                });

                controlarVisibilidadeAcoes(solicitacaoAtual, usuarioLogado);

            } catch (error) {
                console.error("Erro ao carregar detalhes da solicitação:", error);
                showToast('Não foi possível carregar os detalhes da solicitação.', 'Erro', true);
            }
        }

        function controlarVisibilidadeAcoes(solicitacao, usuarioLogado) {
                    acoesContainer.querySelectorAll('button').forEach(btn => btn.classList.add('d-none'));
                    const cargo = usuarioLogado.nomeCargo;
                    const status = solicitacao.status;

                    if (status === 'RASCUNHO') {
                        document.getElementById('btn-editar-rascunho-detalhe').classList.remove('d-none');
                        document.getElementById('btn-enviar-rascunho-detalhe').classList.remove('d-none');
                        document.getElementById('btn-apagar-rascunho-detalhe').classList.remove('d-none');

                    } else if (status === 'PENDENTE_GESTOR') {
                        // Nível 1: Apenas o Gestor pode aprovar/recusar
                        if (cargo === 'GESTOR') {
                            document.getElementById('btn-aprovar').classList.remove('d-none');
                            document.getElementById('btn-recusar').classList.remove('d-none');
                        }
                        // O dono pode cancelar
                        if (usuarioLogado.nome === solicitacao.nomeUsuario) {
                             document.getElementById('btn-cancelar').classList.remove('d-none');
                        }

                    } else if (status === 'PENDENTE_ADMIN') {
                        // Nível 2: Apenas o Admin pode aprovar/recusar
                         if (cargo === 'ADMIN') {
                            document.getElementById('btn-aprovar').classList.remove('d-none');
                            document.getElementById('btn-recusar').classList.remove('d-none');
                        }
                        // O dono ainda pode cancelar
                        if (usuarioLogado.nome === solicitacao.nomeUsuario) {
                             document.getElementById('btn-cancelar').classList.remove('d-none');
                        }

                    } else if (status === 'APROVADA') {
                        const algumPendente = solicitacao.itens.some(item => item.quantidadePendente > 0);
                        if (algumPendente) {
                            document.getElementById('btn-devolver-tudo').classList.remove('d-none');
                        }
                    }
                }

        function getBadgeClassForStatus(status) {
                    const map = {
                        'PENDENTE_GESTOR': 'bg-warning text-dark', // NOVO
                        'PENDENTE_ADMIN': 'bg-warning text-dark', // NOVO
                        'APROVADA': 'bg-success',
                        'FINALIZADA': 'bg-secondary',
                        'RECUSADA': 'bg-danger',
                        'CANCELADA': 'bg-danger',
                        'RASCUNHO': 'bg-info text-dark'
                    };
                    return map[status] || 'bg-primary';
                }

        // --- Event Listeners ---
        if (btnVoltar) btnVoltar.addEventListener('click', () => window.navigateBack());

        if (acoesContainer) {
            acoesContainer.addEventListener('click', async (event) => {
                const target = event.target.closest('button');
                if (!target) return;
                const id = solicitacaoId;

                const acaoPatch = async (endpoint) => {
                    try {
                        await apiClient.patch(endpoint);
                        showToast('Ação executada com sucesso!', 'Sucesso');
                        await carregarDetalhes();
                    } catch (error) { showToast(error.response?.data?.message || 'Erro ao executar a ação.', 'Erro', true); }
                };

                const acaoPost = async (endpoint) => {
                    try {
                        await apiClient.post(endpoint);
                        showToast('Ação executada com sucesso!', 'Sucesso');
                        await carregarDetalhes();
                        window.atualizarNotificacaoPendentes();
                    } catch (error) { showToast(error.response?.data?.message || 'Erro ao executar a ação.', 'Erro', true); }
                };

                switch (target.id) {
                    case 'btn-aprovar':
                        // Lógica inteligente: verifica o status atual para decidir qual endpoint chamar
                        if (solicitacaoAtual.status === 'PENDENTE_GESTOR') {
                            showConfirmModal('Aprovar (Nível 1/2 - Gestor)', `Tem a certeza?`, () => acaoPatch(`/solicitacoes/${id}/aprovar-gestor`));
                        } else if (solicitacaoAtual.status === 'PENDENTE_ADMIN') {
                            showConfirmModal('Aprovar (Nível 2/2 - Admin)', `Tem a certeza?`, () => acaoPatch(`/solicitacoes/${id}/aprovar-admin`));
                        }
                        break;
                    case 'btn-recusar':
                        showConfirmModal('Recusar Solicitação', `Esta ação é irreversível. Deseja continuar?`, async () => {

                            // 1. Pede o motivo ao utilizador
                            const motivo = window.prompt("Por favor, insira o motivo da recusa:");

                            // 2. Se o utilizador cancelou ou deixou vazio, para.
                            if (motivo === null) return; // Clicou em Cancelar
                            if (motivo.trim() === "") {
                                showToast("O motivo da recusa é obrigatório.", "Erro", true);
                                return;
                            }

                            // 3. Chama a API enviando o motivo no corpo (Body)
                            try {
                                await apiClient.patch(`/solicitacoes/${id}/recusar`, { motivo: motivo });
                                showToast('Solicitação recusada.', 'Sucesso');
                                await carregarDetalhes();
                            } catch (error) {
                                showToast(error.response?.data?.message || 'Erro ao recusar.', 'Erro', true);
                            }
                        });
                        break;
                    case 'btn-cancelar':
                        showConfirmModal('Cancelar Solicitação', `Tem a certeza?`, () => acaoPatch(`/solicitacoes/${id}/cancelar`));
                        break;
                    case 'btn-devolver-tudo':
                        showConfirmModal('Devolver Todos os Itens', `Tem a certeza?`, () => acaoPost(`/solicitacoes/${id}/devolver-tudo`));
                        break;
                    case 'btn-editar-rascunho-detalhe':
                        window.navigateTo('solicitacao-form.html', { rascunhoId: id });
                        break;
                    case 'btn-enviar-rascunho-detalhe':
                        showConfirmModal('Enviar Solicitação', 'O rascunho será convertido numa solicitação pendente. Deseja continuar?', () => acaoPatch(`/rascunhos/${id}/enviar`));
                        break;
                    case 'btn-apagar-rascunho-detalhe':
                        showConfirmModal('Apagar Rascunho', 'Tem a certeza?', async () => {
                            try {
                                await apiClient.delete(`/rascunhos/${id}`);
                                showToast('Rascunho apagado!', 'Sucesso');
                                window.navigateTo('solicitacoes.html');
                            } catch (error) { showToast(error.response?.data?.message || 'Não foi possível apagar.', 'Erro', true); }
                        });
                        break;
                }
            });
        }

        if (corpoTabela) corpoTabela.addEventListener('click', function(event) {
            const target = event.target.closest('button.btn-devolver-item');
            if (!target) return;
            const itemId = parseInt(target.dataset.itemId, 10);
            const item = solicitacaoAtual.itens.find(i => i.id === itemId);

            if (item) {
                formDevolucao.reset();
                devolucaoItemIdInput.value = item.id;
                devolucaoNomeEquipamentoSpan.textContent = item.nomeEquipamento;
                devolucaoQtdPendenteSpan.textContent = item.quantidadePendente;
                devolucaoQtdInput.max = item.quantidadePendente;
                devolucaoQtdInput.value = item.quantidadePendente;
                modalDevolucao.show();
            }
        });

        if (formDevolucao) formDevolucao.addEventListener('submit', async function(event) {
            event.preventDefault();
            const data = {
                solicitacaoItemId: parseInt(devolucaoItemIdInput.value, 10),
                quantidadeDevolvida: parseInt(devolucaoQtdInput.value, 10),
                observacao: devolucaoObsTextarea.value
            };

            if (data.quantidadeDevolvida <= 0 || data.quantidadeDevolvida > parseInt(devolucaoQtdInput.max, 10)) {
                showToast(`A quantidade a devolver deve ser entre 1 e ${devolucaoQtdInput.max}.`, 'Erro', true);
                return;
            }
            try {
                await apiClient.post('/devolucoes', data);
                showToast('Devolução registada com sucesso!', 'Sucesso');
                modalDevolucao.hide();
                await carregarDetalhes();
            } catch (error) { showToast(error.response?.data?.message || 'Não foi possível registar a devolução.', 'Erro', true); }
        });

        if (btnImprimir) {
                btnImprimir.addEventListener('click', async () => {
                    showToast('A gerar o seu PDF...', 'Aguarde');
                    try {
                        // 1. Chama a API pedindo o PDF. É crucial usar 'blob' como responseType.
                        const response = await apiClient.get(`/solicitacoes/${solicitacaoId}/pdf`, {
                            responseType: 'blob'
                        });

                        // 2. Cria um URL temporário para o ficheiro recebido
                        const url = window.URL.createObjectURL(new Blob([response.data]));

                        // 3. Cria um link invisível para acionar o download
                        const link = document.createElement('a');
                        link.href = url;
                        link.setAttribute('download', `solicitacao_${solicitacaoId}.pdf`); // Nome do ficheiro

                        // 4. Adiciona o link à página, clica nele e remove-o
                        document.body.appendChild(link);
                        link.click();
                        link.remove();

                        window.URL.revokeObjectURL(url); // Liberta a memória

                    } catch (error) {
                        console.error("Erro ao gerar o PDF:", error);
                        showToast('Não foi possível gerar o PDF.', 'Erro', true);
                    }
                });
            }

        // --- Inicialização ---
        carregarDetalhes();

    })();
}, 0);