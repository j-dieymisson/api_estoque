// consulta-solicitacoes-lista.js - Lógica para a página de resultados de solicitações de um funcionário
setTimeout(() => {
    (async function() {
        console.log("A executar o script da lista de resultados de solicitações...");

        // Lê os parâmetros passados pelo 'navigateTo'
        const usuarioId = window.pageContext?.usuarioId;
        const dataInicio = window.pageContext?.dataInicio;
        const dataFim = window.pageContext?.dataFim;

        if (!usuarioId) {
            document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">ID do funcionário não fornecido.</div>';
            return;
        }

        // --- Seletores ---
        const cabecalhoUsuario = document.getElementById('resultado-cabecalho-usuario');
        const subcabecalhoPeriodo = document.getElementById('resultado-subcabecalho-periodo');
        const corpoTabela = document.getElementById('corpo-tabela-relatorio-solicitacoes');
        const paginacaoContainer = document.getElementById('paginacao-relatorio-solicitacoes');
        const btnVoltar = document.getElementById('btn-voltar');

        // --- Funções ---
        async function carregarDados(page = 0) {
            corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center">A carregar...</td></tr>`;
            const params = {
                page, size: 10, sort: 'dataSolicitacao,desc',
                usuarioId: usuarioId,
                dataInicio: dataInicio || null,
                dataFim: dataFim || null
            };
            Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);

            try {
                // Fazemos as duas chamadas em paralelo para otimizar
                const [userRes, solRes] = await Promise.all([
                    apiClient.get(`/usuarios/${usuarioId}`),
                    apiClient.get('/solicitacoes', { params })
                ]);

                const usuario = userRes.data;
                const pageData = solRes.data;

                // Atualiza o cabeçalho
                cabecalhoUsuario.textContent = `Relatório de Solicitações para: ${usuario.nome}`;
                if (dataInicio && dataFim) {
                    subcabecalhoPeriodo.textContent = `Período: ${new Date(dataInicio+'T00:00:00').toLocaleDateString('pt-BR')} a ${new Date(dataFim+'T00:00:00').toLocaleDateString('pt-BR')}`;
                }

                // Renderiza a tabela
                corpoTabela.innerHTML = '';
                if (pageData.content.length === 0) {
                    corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center">Nenhuma solicitação encontrada para este funcionário no período.</td></tr>`;
                } else {
                    pageData.content.forEach(sol => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${sol.id}</td>
                            <td>${new Date(sol.dataSolicitacao).toLocaleDateString('pt-BR')}</td>
                            <td><span class="badge ${getBadgeClassForStatus(sol.status)}">${sol.status}</span></td>
                            <td><button class="btn btn-sm btn-outline-info btn-detalhes-solicitacao" data-id="${sol.id}">Ver Detalhes</button></td>
                        `;
                        corpoTabela.appendChild(tr);
                    });
                }
                renderizarPaginacao(pageData);

            } catch (error) {
                console.error("Erro ao gerar relatório de solicitações:", error);
                corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center text-danger">Falha ao carregar relatório.</td></tr>`;
            }
        }

        function getBadgeClassForStatus(status) {
                    const map = {
                        'PENDENTE_GESTOR': 'bg-warning text-dark',
                        'PENDENTE_ADMIN': 'bg-warning text-dark',
                        'APROVADA': 'bg-success',
                        'FINALIZADA': 'bg-secondary',
                        'RECUSADA': 'bg-danger',
                        'CANCELADA': 'bg-danger',
                        'RASCUNHO': 'bg-info text-dark'
                    };
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

        // --- Event Listeners ---
        if(btnVoltar) {
               btnVoltar.addEventListener('click', () => window.navigateBack());
            }

        if (paginacaoContainer) paginacaoContainer.addEventListener('click', (event) => {
            const link = event.target.closest('a.page-link');
            if (link && !link.parentElement.classList.contains('disabled')) {
                event.preventDefault();
                carregarDados(parseInt(link.dataset.page));
            }
        });

        if (corpoTabela) corpoTabela.addEventListener('click', (event) => {
            const target = event.target.closest('.btn-detalhes-solicitacao');
            if (target) {
                const id = target.dataset.id;
                window.navigateTo('solicitacao-detalhe.html', { id: id });
            }
        });

        async function carregarDados(page = 0) {
                    corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center">A carregar...</td></tr>`;

                    // --- LER OS NOVOS PARÂMETROS ---
                    const statuses = window.pageContext?.statuses;
                    const devolucaoIndeterminada = window.pageContext?.devolucaoIndeterminada;
                    // --- FIM ---

                    const params = {
                        page, size: 10, sort: 'dataSolicitacao,desc',
                        usuarioId: usuarioId,
                        dataInicio: dataInicio || null,
                        dataFim: dataFim || null,

                        // --- ADICIONAR OS NOVOS PARÂMETROS ---
                        statuses: statuses || null,
                        devolucaoIndeterminada: devolucaoIndeterminada ? true : null
                        // --- FIM ---
                    };
                    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);

                    try {
                        // Fazemos as duas chamadas em paralelo para otimizar
                        const [userRes, solRes] = await Promise.all([
                            apiClient.get(`/usuarios/${usuarioId}`),
                            apiClient.get('/solicitacoes', { params })
                        ]);

                        const usuario = userRes.data;
                        const pageData = solRes.data;

                        // Atualiza o cabeçalho
                        cabecalhoUsuario.textContent = `Relatório de Solicitações para: ${usuario.nome}`;
                        if (dataInicio && dataFim) {
                            subcabecalhoPeriodo.textContent = `Período: ${new Date(dataInicio+'T00:00:00').toLocaleDateString('pt-BR')} a ${new Date(dataFim+'T00:00:00').toLocaleDateString('pt-BR')}`;
                        }

                        // Renderiza a tabela
                        corpoTabela.innerHTML = '';
                        if (pageData.content.length === 0) {
                            corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center">Nenhuma solicitação encontrada para este funcionário no período.</td></tr>`;
                        } else {
                            pageData.content.forEach(sol => {
                                const tr = document.createElement('tr');

                                // LÓGICA DE TRADUÇÃO (que alterámos no passo 2)
                                let statusVisual = sol.status;
                                if (statusVisual === 'PENDENTE_GESTOR' || statusVisual === 'PENDENTE_ADMIN') {
                                    statusVisual = 'Pendente';
                                }

                                tr.innerHTML = `
                                    <td>${sol.id}</td>
                                    <td>${new Date(sol.dataSolicitacao).toLocaleDateString('pt-BR')}</td>
                                    <td><span class="badge ${getBadgeClassForStatus(sol.status)}">${statusVisual}</span></td>
                                    <td><button class="btn btn-sm btn-outline-info btn-detalhes-solicitacao" data-id="${sol.id}">Ver Detalhes</button></td>
                                `;
                                corpoTabela.appendChild(tr);
                            });
                        }
                        renderizarPaginacao(pageData);

                    } catch (error) {
                        console.error("Erro ao gerar relatório de solicitações:", error);
                        corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center text-danger">Falha ao carregar relatório.</td></tr>`;
                    }
                }

    })();
}, 0);