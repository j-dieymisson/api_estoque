// funcionarios.js - Lógica para a página de gestão de funcionários

(async function() {
    console.log("A executar o script da página de funcionários...");

    // --- Seletores de Elementos ---
    const corpoTabela = document.getElementById('corpo-tabela-funcionarios');
    const btnAdicionar = document.getElementById('btn-adicionar-funcionario');

    // Modal Principal (Criar/Editar)
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

    // Modal de Alterar Senha
    const modalSenhaEl = document.getElementById('modal-alterar-senha');
    const modalSenha = new bootstrap.Modal(modalSenhaEl);
    const formSenha = document.getElementById('form-alterar-senha');
    const alterarSenhaUsuarioIdInput = document.getElementById('alterar-senha-usuario-id');
    const novaSenhaInput = document.getElementById('nova-senha');


    // --- Funções de Renderização e Busca ---

    // Carrega a lista de cargos para os menus <select>
    async function carregarCargos() {
        try {
            const response = await apiClient.get('/cargos');
            funcionarioCargoSelect.innerHTML = '<option value="">Selecione um cargo...</option>';
            response.data.forEach(cargo => {
                const option = new Option(cargo.nome, cargo.id);
                funcionarioCargoSelect.appendChild(option);
            });
        } catch (error) {
            console.error("Erro ao carregar cargos", error);
            showToast("Não foi possível carregar a lista de cargos.", "Erro", true);
        }
    }

    // Carrega a lista de utilizadores da API
    async function carregarUsuarios(page = 0, size = 10, nome = '') {
        corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center">A carregar...</td></tr>';
        try {
            const params = { page, size, nome, sort: 'nome,asc' };
            const response = await apiClient.get('/usuarios', { params });
            const usuarios = response.data.content;
            corpoTabela.innerHTML = ''; // Limpa a tabela

            if (usuarios.length === 0) {
                corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhum funcionário encontrado.</td></tr>';
                return;
            }

            usuarios.forEach(user => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${user.id}</td>
                    <td>${user.nome}</td>
                    <td>${user.email}</td>
                    <td>${user.nomeCargo}</td>
                    <td>
                        <span class="badge ${user.ativo ? 'bg-success' : 'bg-danger'}">
                            ${user.ativo ? 'Ativo' : 'Inativo'}
                        </span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-warning btn-editar" data-id="${user.id}" title="Editar"><i class="bi bi-pencil-fill"></i></button>
                        <button class="btn btn-sm btn-secondary btn-senha" data-id="${user.id}" data-nome="${user.nome}" title="Alterar Senha"><i class="bi bi-key-fill"></i></button>
                        ${user.ativo
                            ? `<button class="btn btn-sm btn-danger btn-desativar" data-id="${user.id}" title="Desativar"><i class="bi bi-toggle-off"></i></button>`
                            : `<button class="btn btn-sm btn-success btn-ativar" data-id="${user.id}" title="Ativar"><i class="bi bi-toggle-on"></i></button>`
                        }
                    </td>
                `;
                corpoTabela.appendChild(tr);
            });
        } catch (error) {
            console.error("Erro ao carregar funcionários:", error);
            showToast("Não foi possível carregar a lista de funcionários.", "Erro", true);
        }
    }

    // --- Funções de Controlo dos Modais ---

    function abrirModalParaCriar() {
        formFuncionario.reset();
        funcionarioIdInput.value = '';
        modalFuncionarioLabel.textContent = 'Adicionar Novo Funcionário';
        campoSenha.style.display = 'block'; // Mostra o campo senha
        funcionarioSenhaInput.required = true; // Torna a senha obrigatória
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
            funcionarioCargoSelect.value = user.cargoId; // Assumindo que o DTO de detalhe tem cargoId

            modalFuncionarioLabel.textContent = `Editar Funcionário: ${user.nome}`;
            campoSenha.style.display = 'none'; // Esconde o campo senha na edição
            funcionarioSenhaInput.required = false; // Senha não é obrigatória na edição

            modalFuncionario.show();
        } catch (error) {
            console.error("Erro ao buscar dados do funcionário:", error);
            showToast("Não foi possível carregar os dados do funcionário.", "Erro", true);
        }
    }

    async function salvarFuncionario(event) {
        event.preventDefault();
        const id = funcionarioIdInput.value;
        const isUpdate = !!id; // Se tem ID, é uma atualização

        const data = {
            nome: funcionarioNomeInput.value,
            email: funcionarioEmailInput.value,
            cargoId: parseInt(funcionarioCargoSelect.value)
        };

        try {
            if (isUpdate) {
                await apiClient.put(`/usuarios/${id}`, data);
                showToast('Funcionário atualizado com sucesso!', 'Sucesso');
            } else {
                data.senha = funcionarioSenhaInput.value;
                await apiClient.post('/usuarios', data);
                showToast('Funcionário criado com sucesso!', 'Sucesso');
            }
            modalFuncionario.hide();
            carregarUsuarios();
        } catch(error) {
            console.error("Erro ao salvar funcionário:", error.response);
            showToast(error.response?.data?.message || 'Erro ao salvar.', 'Erro', true);
        }
    }

    function abrirModalSenha(id, nome) {
        formSenha.reset();
        alterarSenhaUsuarioIdInput.value = id;
        modalSenha.show();
        modalSenhaEl.querySelector('.modal-title').textContent = `Alterar Senha de ${nome}`;
    }

    async function salvarNovaSenha(event) {
        event.preventDefault();
        const id = alterarSenhaUsuarioIdInput.value;
        const novaSenha = novaSenhaInput.value;

        try {
            await apiClient.patch(`/usuarios/${id}/alterar-senha`, { novaSenha });
            showToast('Senha alterada com sucesso!', 'Sucesso');
            modalSenha.hide();
        } catch (error) {
            console.error("Erro ao alterar senha:", error.response);
            showToast(error.response?.data?.message || 'Erro ao alterar senha.', 'Erro', true);
        }
    }

    // --- Event Listeners ---
    btnAdicionar.addEventListener('click', abrirModalParaCriar);
    formFuncionario.addEventListener('submit', salvarFuncionario);
    formSenha.addEventListener('submit', salvarNovaSenha);

    // Delegação de eventos para os botões na tabela
    corpoTabela.addEventListener('click', async function(event) {
        const target = event.target.closest('button');
        if (!target) return;

        const id = target.dataset.id;
        if (target.classList.contains('btn-editar')) {
            abrirModalParaEditar(id);
        } else if (target.classList.contains('btn-senha')) {
            abrirModalSenha(id, target.dataset.nome);
        } else if (target.classList.contains('btn-desativar')) {
            if (confirm(`Tem a certeza que quer desativar o utilizador ${id}?`)) {
                try {
                    await apiClient.delete(`/usuarios/${id}`);
                    showToast('Funcionário desativado com sucesso.', 'Sucesso');
                    carregarUsuarios();
                } catch (error) {
                    showToast(error.response?.data?.message || 'Erro ao desativar.', 'Erro', true);
                }
            }
        } else if (target.classList.contains('btn-ativar')) {
             if (confirm(`Tem a certeza que quer ativar o utilizador ${id}?`)) {
                try {
                    await apiClient.patch(`/usuarios/${id}/ativar`);
                    showToast('Funcionário ativado com sucesso.', 'Sucesso');
                    carregarUsuarios();
                } catch (error) {
                    showToast(error.response?.data?.message || 'Erro ao ativar.', 'Erro', true);
                }
            }
        }
    });

    // --- Inicialização ---
    carregarCargos();
    carregarUsuarios();

})();