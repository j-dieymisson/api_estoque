// equipamentos.js - Lógica para a página de listagem e busca de equipamentos
setTimeout(() => {
    (async function() {
        console.log("A executar o script da página de equipamentos...");

        // --- Seletores e Variáveis ---
        const corpoTabela = document.getElementById('corpo-tabela-equipamentos');
        const paginacaoContainer = document.getElementById('paginacao-equipamentos');
        const formFiltros = document.getElementById('form-filtros-equipamentos');
        const btnLimparFiltros = document.getElementById('btn-limpar-filtros-equipamentos');
        const btnAdicionarEquipamento = document.getElementById('btn-adicionar-equipamento');
        const filtroId = document.getElementById('filtro-id');
        const filtroNome = document.getElementById('filtro-nome');
        const filtroCategoria = document.getElementById('filtro-categoria');
        let currentUserRole = null;
        let currentPage = 0;

        // --- Funções de Busca e Renderização ---
        async function carregarEquipamentos(page = 0) {
                    currentPage = page;
                    const colspan = (currentUserRole === 'ADMIN' || currentUserRole === 'GESTOR') ? 6 : 5;
                    corpoTabela.innerHTML = `<tr><td colspan="${colspan}" class="text-center">A carregar...</td></tr>`;

                    const params = {
                        page, size: 10, sort: 'nome,asc',
                        id: filtroId.value || null,
                        nome: filtroNome.value || null,
                        categoriaId: filtroCategoria.value || null,
                    };
                    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);

                    try {
                        // A chamada agora é sempre para o mesmo endpoint. A "magia" acontece no back-end.
                        const response = await apiClient.get('/equipamentos', { params });
                        renderizarTabelaEquipamentos(response.data.content);
                        renderizarPaginacao(response.data);
                    } catch (error) {
                        console.error("Erro ao carregar equipamentos:", error);
                        corpoTabela.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-danger">Falha ao carregar equipamentos.</td></tr>`;
                    }
                }

        async function carregarCategorias() {
            try {
                const response = await apiClient.get('/categorias');
                filtroCategoria.innerHTML = '<option value="">Todas</option>';
                response.data.forEach(cat => {
                    if (cat.ativa)  filtroCategoria.appendChild(new Option(cat.nome, cat.id));
                });
            } catch (error) { console.error("Erro ao carregar categorias:", error); }
        }

        function renderizarTabelaEquipamentos(equipamentos) {
            corpoTabela.innerHTML = '';
            const isAdminOuGestor = currentUserRole === 'ADMIN'|| currentUserRole === 'GESTOR';
            const colspan = isAdminOuGestor ? 6 : 5;
            if (!equipamentos || equipamentos.length === 0) {
                corpoTabela.innerHTML = `<tr><td colspan="${colspan}" class="text-center">Nenhum equipamento encontrado.</td></tr>`;
                return;
            }
            equipamentos.forEach(eq => {
                const tr = document.createElement('tr');
                let acoesHtml = '';
                if (isAdminOuGestor) {
                 const botaoEditar = `<button class="btn btn-sm btn-outline-primary btn-editar" data-id="${eq.id}" title="Editar/Ajustar Stock"><i class="bi bi-pencil-fill"></i></button>`;
                    const botaoAtivarDesativar = eq.ativo
                                    ? `<button class="btn btn-sm btn-outline-secondary btn-desativar ms-1" data-id="${eq.id}" title="Desativar"><i class="bi bi-toggle-off"></i></button>`
                                    : `<button class="btn btn-sm btn-outline-success btn-ativar ms-1" data-id="${eq.id}" title="Ativar"><i class="bi bi-toggle-on"></i></button>`;

                                acoesHtml = `<td>${botaoEditar} ${botaoAtivarDesativar}</td>`;
                            }

                                     tr.innerHTML = `
                                         <td>${eq.id}</td>
                                         <td>${eq.nome}</td>
                                         <td>${eq.nomeCategoria}</td>
                                         <td>${eq.quantidadeTotal}</td>
                                         <td>${eq.quantidadeDisponivel}</td>
                                         ${acoesHtml}
                                     `;
                                     corpoTabela.appendChild(tr);
                                 });
                             }

        function renderizarPaginacao(pageData) {
                    paginacaoContainer.innerHTML = ''; // Limpa a paginação antiga

                    // Se houver apenas uma página (ou nenhuma), não mostra nada
                    if (!pageData || pageData.totalPages <= 1) {
                        return;
                    }

                    let html = '<ul class="pagination pagination-sm justify-content-center">';

                    // Cria o botão "Anterior" e desativa-o se for a primeira página
                    html += `<li class="page-item ${pageData.first ? 'disabled' : ''}">
                                <a class="page-link" href="#" data-page="${pageData.number - 1}">Anterior</a>
                             </li>`;

                    // Mostra o status atual da página (ex: "Página 1 de 5")
                    html += `<li class="page-item disabled">
                                <span class="page-link">Página ${pageData.number + 1} de ${pageData.totalPages}</span>
                             </li>`;

                    // Cria o botão "Próximo" e desativa-o se for a última página
                    html += `<li class="page-item ${pageData.last ? 'disabled' : ''}">
                                <a class="page-link" href="#" data-page="${pageData.number + 1}">Próximo</a>
                             </li>`;

                    html += '</ul>';
                    paginacaoContainer.innerHTML = html;
                }

        // --- Inicialização e Event Listeners ---
        async function init() {
                try {
                    const response = await apiClient.get('/perfil');
                    currentUserRole = response.data.nomeCargo;
                    document.querySelectorAll('.admin-only').forEach(el => {
                        if (currentUserRole === 'ADMIN') {
                            // Ajuste para funcionar com o dropdown
                            if (el.classList.contains('btn-group')) {
                                el.style.display = 'inline-block';
                            } else {
                                el.style.display = el.tagName === 'TH' || el.tagName === 'TD' ? 'table-cell' : '';
                            }
                        }
                    });
                } catch (e) { return; }

                await carregarCategorias();
                await carregarEquipamentos(0);

                // --- Event Listeners Corrigidos ---

                if (formFiltros) formFiltros.addEventListener('submit', (e) => { e.preventDefault(); carregarEquipamentos(0); });
                if (btnLimparFiltros) btnLimparFiltros.addEventListener('click', () => { formFiltros.reset(); carregarEquipamentos(0); });

                // Listener para "Adicionar Equipamento" (agora separado)
                const btnAdicionarEquipamento = document.getElementById('btn-adicionar-equipamento');
                if (btnAdicionarEquipamento) {
                    btnAdicionarEquipamento.addEventListener('click', () => {
                        window.navigateTo('equipamento-form.html');
                    });
                }

                // Listener para "Gerir Categorias" (agora separado e correto)
                const btnGerirCategorias = document.getElementById('btn-gerir-categorias');
                if (btnGerirCategorias) {
                    btnGerirCategorias.addEventListener('click', () => {
                        window.navigateTo('categorias.html');
                    });
                }

                // Listener para a tabela de equipamentos
                if (corpoTabela) {
                    corpoTabela.addEventListener('click', function(event) {
                        const target = event.target.closest('button');
                        if (!target) return;
                        const id = target.dataset.id;

                        if (target.classList.contains('btn-editar')) {
                            window.navigateTo('equipamento-form.html', { id: id });
                        } else if (target.classList.contains('btn-desativar')) {
                            showConfirmModal('Desativar Equipamento', `Tem a certeza?`, async () => {
                                try {
                                    await apiClient.delete(`/equipamentos/${id}`);
                                    showToast('Equipamento desativado.', 'Sucesso');
                                    carregarEquipamentos(currentPage);
                                    }catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                            });
                        } else if (target.classList.contains('btn-ativar')) { // <-- Esta parte é necessária
                                              showConfirmModal('Ativar Equipamento', `Tem a certeza?`, async () => {
                                  try {
                                      await apiClient.patch(`/equipamentos/${id}/ativar`);
                                      showToast('Equipamento ativado!', 'Sucesso');
                                      carregarEquipamentos(currentPage);
                                  } catch(error) {
                                      showToast(error.response?.data?.message, 'Erro', true);
                                }
                            });
                        }
                    });
                }

                // Listener para a paginação
                if (paginacaoContainer) {
                    paginacaoContainer.addEventListener('click', (event) => {
                        const link = event.target.closest('a.page-link');
                        if (link && !link.parentElement.classList.contains('disabled')) {
                            event.preventDefault();
                            carregarEquipamentos(parseInt(link.dataset.page));
                        }
                    });
                }
            }
        init();
    })();
}, 0);