// funcionarios.js - Versão final e completa
setTimeout(() => {
    (async function() {
        console.log("A executar o script da página de funcionários...");

        // --- Seletores e Variáveis ---
        const corpoTabela = document.getElementById('corpo-tabela-funcionarios');
        const paginacaoContainer = document.getElementById('paginacao-funcionarios');
        const formPesquisa = document.getElementById('form-pesquisa-funcionario');
        const inputPesquisaNome = document.getElementById('input-pesquisa-nome');
        let currentPage = 0;

        const modalFuncionarioEl = document.getElementById('modal-funcionario');
        const modalFuncionario = new bootstrap.Modal(modalFuncionarioEl);
        const formFuncionario = document.getElementById('form-funcionario');
        const modalFuncionarioLabel = document.getElementById('modalFuncionarioLabel');
        const funcionarioIdInput = document.getElementById('funcionario-id');
        const funcionarioNomeInput = document.getElementById('funcionario-nome');
        const funcionarioEmailInput = document.getElementById('funcionario-email');
        const funcionarioCargoSelect = document.getElementById('funcionario-cargo');
        const campoSenha = document.getElementById('campo-senha');
        const funcionarioSenhaInput = document.getElementById('funcionario-senha');
        const btnAdicionarFuncionario = document.getElementById('btn-adicionar-funcionario');

        const modalSenhaEl = document.getElementById('modal-alterar-senha');
        const modalSenha = new bootstrap.Modal(modalSenhaEl);
        const formSenha = document.getElementById('form-alterar-senha');
        const alterarSenhaUsuarioIdInput = document.getElementById('alterar-senha-usuario-id');
        const novaSenhaInput = document.getElementById('nova-senha');

        // --- Funções de Busca e Renderização ---
        async function carregarUsuarios(page = 0) {
            currentPage = page;
            corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center">A carregar...</td></tr>';
            const nomePesquisado = inputPesquisaNome.value;
            const params = { page, size: 10, sort: 'nome,asc' };
            if (nomePesquisado) params.nome = nomePesquisado;
            try {
                const response = await apiClient.get('/usuarios', { params });
                renderizarTabela(response.data.content);
                renderizarPaginacao(response.data);
            } catch (error) {
                corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Falha ao carregar.</td></tr>';
            }
        }

        function renderizarTabela(usuarios) {
            corpoTabela.innerHTML = '';
            if (!usuarios || usuarios.length === 0) {
                corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhum funcionário encontrado.</td></tr>';
                return;
            }
            usuarios.forEach(user => {
                const tr = document.createElement('tr');
                const statusBadge = `<span class="badge ${user.ativo ? 'bg-success' : 'bg-danger'}">${user.ativo ? 'Ativo' : 'Inativo'}</span>`;
                const acoesHtml = `<button class="btn btn-sm btn-outline-primary btn-editar" data-id="${user.id}" title="Editar"><i class="bi bi-pencil-fill"></i></button>
                                    <button class="btn btn-sm btn-outline-secondary btn-senha ms-1" data-id="${user.id}" data-nome="${user.nome}" title="Alterar senha"><i class="bi bi-key-fill"></i></button>
                                    ${user.ativo ? `<button class="btn btn-sm btn-outline-success btn-desativar ms-1" data-id="${user.id}" title="Desativar"><i class="bi bi-toggle-off"></i></button>` : `<button class="btn btn-sm btn-outline-danger btn-ativar ms-1" data-id="${user.id}" title="Ativar"><i class="bi bi-toggle-on"></i></button>`}`;
                tr.innerHTML = `<td>${user.id}</td>
                                <td class="truncate-text" title="${user.nome}">${user.nome}</td>
                                <td class="truncate-text" title="${user.email}">${user.email}</td>
                                <td>${user.nomeCargo}</td>
                                <td>${statusBadge}</td>
                                <td>${acoesHtml}</td>`;
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

        async function carregarCargos() {
            try {
                const response = await apiClient.get('/cargos');
                funcionarioCargoSelect.innerHTML = '<option value="">Selecione...</option>';
                response.data.forEach(cargo => funcionarioCargoSelect.appendChild(new Option(cargo.nome, cargo.id)));
            } catch (error) { showToast("Não foi possível carregar cargos.", "Erro", true); }
        }

        // --- Funções dos Modais ---
        function abrirModalParaCriar() {
            formFuncionario.reset();
            funcionarioIdInput.value = '';
            modalFuncionarioLabel.textContent = 'Adicionar Funcionário';
            campoSenha.style.display = 'block';
            funcionarioSenhaInput.required = true;
            modalFuncionario.show();
        }

        async function abrirModalParaEditar(id) {
            formFuncionario.reset();
            try {
                const response = await apiClient.get(`/usuarios/${id}`);
                const user = response.data;
                funcionarioIdInput.value = user.id;
                funcionarioNomeInput.value = user.nome;
                funcionarioEmailInput.value = user.email;
                funcionarioCargoSelect.value = user.cargoId;
                modalFuncionarioLabel.textContent = `Editar: ${user.nome}`;
                campoSenha.style.display = 'none';
                funcionarioSenhaInput.required = false;
                modalFuncionario.show();
            } catch (error) { showToast("Não foi possível carregar dados.", "Erro", true); }
        }

        async function salvarFuncionario(event) {
            event.preventDefault();
            const id = funcionarioIdInput.value;
            const isUpdate = !!id;
            const data = { nome: funcionarioNomeInput.value, email: funcionarioEmailInput.value, cargoId: parseInt(funcionarioCargoSelect.value) };
            if (!isUpdate) data.senha = funcionarioSenhaInput.value;
            try {
                if (isUpdate) {
                    await apiClient.put(`/usuarios/${id}`, data);
                    showToast('Funcionário atualizado!', 'Sucesso');
                } else {
                    await apiClient.post('/usuarios', data);
                    showToast('Funcionário criado!', 'Sucesso');
                }
                modalFuncionario.hide();
                carregarUsuarios(isUpdate ? currentPage : 0);
            } catch(error) { showToast(error.response?.data?.message || 'Erro ao salvar.', 'Erro', true); }
        }

        function abrirModalSenha(id, nome) {
            formSenha.reset();
            alterarSenhaUsuarioIdInput.value = id;
            modalSenhaEl.querySelector('.modal-title').textContent = `Alterar Senha de ${nome}`;
            modalSenha.show();
        }

        async function salvarNovaSenha(event) {
            event.preventDefault();
            const id = alterarSenhaUsuarioIdInput.value;
            const novaSenha = novaSenhaInput.value;
            try {
                await apiClient.patch(`/usuarios/${id}/alterar-senha`, { novaSenha });
                showToast('Senha alterada!', 'Sucesso');
                modalSenha.hide();
            } catch (error) { showToast(error.response?.data?.message || 'Erro ao alterar.', 'Erro', true); }
        }

        // --- Inicialização e Event Listeners ---
        async function init() {
            await carregarCargos();
            await carregarUsuarios(0);

            if(formPesquisa) formPesquisa.addEventListener('submit', (e) => { e.preventDefault(); carregarUsuarios(0); });
            if(paginacaoContainer) paginacaoContainer.addEventListener('click', (e) => {
                const link = e.target.closest('a.page-link');
                if (link && !link.parentElement.classList.contains('disabled')) { e.preventDefault(); carregarUsuarios(parseInt(link.dataset.page)); }
            });
            if (btnAdicionarFuncionario) btnAdicionarFuncionario.addEventListener('click', abrirModalParaCriar);
            if (formFuncionario) formFuncionario.addEventListener('submit', salvarFuncionario);
            if (formSenha) formSenha.addEventListener('submit', salvarNovaSenha);

            if(corpoTabela) corpoTabela.addEventListener('click', async function(event) {
                const target = event.target.closest('button');
                if (!target) return;
                const id = target.dataset.id;

                if (target.classList.contains('btn-editar')) abrirModalParaEditar(id);
                else if (target.classList.contains('btn-senha')) abrirModalSenha(id, target.dataset.nome);
                else if (target.classList.contains('btn-desativar')) {
                    showConfirmModal('Desativar Funcionário', `Tem a certeza?`, async () => {
                        try {
                            await apiClient.delete(`/usuarios/${id}`);
                            showToast('Funcionário desativado.', 'Sucesso');
                            carregarUsuarios(currentPage);
                        } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                    });
                } else if (target.classList.contains('btn-ativar')) {
                    showConfirmModal('Ativar Funcionário', `Tem a certeza?`, async () => {
                        try {
                            await apiClient.patch(`/usuarios/${id}/ativar`);
                            showToast('Funcionário ativado.', 'Sucesso');
                            carregarUsuarios(currentPage);
                        } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                    });
                }
            });
        }
        init();
    })();
}, 0);