// solicitacoes.js - Lógica específica para a página "Minhas Solicitações"

(async function() {
    console.log("A executar o script da página de solicitações...");

    const corpoTabela = document.getElementById('corpo-tabela-solicitacoes');
    const filtroStatusSelect = document.getElementById('filtro-status');
    const filtroDataInicio = document.getElementById('filtro-data-inicio');
    const filtroDataFim = document.getElementById('filtro-data-fim');
    const formFiltros = document.getElementById('form-filtros-solicitacoes');
    const btnLimparFiltros = document.getElementById('btn-limpar-filtros');

    if (!corpoTabela || !formFiltros) {
        console.error("Elementos essenciais da página de solicitações não foram encontrados!");
        return;
    }

    // Função para obter a classe de cor do badge com base no status
        function getBadgeClassForStatus(status) {
            switch (status) {
                case 'PENDENTE':
                    return 'bg-warning text-dark'; // Usamos text-dark para melhor contraste em fundo amarelo
                case 'APROVADA':
                    return 'bg-success';
                case 'FINALIZADA':
                    return 'bg-secondary';
                case 'RECUSADA':
                case 'CANCELADA':
                    return 'bg-danger';
                case 'RASCUNHO':
                    return 'bg-info text-dark';
                default:
                    return 'bg-primary';
            }
        }
    // Função para buscar e renderizar as solicitações na tabela
    async function carregarSolicitacoes(params = {}) {
            corpoTabela.innerHTML = '<tr><td colspan="5" class="text-center"><div class="spinner-border spinner-border-sm" role="status"></div></td></tr>';
            try {
                const response = await apiClient.get('/solicitacoes/minhas', { params });
                const solicitacoes = response.data.content;
                corpoTabela.innerHTML = '';

                if (solicitacoes.length === 0) {
                    corpoTabela.innerHTML = '<tr><td colspan="5" class="text-center">Nenhuma solicitação encontrada.</td></tr>';
                    return;
                }

                solicitacoes.forEach(sol => {
                    const tr = document.createElement('tr');
                    let acoesHtml = `
                        <button class="btn btn-sm btn-info btn-detalhes" data-id="${sol.id}" title="Ver Detalhes">
                            <i class="bi bi-eye-fill"></i>
                        </button>
                    `;

                    // Adiciona o botão de cancelar APENAS se o status for PENDENTE
                    if (sol.status === 'PENDENTE') {
                        acoesHtml += `
                            <button class="btn btn-sm btn-danger btn-cancelar ms-1" data-id="${sol.id}" title="Cancelar Solicitação">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        `;
                    }

                    tr.innerHTML = `
                        <td>${sol.id}</td>
                        <td>${sol.justificativa}</td>
                        <td>${new Date(sol.dataSolicitacao).toLocaleDateString('pt-BR')}</td>
                        <td><span class="badge ${getBadgeClassForStatus(sol.status)}">${sol.status}</span></td>
                        <td>${acoesHtml}</td>
                    `;
                    corpoTabela.appendChild(tr);
                });
            } catch (error) {
                console.error("Erro ao carregar solicitações:", error);
                corpoTabela.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Falha ao carregar solicitações.</td></tr>';
            }
        }

    // Função para preencher o <select> de status
    async function carregarFiltroStatus() {
        try {
            const response = await apiClient.get('/solicitacoes/status');
            const statusList = response.data;

            filtroStatusSelect.innerHTML = '<option value="">Todos</option>'; // Opção padrão
            statusList.forEach(status => {
                const option = document.createElement('option');
                option.value = status;
                option.textContent = status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
                filtroStatusSelect.appendChild(option);
            });
        } catch (error) {
            console.error("Erro ao carregar os status para o filtro:", error);
        }
    }

    // --- Event Listeners ---

    // Quando o formulário de filtros é submetido
    formFiltros.addEventListener('submit', function(event) {
        event.preventDefault();
        const params = {
            status: filtroStatusSelect.value || null,
            dataInicio: filtroDataInicio.value || null,
            dataFim: filtroDataFim.value || null,
            // Adicionamos page e size para futuras implementações de paginação
            page: 0,
            size: 10
        };
        // Remove parâmetros nulos para não os enviar na URL
        Object.keys(params).forEach(key => params[key] == null && delete params[key]);

        carregarSolicitacoes(params);
    });

    // Quando o botão de limpar é clicado
    btnLimparFiltros.addEventListener('click', function() {
        formFiltros.reset();
        carregarSolicitacoes(); // Carrega a lista sem filtros
    });

    // Botões de ação na tabela
    corpoTabela.addEventListener('click', function(event) {
            const target = event.target.closest('button');
            if (!target) return;

            const solicitacaoId = target.dataset.id;

            if (target.classList.contains('btn-cancelar')) {
                // Usa a nossa nova função de modal!
                showConfirmModal(
                    'Confirmar Cancelamento',
                    `Tem a certeza que quer cancelar a solicitação ID ${solicitacaoId}?`,
                    async () => { // Esta é a função que será executada se o utilizador confirmar
                        try {
                            await apiClient.patch(`/solicitacoes/${solicitacaoId}/cancelar`);
                            showToast('Solicitação cancelada com sucesso!', 'Sucesso');
                            carregarSolicitacoes(); // Atualiza a tabela
                        } catch (error) {
                            console.error("Erro ao cancelar solicitação:", error);
                            showToast(error.response?.data?.message || 'Não foi possível cancelar a solicitação.', 'Erro', true);
                        }
                    }
                );
            }

            if (target.classList.contains('btn-detalhes')) {
                // No futuro, aqui chamaremos a função para carregar a página de detalhes
                console.log(`Abrir detalhes da solicitação ${solicitacaoId}`);
            }
        });


    // --- Inicialização ---
    carregarFiltroStatus(); // Carrega as opções do filtro
    carregarSolicitacoes(); // Carrega a lista inicial de solicitações

})();