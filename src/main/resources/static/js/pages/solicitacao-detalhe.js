// solicitacao-detalhe.js - Lógica para a página de detalhe de uma solicitação

(async function() {
    console.log("A executar o script da página de detalhe da solicitação...");

    // O ID da solicitação é passado através do objeto global 'window.pageContext' pelo nosso router
    const solicitacaoId = window.pageContext?.id;

    if (!solicitacaoId) {
        document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID da solicitação não encontrado. Por favor, volte à lista.</div>';
        return;
    }

    // --- Seletores de Elementos ---
    const detalheIdSpan = document.getElementById('detalhe-id');
    const statusBadge = document.getElementById('detalhe-status-badge');
    const solicitanteSpan = document.getElementById('detalhe-solicitante');
    const dataSolicitacaoSpan = document.getElementById('detalhe-data-solicitacao');
    const dataEntregaSpan = document.getElementById('detalhe-data-entrega');
    const dataDevolucaoSpan = document.getElementById('detalhe-data-devolucao');
    const justificativaP = document.getElementById('detalhe-justificativa');
    const corpoTabela = document.getElementById('corpo-tabela-itens-detalhe');
    const acoesContainer = document.getElementById('acoes-solicitacao');
    const btnVoltar = document.getElementById('btn-voltar-lista');

    // Seletores do Modal de Devolução
    const modalDevolucaoEl = document.getElementById('modal-devolucao');
    const modalDevolucao = new bootstrap.Modal(modalDevolucaoEl);
    const formDevolucao = document.getElementById('form-devolucao');
    const devolucaoItemIdInput = document.getElementById('devolucao-item-id');
    const devolucaoNomeEquipamentoSpan = document.getElementById('devolucao-nome-equipamento');
    const devolucaoQtdPendenteSpan = document.getElementById('devolucao-qtd-pendente');
    const devolucaoQtdInput = document.getElementById('devolucao-qtd');
    const devolucaoObsTextarea = document.getElementById('devolucao-obs');

    // Guarda os dados da solicitação para fácil acesso
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

            // Preenche informações gerais
            detalheIdSpan.textContent = solicitacaoAtual.id;
            solicitanteSpan.textContent = solicitacaoAtual.nomeUsuario;
            dataSolicitacaoSpan.textContent = new Date(solicitacaoAtual.dataSolicitacao).toLocaleString('pt-BR');
            dataEntregaSpan.textContent = new Date(solicitacaoAtual.dataPrevisaoEntrega).toLocaleDateString('pt-BR');
            dataDevolucaoSpan.textContent = new Date(solicitacaoAtual.dataPrevisaoDevolucao).toLocaleDateString('pt-BR');
            justificativaP.textContent = solicitacaoAtual.justificativa;

            // Atualiza badge de status
            statusBadge.textContent = solicitacaoAtual.status;
            statusBadge.className = `badge ${getBadgeClassForStatus(solicitacaoAtual.status)}`;

            // Preenche tabela de itens
            corpoTabela.innerHTML = '';
            solicitacaoAtual.itens.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
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

        if (status === 'PENDENTE') {
            if (cargo === 'ADMIN') {
                document.getElementById('btn-aprovar').classList.remove('d-none');
                document.getElementById('btn-recusar').classList.remove('d-none');
            }
            if (usuarioLogado.nome === solicitacao.nomeUsuario) {
                 document.getElementById('btn-cancelar').classList.remove('d-none');
            }
        }
        if (status === 'APROVADA') {
            const algumPendente = solicitacao.itens.some(item => item.quantidadePendente > 0);
            if (algumPendente) {
                document.getElementById('btn-devolver-tudo').classList.remove('d-none');
            }
        }
    }

    function getBadgeClassForStatus(status) {
        switch (status) {
            case 'PENDENTE': return 'bg-warning text-dark';
            case 'APROVADA': return 'bg-success';
            case 'FINALIZADA': return 'bg-secondary';
            case 'RECUSADA': case 'CANCELADA': return 'bg-danger';
            case 'RASCUNHO': return 'bg-info text-dark';
            default: return 'bg-primary';
        }
    }

    // --- Event Listeners ---
    btnVoltar.addEventListener('click', () => window.navigateTo('solicitacoes.html'));

    acoesContainer.addEventListener('click', async (event) => {
        const target = event.target.closest('button');
        if (!target) return;
        const id = target.id;

        const acaoPatch = async (endpoint) => {
            try {
                await apiClient.patch(endpoint);
                showToast('Ação executada com sucesso!', 'Sucesso');
                await carregarDetalhes();
            } catch (error) {
                console.error(`Erro na ação ${id}:`, error);
                showToast(error.response?.data?.message || 'Erro ao executar a ação.', 'Erro', true);
            }
        };

        const acaoPost = async (endpoint) => {
            try {
                await apiClient.post(endpoint);
                showToast('Ação executada com sucesso!', 'Sucesso');
                await carregarDetalhes();
            } catch (error) {
                console.error(`Erro na ação ${id}:`, error);
                showToast(error.response?.data?.message || 'Erro ao executar a ação.', 'Erro', true);
            }
        };

        switch (id) {
            case 'btn-aprovar':
                showConfirmModal('Aprovar Solicitação', `Tem a certeza?`, () => acaoPatch(`/solicitacoes/${solicitacaoId}/aprovar`));
                break;
            case 'btn-recusar':
                showConfirmModal('Recusar Solicitação', `Tem a certeza?`, () => acaoPatch(`/solicitacoes/${solicitacaoId}/recusar`));
                break;
            case 'btn-cancelar':
                showConfirmModal('Cancelar Solicitação', `Tem a certeza?`, () => acaoPatch(`/solicitacoes/${solicitacaoId}/cancelar`));
                break;
            case 'btn-devolver-tudo':
                showConfirmModal('Devolver Todos os Itens', `Tem a certeza?`, () => acaoPost(`/solicitacoes/${solicitacaoId}/devolver-tudo`));
                break;
        }
    });

    corpoTabela.addEventListener('click', function(event) {
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

    formDevolucao.addEventListener('submit', async function(event) {
        event.preventDefault();
        const data = {
            solicitacaoItemId: parseInt(devolucaoItemIdInput.value, 10),
            quantidadeDevolvida: parseInt(devolucaoQtdInput.value, 10),
            observacao: devolucaoObsTextarea.value
        };

        if (data.quantidadeDevolvida <= 0 || data.quantidadeDevolvida > parseInt(devolucaoQtdInput.max, 10)) {
            showToast(`A quantidade a devolver deve ser entre 1 e ${devolucaoQtdInput.max}.`, 'Erro de Validação', true);
            return;
        }

        try {
            await apiClient.post('/devolucoes', data);
            showToast('Devolução registada com sucesso!', 'Sucesso');
            modalDevolucao.hide();
            await carregarDetalhes();
        } catch (error) {
            console.error("Erro ao registar devolução:", error);
            showToast(error.response?.data?.message || 'Não foi possível registar a devolução.', 'Erro', true);
        }
    });

    // --- Inicialização ---
    carregarDetalhes();

})();