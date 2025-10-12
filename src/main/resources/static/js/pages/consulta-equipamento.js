// consulta-equipamento.js - Lógica para a página de resultados de histórico de equipamento

(function() {
    console.log("A executar o script da página de consulta de equipamento...");

    // O ID do equipamento é passado através do objeto global 'window.pageContext'
    const equipamentoId = window.pageContext?.id;

    if (!equipamentoId) {
        document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID do equipamento não fornecido.</div>';
        return;
    }

    // --- Seletores ---
    const cabecalhoNome = document.getElementById('equipamento-nome-cabecalho');
    const corpoTabela = document.getElementById('corpo-tabela-hist-equipamento');
    const paginacaoContainer = document.getElementById('paginacao-hist-equipamento');
    const btnVoltar = document.getElementById('btn-voltar-consulta-eq');

    // --- Funções de Renderização ---
    function renderizarTabela(movimentacoes) {
        corpoTabela.innerHTML = '';
        if (!movimentacoes || movimentacoes.length === 0) {
            corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center">Nenhum registo encontrado.</td></tr>';
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
    async function carregarHistorico(page = 0) {
        corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center">A carregar...</td></tr>';
        try {
            // Buscamos o nome do equipamento e o seu histórico em paralelo
            const [equipamentoRes, historicoRes] = await Promise.all([
                apiClient.get(`/equipamentos/${equipamentoId}`),
                apiClient.get(`/historico/equipamento/${equipamentoId}`, { params: { page, size: 10 } })
            ]);

            const equipamento = equipamentoRes.data;
            const pageData = historicoRes.data;

            // Atualiza o cabeçalho da página
            cabecalhoNome.textContent = `Histórico para: ${equipamento.nome}`;

            // Renderiza a tabela e a paginação
            renderizarTabela(pageData.content);
            renderizarPaginacao(pageData);

        } catch (error) {
            console.error("Erro ao carregar histórico do equipamento:", error);
            showToast('Não foi possível carregar o histórico.', 'Erro', true);
            cabecalhoNome.textContent = 'Erro ao Carregar Histórico';
            corpoTabela.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Falha ao carregar dados.</td></tr>';
        }
    }

    // --- Event Listeners ---
    btnVoltar.addEventListener('click', () => {
        window.navigateTo('consultas.html');
    });

    paginacaoContainer.addEventListener('click', (event) => {
        const link = event.target.closest('a.page-link');
        if (link && !link.parentElement.classList.contains('disabled')) {
            event.preventDefault();
            carregarHistorico(parseInt(link.dataset.page));
        }
    });

    // --- Inicialização ---
    carregarHistorico(0); // Carrega a primeira página do histórico ao iniciar

})();