// solicitacoes.js - Versão final, corrigida e simplificada
setTimeout(() => {
    (async function() {
        console.log("A executar o script da página de solicitações...");

        // --- Seletores e Variáveis ---
        const corpoTabela = document.getElementById('corpo-tabela-solicitacoes');
        const paginacaoContainer = document.getElementById('paginacao-solicitacoes');
        const formFiltros = document.getElementById('form-filtros-solicitacoes');
        const filtroStatusSelect = document.getElementById('filtro-status');
        const filtroDataInicio = document.getElementById('filtro-data-inicio');
        const filtroDataFim = document.getElementById('filtro-data-fim');
        const btnLimparFiltros = document.getElementById('btn-limpar-filtros');
        const btnNovaSolicitacao = document.getElementById('btn-nova-solicitacao');
        let currentUserRole = null;
        let currentPage = 0;

        // --- Funções Auxiliares ---
        function getBadgeClassForStatus(status) {
            const map = { 'PENDENTE': 'bg-warning text-dark', 'APROVADA': 'bg-success', 'FINALIZADA': 'bg-secondary', 'RECUSADA': 'bg-danger', 'CANCELADA': 'bg-danger', 'RASCUNHO': 'bg-info text-dark' };
            return map[status] || 'bg-primary';
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

        function renderizarTabela(pageData) {
            const solicitacoes = pageData.content;
            const isAdminOuGestor = currentUserRole === 'ADMIN' || currentUserRole === 'GESTOR';
            const colspan = isAdminOuGestor ? 5 : 4;
            corpoTabela.innerHTML = '';

            if (!solicitacoes || solicitacoes.length === 0) {
                corpoTabela.innerHTML = `<tr><td colspan="${colspan}" class="text-center">Nenhuma solicitação encontrada.</td></tr>`;
                return;
            }

            solicitacoes.forEach(sol => {
                const tr = document.createElement('tr');
                // LÓGICA SIMPLIFICADA: Apenas o botão de detalhes, como você pediu
                const acoesHtml = `<button class="btn btn-sm btn-outline-primary btn-detalhes" data-id="${sol.id}" title="Ver Detalhes"><i class="bi bi-eye-fill"></i></button>`;
                const solicitanteHtml = isAdminOuGestor ? `<td>${sol.nomeUsuario}</td>` : '';

                tr.innerHTML = `
                    <td>${sol.id}</td>
                    ${solicitanteHtml}
                    <td>${new Date(sol.dataSolicitacao).toLocaleDateString('pt-BR')}</td>
                    <td><span class="badge ${getBadgeClassForStatus(sol.status)}">${sol.status}</span></td>
                    <td>${acoesHtml}</td>
                `;
                corpoTabela.appendChild(tr);
            });
            renderizarPaginacao(pageData);
        }

        // --- Funções Principais ---
        async function carregarSolicitacoes(page = 0) {
            currentPage = page;
            const isAdminOuGestor = currentUserRole === 'ADMIN' || currentUserRole === 'GESTOR';
            const colspan = isAdminOuGestor ? 6 : 5;
            corpoTabela.innerHTML = `<tr><td colspan="${colspan}" class="text-center">A carregar...</td></tr>`;

            const endpoint = isAdminOuGestor ? '/solicitacoes' : '/solicitacoes/minhas';

            const params = {
                page, size: 5, sort: 'dataSolicitacao,desc',
                status: filtroStatusSelect.value || null,
                dataInicio: filtroDataInicio.value || null,
                dataFim: filtroDataFim.value || null,
            };
            Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);

            try {
                const response = await apiClient.get(endpoint, { params });
                renderizarTabela(response.data);
            } catch (error) {
                console.error("Erro ao carregar solicitações:", error);
                corpoTabela.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-danger">Falha ao carregar.</td></tr>`;
            }
        }

        async function carregarFiltroStatus() {
            try {
                const response = await apiClient.get('/solicitacoes/status');
                filtroStatusSelect.innerHTML = '<option value="">Todos</option>';
                response.data.forEach(status => {
                    const option = new Option(status.charAt(0).toUpperCase() + status.slice(1).toLowerCase(), status);
                    filtroStatusSelect.appendChild(option);
                });
            } catch (error) { console.error("Erro ao carregar os status para o filtro:", error); }
        }

        // --- Inicialização e Event Listeners ---
        async function init() {
            try {
                const response = await apiClient.get('/perfil');
                currentUserRole = response.data.nomeCargo;

                const thSolicitante = document.querySelector('#tabela-solicitacoes th.admin-only');
                            if (currentUserRole === 'ADMIN' || currentUserRole === 'GESTOR') {
                                if (thSolicitante) thSolicitante.style.display = 'table-cell'; // Mostra a coluna
                            } else {
                                if (thSolicitante) thSolicitante.style.display = 'none'; // Garante que está escondida
                            }


                await carregarFiltroStatus();
                await carregarSolicitacoes(0);
            } catch(e) { console.error("Erro na inicialização da página de solicitações", e); }
        }

        if (formFiltros) formFiltros.addEventListener('submit', (e) => { e.preventDefault(); carregarSolicitacoes(0); });
        if (btnLimparFiltros) btnLimparFiltros.addEventListener('click', () => { formFiltros.reset(); carregarSolicitacoes(0); });
        if (btnNovaSolicitacao) btnNovaSolicitacao.addEventListener('click', () => window.navigateTo('solicitacao-form.html'));

        if (corpoTabela) corpoTabela.addEventListener('click', function(event) {
            const target = event.target.closest('button.btn-detalhes');
            if (target) {
                const solicitacaoId = target.dataset.id;
                window.navigateTo('solicitacao-detalhe.html', { id: solicitacaoId });
            }
        });

        if (paginacaoContainer) paginacaoContainer.addEventListener('click', (event) => {
            const link = event.target.closest('a.page-link');
            if (link && !link.parentElement.classList.contains('disabled')) {
                event.preventDefault();
                carregarSolicitacoes(parseInt(link.dataset.page));
            }
        });

        init();
    })();
}, 0);