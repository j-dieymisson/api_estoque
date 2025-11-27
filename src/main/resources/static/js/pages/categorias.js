// categorias.js - Lógica para a página de gestão de categorias
setTimeout(() => {
    (async function() {

        // --- Seletores ---
        const corpoTabela = document.getElementById('corpo-tabela-categorias');
        const btnAdicionar = document.getElementById('btn-adicionar-categoria');
        const btnVoltar = document.getElementById('btn-voltar-equipamentos');

        // --- Funções ---
        async function carregarCategorias() {
            corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center">A carregar...</td></tr>`;
            try {
                const response = await apiClient.get('/categorias');
                const categorias = response.data;
                corpoTabela.innerHTML = '';

                if (categorias.length === 0) {
                    corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center">Nenhuma categoria encontrada.</td></tr>`;
                    return;
                }

                categorias.forEach(cat => {
                    const tr = document.createElement('tr');
                    const statusBadge = cat.ativa ? '<span class="badge bg-success">Ativa</span>' : '<span class="badge bg-secondary">Inativa</span>';
                    const actionButton = cat.ativa
                        ? `<button class="btn btn-sm btn-outline-success btn-desativar-categoria" data-id="${cat.id}" title="Desativar"><i class="bi bi-toggle-off"></i></button>`
                        : `<button class="btn btn-sm btn-outline-danger btn-ativar-categoria" data-id="${cat.id}" title="Ativar"><i class="bi bi-toggle-on"></i></button>`;

                    tr.innerHTML = `
                        <td>${cat.id}</td>
                        <td>${cat.nome}</td>
                        <td>${statusBadge}</td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary btn-editar-categoria" data-id="${cat.id}" title="Editar"><i class="bi bi-pencil-fill"></i></button>
                            ${actionButton}
                        </td>
                    `;
                    corpoTabela.appendChild(tr);
                });

            } catch (error) {
                console.error("Erro ao carregar categorias:", error);
                corpoTabela.innerHTML = `<tr><td colspan="4" class="text-center text-danger">Falha ao carregar categorias.</td></tr>`;
            }
        }

        // --- Event Listeners ---
        if (btnAdicionar) {
            btnAdicionar.addEventListener('click', () => {
                window.navigateTo('categoria-form.html');
            });
        }

        if (btnVoltar) {
            btnVoltar.addEventListener('click', () => {
                window.navigateTo('equipamentos.html');
            });
        }

        if (corpoTabela) {
            corpoTabela.addEventListener('click', async function(event) {
                const target = event.target.closest('button');
                if (!target) return;
                const id = target.dataset.id;

                if (target.classList.contains('btn-editar-categoria')) {
                    window.navigateTo('categoria-form.html', { id: id });
                } else if (target.classList.contains('btn-desativar-categoria')) {
                    showConfirmModal('Desativar Categoria', `Tem a certeza?`, async () => {
                        try {
                            await apiClient.delete(`/categorias/${id}`);
                            showToast('Categoria desativada.', 'Sucesso');
                            await carregarCategorias();
                        } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                    });
                } else if (target.classList.contains('btn-ativar-categoria')) {
                    try {
                        await apiClient.patch(`/categorias/${id}/ativar`);
                        showToast('Categoria ativada.', 'Sucesso');

                        await carregarCategorias();
                    } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                }
            });
        }

        // --- Inicialização ---
        carregarCategorias();

    })();
}, 0);