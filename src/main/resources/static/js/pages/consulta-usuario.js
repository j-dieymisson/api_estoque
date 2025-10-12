// consulta-usuario.js - Lógica para a página de resultados de solicitações de um funcionário

(function() {
    console.log("A executar o script da página de consulta de utilizador...");

    // O ID do utilizador é passado através do objeto global 'window.pageContext'
    const usuarioId = window.pageContext?.id;

    if (!usuarioId) {
        document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID do funcionário não fornecido.</div>';
        return;
    }

    // --- Seletores ---
    const cabecalhoNome = document.getElementById('usuario-nome-cabecalho');
    const listaContainer = document.getElementById('lista-solicitacoes-usuario');
    const paginacaoContainer = document.getElementById('paginacao-solicitacoes-usuario');
    const btnVoltar = document.getElementById('btn-voltar-consulta-usr');

    // --- Funções de Renderização ---
    function renderizarLista(solicitacoes) {
        listaContainer.innerHTML = '';
        if (!solicitacoes || solicitacoes.length === 0) {
            listaContainer.innerHTML = '<p class="text-info">Nenhuma solicitação encontrada para este funcionário.</p>';
            return;
        }

        let listHtml = '';
        solicitacoes.forEach(sol => {
            listHtml += `
                <a href="#" class="list-group-item list-group-item-action link-solicitacao" data-id="${sol.id}">
                    <div class="d-flex w-100 justify-content-between">
                        <h5 class="mb-1">Solicitação #${sol.id}</h5>
                        <small>${new Date(sol.dataSolicitacao).toLocaleDateString('pt-BR')}</small>
                    </div>
                    <p class="mb-1">${sol.justificativa || 'Sem justificativa.'}</p>
                    <small>Status: ${sol.status}</small>
                </a>`;
        });
        listaContainer.innerHTML = listHtml;
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
    async function carregarDadosUsuario(page = 0) {
        listaContainer.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"></div>';
        try {
            // Buscamos o nome do utilizador e as suas solicitações em paralelo
            const [userRes, solRes] = await Promise.all([
                apiClient.get(`/usuarios/${usuarioId}`),
                apiClient.get('/solicitacoes', { params: { usuarioId: usuarioId, page: page, size: 5, sort: 'dataSolicitacao,desc' } })
            ]);

            const usuario = userRes.data;
            const pageData = solRes.data;

            cabecalhoNome.textContent = `Solicitações de: ${usuario.nome}`;

            renderizarLista(pageData.content);
            renderizarPaginacao(pageData);

        } catch (error) {
            console.error("Erro ao carregar dados do funcionário:", error);
            showToast('Não foi possível carregar os dados.', 'Erro', true);
            cabecalhoNome.textContent = 'Erro ao Carregar';
            listaContainer.innerHTML = '<p class="text-danger">Falha ao carregar dados.</p>';
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
            carregarDadosUsuario(parseInt(link.dataset.page));
        }
    });

    listaContainer.addEventListener('click', function(event){
        const link = event.target.closest('a.link-solicitacao');
        if(link){
            event.preventDefault();
            const id = link.dataset.id;
            window.navigateTo('solicitacao-detalhe.html', { id });
        }
    });

    // --- Inicialização ---
    carregarDadosUsuario(0);

})();