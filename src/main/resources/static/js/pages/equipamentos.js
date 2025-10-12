// equipamentos.js - Lógica completa para a página de gestão de equipamentos

(async function() {
    console.log("A executar o script da página de equipamentos...");

    // --- Seletores Globais ---
    const corpoTabela = document.getElementById('corpo-tabela-equipamentos');
    const paginacaoContainer = document.getElementById('paginacao-equipamentos');
    let currentUserRole = null;
    let currentPage = 0;

    // --- Seletores de Filtros ---
    const formFiltros = document.getElementById('form-filtros-equipamentos');
    const filtroId = document.getElementById('filtro-id');
    const filtroNome = document.getElementById('filtro-nome');
    const filtroCategoria = document.getElementById('filtro-categoria');
    const btnLimparFiltros = document.getElementById('btn-limpar-filtros-equipamentos');

    // --- Seletores do Modal de Equipamento (Criar/Editar) ---
    const modalEquipamentoEl = document.getElementById('modalEquipamento');
    const modalEquipamento = new bootstrap.Modal(modalEquipamentoEl);
    const formEquipamento = document.getElementById('form-equipamento');
    const modalEquipamentoLabel = document.getElementById('modalEquipamentoLabel');
    const equipamentoIdInput = document.getElementById('equipamento-id');
    const equipamentoNomeInput = document.getElementById('equipamento-nome');
    const equipamentoDescricaoInput = document.getElementById('equipamento-descricao');
    const equipamentoQtdTotalInput = document.getElementById('equipamento-qtd-total');
    const equipamentoCategoriaSelect = document.getElementById('equipamento-categoria');
    const btnAdicionarEquipamento = document.getElementById('btn-adicionar-equipamento');

    // --- Seletores do Modal de Categorias ---
    const modalCategoriasEl = document.getElementById('modal-categorias');
    const modalCategorias = new bootstrap.Modal(modalCategoriasEl);
    const btnGerirCategorias = document.getElementById('btn-gerir-categorias');
    const listaCategoriasContainer = document.getElementById('lista-categorias');
    const formCategoria = document.getElementById('form-categoria');
    const categoriaIdInput = document.getElementById('categoria-id');
    const categoriaNomeInput = document.getElementById('categoria-nome');
    const btnCancelarEdicaoCategoria = document.getElementById('btn-cancelar-edicao-categoria');

    // --- Funções de Renderização ---
    function renderizarTabelaEquipamentos(equipamentos) {
        corpoTabela.innerHTML = '';
        if (!equipamentos || equipamentos.length === 0) {
            corpoTabela.innerHTML = `<tr><td colspan="${currentUserRole === 'ADMIN' ? 6 : 5}" class="text-center">Nenhum equipamento encontrado.</td></tr>`;
            return;
        }
        equipamentos.forEach(eq => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${eq.id}</td>
                <td>${eq.nome}</td>
                <td>${eq.nomeCategoria}</td>
                <td>${eq.quantidadeTotal}</td>
                <td>${eq.quantidadeDisponivel}</td>
                ${currentUserRole === 'ADMIN' ? `
                <td>
                    <button class="btn btn-sm btn-warning btn-editar" data-id="${eq.id}" title="Editar"><i class="bi bi-pencil-fill"></i></button>
                    <button class="btn btn-sm btn-danger btn-desativar" data-id="${eq.id}" title="Desativar"><i class="bi bi-trash-fill"></i></button>
                </td>` : ''}
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

    function renderizarListaCategorias(categorias) {
        listaCategoriasContainer.innerHTML = '';
        if (!categorias || categorias.length === 0) {
            listaCategoriasContainer.innerHTML = '<li class="list-group-item">Nenhuma categoria encontrada.</li>';
            return;
        }
        categorias.forEach(cat => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.innerHTML = `
                <span>${cat.nome}</span>
                <div>
                    <button class="btn btn-sm btn-outline-warning btn-editar-categoria" data-id="${cat.id}" data-nome="${cat.nome}"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-outline-danger btn-apagar-categoria ms-2" data-id="${cat.id}"><i class="bi bi-trash"></i></button>
                </div>
            `;
            listaCategoriasContainer.appendChild(li);
        });
    }

    // --- Funções de Busca de Dados ---
    async function carregarEquipamentos(page = 0) {
        currentPage = page;
        corpoTabela.innerHTML = `<tr><td colspan="${currentUserRole === 'ADMIN' ? 6 : 5}" class="text-center">A carregar...</td></tr>`;
        const params = {
            page, size: 10, sort: 'nome,asc',
            id: filtroId.value || null,
            nome: filtroNome.value || null,
            categoriaId: filtroCategoria.value || null,
        };
        Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
        try {
            const response = await apiClient.get('/equipamentos', { params });
            renderizarTabelaEquipamentos(response.data.content);
            renderizarPaginacao(response.data);
        } catch (error) {
            corpoTabela.innerHTML = `<tr><td colspan="${currentUserRole === 'ADMIN' ? 6 : 5}" class="text-center text-danger">Falha ao carregar equipamentos.</td></tr>`;
        }
    }

    async function carregarCategorias() {
        try {
            const response = await apiClient.get('/categorias');
            const categorias = response.data;
            filtroCategoria.innerHTML = '<option value="">Todas</option>';
            equipamentoCategoriaSelect.innerHTML = '<option value="">Selecione...</option>';
            categorias.forEach(cat => {
                filtroCategoria.appendChild(new Option(cat.nome, cat.id));
                equipamentoCategoriaSelect.appendChild(new Option(cat.nome, cat.id));
            });
            renderizarListaCategorias(categorias);
        } catch (error) { console.error("Erro ao carregar categorias:", error); }
    }

    // --- Lógica dos Modais ---
    function abrirModalParaCriarEquipamento() {
        formEquipamento.reset();
        equipamentoIdInput.value = '';
        modalEquipamentoLabel.textContent = 'Adicionar Novo Equipamento';
        modalEquipamento.show();
    }

    async function abrirModalParaEditarEquipamento(id) {
        formEquipamento.reset();
        try {
            const response = await apiClient.get(`/equipamentos/${id}`);
            const eq = response.data;
            equipamentoIdInput.value = eq.id;
            equipamentoNomeInput.value = eq.nome;
            equipamentoDescricaoInput.value = eq.descricao;
            equipamentoQtdTotalInput.value = eq.quantidadeTotal;
            equipamentoCategoriaSelect.value = eq.categoriaId;
            modalEquipamentoLabel.textContent = `Editar Equipamento: ${eq.nome}`;
            modalEquipamento.show();
        } catch (error) { showToast("Não foi possível carregar os dados do equipamento.", "Erro", true); }
    }

    async function salvarEquipamento(event) {
        event.preventDefault();
        const id = equipamentoIdInput.value;
        const isUpdate = !!id;
        const data = {
            nome: equipamentoNomeInput.value,
            descricao: equipamentoDescricaoInput.value,
            quantidadeTotal: parseInt(equipamentoQtdTotalInput.value),
            categoriaId: parseInt(equipamentoCategoriaSelect.value),
        };
        try {
            if (isUpdate) {
                await apiClient.put(`/equipamentos/${id}`, data);
                showToast('Equipamento atualizado com sucesso!', 'Sucesso');
            } else {
                await apiClient.post('/equipamentos', data);
                showToast('Equipamento criado com sucesso!', 'Sucesso');
            }
            modalEquipamento.hide();
            carregarEquipamentos(isUpdate ? currentPage : 0);
        } catch (error) { showToast(error.response?.data?.message || 'Erro ao salvar.', 'Erro', true); }
    }

    async function salvarCategoria(event) {
        event.preventDefault();
        const id = categoriaIdInput.value;
        const nome = categoriaNomeInput.value;
        const isUpdate = !!id;
        try {
            if (isUpdate) {
                await apiClient.put(`/categorias/${id}`, { nome });
                showToast('Categoria atualizada!', 'Sucesso');
            } else {
                await apiClient.post('/categorias', { nome });
                showToast('Categoria criada!', 'Sucesso');
            }
            formCategoria.reset();
            categoriaIdInput.value = '';
            btnCancelarEdicaoCategoria.style.display = 'none';
            carregarCategorias();
        } catch (error) { showToast(error.response?.data?.message || 'Erro ao salvar.', 'Erro', true); }
    }

    // --- Inicialização e Event Listeners ---
    async function init() {
        try {
            const response = await apiClient.get('/perfil');
            currentUserRole = response.data.nomeCargo;
            document.querySelectorAll('.admin-only').forEach(el => {
                el.style.display = currentUserRole === 'ADMIN' ? '' : 'none';
            });
        } catch (e) { return; }

        await carregarCategorias();
        await carregarEquipamentos();

        if (formFiltros) formFiltros.addEventListener('submit', (e) => { e.preventDefault(); carregarEquipamentos(0); });
        if (btnLimparFiltros) btnLimparFiltros.addEventListener('click', () => { formFiltros.reset(); carregarEquipamentos(0); });
        if (btnAdicionarEquipamento) btnAdicionarEquipamento.addEventListener('click', abrirModalParaCriarEquipamento);
        if (btnGerirCategorias) btnGerirCategorias.addEventListener('click', () => modalCategorias.show());
        if (formEquipamento) formEquipamento.addEventListener('submit', salvarEquipamento);
        if (formCategoria) formCategoria.addEventListener('submit', salvarCategoria);
        if (btnCancelarEdicaoCategoria) btnCancelarEdicaoCategoria.addEventListener('click', () => {
            formCategoria.reset();
            categoriaIdInput.value = '';
            btnCancelarEdicaoCategoria.style.display = 'none';
        });

        corpoTabela.addEventListener('click', function(event) {
            const target = event.target.closest('button');
            if (!target) return;
            const id = target.dataset.id;
            if (target.classList.contains('btn-editar')) {
                abrirModalParaEditarEquipamento(id);
            } else if (target.classList.contains('btn-desativar')) {
                showConfirmModal('Desativar Equipamento', `Tem a certeza?`, async () => {
                    try {
                        await apiClient.delete(`/equipamentos/${id}`);
                        showToast('Equipamento desativado.', 'Sucesso');
                        carregarEquipamentos(currentPage);
                    } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                });
            }
        });

        listaCategoriasContainer.addEventListener('click', function(event) {
            const target = event.target.closest('button');
            if (!target) return;
            const id = target.dataset.id;
            if (target.classList.contains('btn-editar-categoria')) {
                categoriaIdInput.value = id;
                categoriaNomeInput.value = target.dataset.nome;
                btnCancelarEdicaoCategoria.style.display = 'inline-block';
                categoriaNomeInput.focus();
            } else if (target.classList.contains('btn-apagar-categoria')) {
                showConfirmModal('Apagar Categoria', `Tem a certeza?`, async () => {
                    try {
                        await apiClient.delete(`/categorias/${id}`);
                        showToast('Categoria apagada.', 'Sucesso');
                        carregarCategorias();
                    } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                });
            }
        });

        paginacaoContainer.addEventListener('click', (event) => {
            const link = event.target.closest('a.page-link');
            if (link && !link.parentElement.classList.contains('disabled')) {
                event.preventDefault();
                carregarEquipamentos(parseInt(link.dataset.page));
            }
        });
    }

    init();
})();