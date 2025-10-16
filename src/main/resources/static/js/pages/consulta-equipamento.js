// consulta-equipamento.js - Versão refatorada e corrigida

(function() {
    console.log("A executar o script da página de consulta de equipamento...");

    // O ID é passado pelo nosso router no main.js
    const equipamentoId = window.pageContext?.id;

    if (!equipamentoId) {
        document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID do equipamento não fornecido. Volte e tente novamente.</div>';
        return;
    }

    // --- Seletores de Elementos ---
    const cabecalhoNome = document.getElementById('equipamento-nome-cabecalho');
    const corpoTabela = document.getElementById('corpo-tabela-hist-equipamento');
    const paginacaoContainer = document.getElementById('paginacao-hist-equipamento');
    const btnVoltar = document.getElementById('btn-voltar');
    const detalheNome = document.getElementById('detalhe-eq-nome');
    const detalheCategoria = document.getElementById('detalhe-eq-categoria');
    const detalheQtdTotal = document.getElementById('detalhe-eq-qtd-total');
    const detalheQtdDisp = document.getElementById('detalhe-eq-qtd-disp');

    // --- Funções de Renderização ---
    function renderizarTabela(movimentacoes) {
        corpoTabela.innerHTML = '';
        if (!movimentacoes || movimentacoes.length === 0) {
            corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center">Nenhum registo de movimentação encontrado para este equipamento.</td></tr>';
            return;
        }
        movimentacoes.forEach(h => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${new Date(h.dataMovimentacao).toLocaleString('pt-BR')}</td>
                <td>${h.tipoMovimentacao}</td>
                <td>${h.quantidade}</td>
                <td>${h.quantidadeAnterior}</td>
                <td>${h.quantidadePosterior}</td>
                <td>${h.usuarioResponsavel}</td>
                <td>${h.solicitacaoId || 'N/A'}</td>
            `;
            corpoTabela.appendChild(tr);
        });
    }

    function renderizarPaginacao(pageData) {
        paginacaoContainer.innerHTML = '';
        if (!pageData || pageData.totalPages <= 1) return;
        let html = '<ul class="pagination pagination-sm justify-content-center">';
        html += `<li class="page-item ${pageData.first ? 'disabled' : ''}"><a class="page-link" href="#" data-page="${pageData.number - 1}">Anterior</a></li>`;
        html += `<li class="page-item disabled"><span class="page-link">Página ${pageData.number + 1} de ${pageData.totalPages}</span></li>`;
        html += `<li class="page-item ${pageData.last ? 'disabled' : ''}"><a class="page-link" href="#" data-page="${pageData.number + 1}">Próximo</a></li>`;
        html += '</ul>';
        paginacaoContainer.innerHTML = html;
    }

    // --- Lógica Principal ---
    async function carregarDados(page = 0) {
        corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center">A carregar histórico...</td></tr>';
        cabecalhoNome.textContent = `Consulta para Equipamento ID: ${equipamentoId}`;

        try {
            // 1. Busca o histórico (informação principal)
            const historicoRes = await apiClient.get(`/historico/equipamento/${equipamentoId}`, {
                params: { page, size: 10, sort: 'dataMovimentacao,desc' }
            });
            const pageData = historicoRes.data;
            renderizarTabela(pageData.content);
            renderizarPaginacao(pageData);

            // 2. Tenta buscar os detalhes do equipamento para enriquecer a página
            try {
                const equipamentoRes = await apiClient.get(`/equipamentos/${equipamentoId}`);
                const equipamento = equipamentoRes.data;
                cabecalhoNome.textContent = `Consulta: ${equipamento.nome}`;
                detalheNome.textContent = equipamento.nome;
                detalheCategoria.textContent = equipamento.nomeCategoria;
                detalheQtdTotal.textContent = equipamento.quantidadeTotal;
                detalheQtdDisp.textContent = equipamento.quantidadeDisponivel;
            } catch (detailError) {
                console.warn("Não foi possível buscar os detalhes do equipamento (pode ser falta de permissão), mas o histórico foi carregado.");
            }
        } catch (mainError) {
            console.error("Erro ao carregar histórico do equipamento:", mainError);
            showToast('Não foi possível carregar o histórico.', 'Erro', true);
            cabecalhoNome.textContent = 'Erro ao Carregar Histórico';
            corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Falha ao carregar dados. Verifique o ID ou as suas permissões.</td></tr>';
        }
    }

    // --- Event Listeners ---
    if(btnVoltar) {
       btnVoltar.addEventListener('click', () => window.navigateBack());
    }

    if(paginacaoContainer) {
        paginacaoContainer.addEventListener('click', (event) => {
            const link = event.target.closest('a.page-link');
            if (link && !link.parentElement.classList.contains('disabled')) {
                event.preventDefault();
                carregarDados(parseInt(link.dataset.page));
            }
        });
    }

    // --- Inicialização ---
    carregarDados(0); // Chama a função principal para carregar os dados
})();