// solicitacao-detalhe.js - Lógica para a página de detalhe de uma solicitação

(async function() {
    console.log("A executar o script da página de detalhe da solicitação...");

    // O ID da solicitação é passado através do objeto global 'pageContext' pelo nosso router
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

    // --- Funções ---

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

    // Função para buscar e renderizar os dados da solicitação
    async function carregarDetalhes() {
        try {
            const [solicitacaoRes, perfilRes] = await Promise.all([
                apiClient.get(`/solicitacoes/${solicitacaoId}`),
                apiClient.get('/perfil')
            ]);

            const solicitacao = solicitacaoRes.data;
            const usuarioLogado = perfilRes.data;

            // Preenche as informações gerais
            detalheIdSpan.textContent = solicitacao.id;
            solicitanteSpan.textContent = solicitacao.nomeUsuario;
            dataSolicitacaoSpan.textContent = new Date(solicitacao.dataSolicitacao).toLocaleString('pt-BR');
            dataEntregaSpan.textContent = new Date(solicitacao.dataPrevisaoEntrega).toLocaleDateString('pt-BR');
            dataDevolucaoSpan.textContent = new Date(solicitacao.dataPrevisaoDevolucao).toLocaleDateString('pt-BR');
            justificativaP.textContent = solicitacao.justificativa;

            // Atualiza o badge de status com a cor correta
            statusBadge.textContent = solicitacao.status;
            statusBadge.className = `badge ${getBadgeClassForStatus(solicitacao.status)}`;

            // Preenche a tabela de itens
            corpoTabela.innerHTML = '';
            solicitacao.itens.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.nomeEquipamento}</td>
                    <td>${item.quantidadeSolicitada}</td>
                    <td>${item.quantidadeDevolvida}</td>
                    <td>${item.quantidadePendente}</td>
                    <td>
                        ${solicitacao.status === 'APROVADA' && item.quantidadePendente > 0 ?
                            `<button class="btn btn-sm btn-outline-primary btn-devolver-item" data-item-id="${item.id}" title="Devolver este item">Devolver</button>` : ''
                        }
                    </td>
                `;
                corpoTabela.appendChild(tr);
            });

            // Controla a visibilidade dos botões de ação
            controlarVisibilidadeAcoes(solicitacao, usuarioLogado);

        } catch (error) {
            console.error("Erro ao carregar detalhes da solicitação:", error);
            showToast('Não foi possível carregar os detalhes da solicitação.', 'Erro', true);
        }
    }

    // Função para decidir quais botões de ação mostrar
    function controlarVisibilidadeAcoes(solicitacao, usuarioLogado) {
        // Esconde todos os botões por padrão
        acoesContainer.querySelectorAll('button').forEach(btn => btn.classList.add('d-none'));

        const cargo = usuarioLogado.nomeCargo;
        const status = solicitacao.status;

        if (status === 'PENDENTE') {
            if (cargo === 'ADMIN') {
                document.getElementById('btn-aprovar').classList.remove('d-none');
                document.getElementById('btn-recusar').classList.remove('d-none');
            }
            // Assumindo que o utilizador só pode cancelar os seus próprios pedidos
            if (usuarioLogado.nome === solicitacao.nomeUsuario) {
                 document.getElementById('btn-cancelar').classList.remove('d-none');
            }
        }

        if (status === 'APROVADA') {
            // Verifica se ainda há itens pendentes de devolução
            const algumPendente = solicitacao.itens.some(item => item.quantidadePendente > 0);
            if (algumPendente) {
                document.getElementById('btn-devolver-tudo').classList.remove('d-none');
            }
        }
    }

    // Função auxiliar para obter a cor do badge (copiada do solicitacoes.js)
    function getBadgeClassForStatus(status) { /* ... (cole aqui a mesma função que já temos no solicitacoes.js) ... */ }


    // --- Event Listeners ---
    btnVoltar.addEventListener('click', () => {
        // Usa a nossa função global para voltar para a página da lista
        window.navigateTo('solicitacoes.html');
    });

    // --- Inicialização ---
    carregarDetalhes();

})();