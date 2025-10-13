(function() {
    const equipamentoId = window.pageContext?.id;
    if (!equipamentoId) {
        document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID do equipamento não fornecido.</div>';
        return;
    }
    const cabecalhoNome = document.getElementById('equipamento-nome-cabecalho');
    const corpoTabela = document.getElementById('corpo-tabela-hist-equipamento');
    const paginacaoContainer = document.getElementById('paginacao-hist-equipamento');
    const btnVoltar = document.getElementById('btn-voltar-consulta-eq');
    const detalheNome = document.getElementById('detalhe-eq-nome');
    const detalheCategoria = document.getElementById('detalhe-eq-categoria');
    const detalheQtdTotal = document.getElementById('detalhe-eq-qtd-total');
    const detalheQtdDisp = document.getElementById('detalhe-eq-qtd-disp');

    function renderizarTabela(movimentacoes) {
        corpoTabela.innerHTML = '';
        if (!movimentacoes || movimentacoes.length === 0) {
            corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center">Nenhum registo encontrado.</td></tr>';
            return;
        }
        movimentacoes.forEach(h => {
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${new Date(h.dataMovimentacao).toLocaleString('pt-BR')}</td><td>${h.tipoMovimentacao}</td><td>${h.quantidade}</td><td>${h.quantidadeAnterior}</td><td>${h.quantidadePosterior}</td><td>${h.usuarioResponsavel}</td><td>${h.solicitacaoId || 'N/A'}</td>`;
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

    async function carregarDados(page = 0) {
        corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center">A carregar...</td></tr>';
        cabecalhoNome.textContent = `Consulta para Equipamento ID: ${equipamentoId}`;
        try {
            const historicoRes = await apiClient.get(`/historico/equipamento/${equipamentoId}`, { params: { page, size: 10, sort: 'dataMovimentacao,desc' } });
            const pageData = historicoRes.data;
            renderizarTabela(pageData.content);
            renderizarPaginacao(pageData);
            try {
                const equipamentoRes = await apiClient.get(`/equipamentos/${equipamentoId}`);
                const equipamento = equipamentoRes.data;
                cabecalhoNome.textContent = `Consulta: ${equipamento.nome}`;
                detalheNome.textContent = equipamento.nome;
                detalheCategoria.textContent = equipamento.nomeCategoria;
                detalheQtdTotal.textContent = equipamento.quantidadeTotal;
                detalheQtdDisp.textContent = equipamento.quantidadeDisponivel;
            } catch (detailError) {
                console.warn("Não foi possível buscar os detalhes do equipamento.");
            }
        } catch (mainError) {
            showToast('Não foi possível carregar o histórico.', 'Erro', true);
            cabecalhoNome.textContent = 'Erro ao Carregar';
            corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Falha ao carregar dados.</td></tr>';
        }
    }

    btnVoltar.addEventListener('click', () => window.navigateTo('consultas.html'));
    paginacaoContainer.addEventListener('click', (event) => {
        const link = event.target.closest('a.page-link');
        if (link && !link.parentElement.classList.contains('disabled')) {
            event.preventDefault();
            carregarDados(parseInt(link.dataset.page));
        }
    });
    carregarDados(0);
})();