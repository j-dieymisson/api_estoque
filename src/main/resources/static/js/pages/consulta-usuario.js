// consulta-usuario.js - Versão refatorada com tabela paginada

(function() {
    console.log("A executar o script da página de consulta de utilizador...");

    const usuarioId = window.pageContext?.id;

    if (!usuarioId) {
        document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID do funcionário não fornecido.</div>';
        return;
    }

    // --- Seletores ---
    const cabecalhoNome = document.getElementById('usuario-nome-cabecalho');
    const corpoTabela = document.getElementById('corpo-tabela-solicitacoes-usuario');
    const paginacaoContainer = document.getElementById('paginacao-solicitacoes-usuario');
    const btnVoltar = document.getElementById('btn-voltar-consulta-usr');

    const detalheId = document.getElementById('detalhe-usr-id');
    const detalheNome = document.getElementById('detalhe-usr-nome');
    const detalheEmail = document.getElementById('detalhe-usr-email');
    const detalheCargo = document.getElementById('detalhe-usr-cargo');


    function renderizarTabela(solicitacoes) {
        corpoTabela.innerHTML = '';
        if (!solicitacoes || solicitacoes.length === 0) {
            corpoTabela.innerHTML = '<tr><td colspan="4" class="text-center">Nenhuma solicitação encontrada para este funcionário.</td></tr>';
            return;
        }

        solicitacoes.forEach(sol => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${sol.id}</td>
                <td>${sol.justificativa || 'N/A'}</td>
                <td>${new Date(sol.dataSolicitacao).toLocaleDateString('pt-BR')}</td>
                <td><span class="badge ${getBadgeClassForStatus(sol.status)}">${sol.status}</span></td>
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

    function getBadgeClassForStatus(status) {
        const statusClassMap = {
            'PENDENTE': 'bg-warning text-dark', 'APROVADA': 'bg-success',
            'FINALIZADA': 'bg-secondary', 'RECUSADA': 'bg-danger',
            'CANCELADA': 'bg-danger', 'RASCUNHO': 'bg-info text-dark'
        };
        return statusClassMap[status] || 'bg-primary';
    }

    async function carregarDadosUsuario(page = 0) {
        // Mostra o loading apenas na tabela, o cabeçalho pode esperar
        corpoTabela.innerHTML = '<tr><td colspan="4" class="text-center">A carregar...</td></tr>';
        try {
            const [userRes, solRes] = await Promise.all([
                apiClient.get(`/usuarios/${usuarioId}`),
                apiClient.get('/solicitacoes', { params: { usuarioId: usuarioId, page: page, size: 10, sort: 'dataSolicitacao,desc' } })
            ]);

            const usuario = userRes.data;
            const pageData = solRes.data;

            // Preenche o cabeçalho e o card de detalhes
            cabecalhoNome.textContent = `Consulta: ${usuario.nome}`;
            detalheId.textContent = usuario.id;
            detalheNome.textContent = usuario.nome;
            detalheEmail.textContent = usuario.email;
            detalheCargo.textContent = usuario.nomeCargo;

            // Renderiza a tabela e a paginação
            renderizarTabela(pageData.content);
            renderizarPaginacao(pageData);

        } catch (error) {
            console.error("Erro ao carregar dados do funcionário:", error);
            showToast('Não foi possível carregar os dados.', 'Erro', true);
            cabecalhoNome.textContent = 'Erro ao Carregar';
            corpoTabela.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Falha ao carregar dados.</td></tr>';
        }
    }

    // --- Event Listeners ---
    btnVoltar.addEventListener('click', () => window.navigateTo('consultas.html'));

    paginacaoContainer.addEventListener('click', (event) => {
        const link = event.target.closest('a.page-link');
        if (link && !link.parentElement.classList.contains('disabled')) {
            event.preventDefault();
            carregarDadosUsuario(parseInt(link.dataset.page));
        }
    });

    // --- Inicialização ---
    carregarDadosUsuario(0);

})();