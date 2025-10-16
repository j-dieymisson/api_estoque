// consulta-equipamentos-lista.js - Lógica para a página de resultados do relatório de equipamentos
setTimeout(() => {
    (async function() {
        console.log("A executar o script da lista de resultados de equipamentos...");

        // Lê os parâmetros de data passados pelo 'navigateTo'
        const dataInicio = window.pageContext?.dataInicioCriacao;
        const dataFim = window.pageContext?.dataFimCriacao;

        if (!dataInicio || !dataFim) {
            document.getElementById('main-content-area').innerHTML = '<div class="alert alert-danger">Período de datas não fornecido.</div>';
            return;
        }

        // --- Seletores ---
        const cabecalhoResultado = document.getElementById('resultado-cabecalho');
        const corpoTabela = document.getElementById('corpo-tabela-relatorio-equipamentos');
        const paginacaoContainer = document.getElementById('paginacao-relatorio-equipamentos');
        const btnVoltar = document.getElementById('btn-voltar');

        // --- Funções ---
        async function carregarDados(page = 0) {
            corpoTabela.innerHTML = `<tr><td colspan="5" class="text-center">A carregar...</td></tr>`;
            const params = {
                page, size: 10, sort: 'dataCriacao,desc',
                dataInicioCriacao: dataInicio,
                dataFimCriacao: dataFim
            };

            try {
                const response = await apiClient.get('/equipamentos', { params });
                const pageData = response.data;

                // Atualiza o cabeçalho com o período da busca
                cabecalhoResultado.textContent = `Período da busca: ${new Date(dataInicio+'T00:00:00').toLocaleDateString('pt-BR')} a ${new Date(dataFim+'T00:00:00').toLocaleDateString('pt-BR')}`;

                corpoTabela.innerHTML = '';
                if (pageData.content.length === 0) {
                    corpoTabela.innerHTML = `<tr><td colspan="5" class="text-center">Nenhum equipamento criado neste período.</td></tr>`;
                } else {
                    pageData.content.forEach(eq => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${eq.id}</td>
                            <td>${eq.nome}</td>
                            <td>${eq.nomeCategoria}</td>
                            <td>${new Date(eq.dataCriacao).toLocaleDateString('pt-BR')}</td>
                            <td><button class="btn btn-sm btn-outline-info btn-ver-historico" data-id="${eq.id}">Ver Histórico</button></td>
                        `;
                        corpoTabela.appendChild(tr);
                    });
                }
                renderizarPaginacao(pageData);
            } catch (error) {
                corpoTabela.innerHTML = `<tr><td colspan="5" class="text-center text-danger">Falha ao carregar relatório.</td></tr>`;
            }
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
            const target = event.target.closest('.btn-ver-historico');
            if (target) {
                const id = target.dataset.id;
                window.navigateTo('consulta-equipamento.html', { id: id });
            }
        });

        // --- Inicialização ---
        carregarDados(0);

    })();
}, 0);