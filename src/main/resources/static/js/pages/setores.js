// setores.js - Lógica para a página de gestão de setores
setTimeout(() => {
    (async function() {

        // --- Seletores ---
        const corpoTabela = document.getElementById('corpo-tabela-setores');
        const btnAdicionarSetor = document.getElementById('btn-adicionar-setor');
        const btnVoltar = document.getElementById('btn-voltar-funcionarios');

        const modalSetorEl = document.getElementById('modal-setor');
        const modalSetor = new bootstrap.Modal(modalSetorEl);
        const formSetor = document.getElementById('form-setor');
        const modalSetorLabel = document.getElementById('modalSetorLabel');
        const setorIdInput = document.getElementById('setor-id');
        const setorNomeInput = document.getElementById('setor-nome');

        // --- Funções ---
        async function carregarSetores() {
            corpoTabela.innerHTML = '<tr><td colspan="4" class="text-center">A carregar...</td></tr>';
            try {
                // Chama o endpoint que criámos (listarTodos com apenasAtivos=false)
                const response = await apiClient.get('/setores');
                renderizarTabela(response.data);
            } catch (error) {
                console.error("Erro ao carregar setores:", error);
                corpoTabela.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Falha ao carregar setores.</td></tr>';
            }
        }

        function renderizarTabela(setores) {
            corpoTabela.innerHTML = '';
            if (!setores || setores.length === 0) {
                corpoTabela.innerHTML = '<tr><td colspan="4" class="text-center">Nenhum setor encontrado.</td></tr>';
                return;
            }

            setores.forEach(setor => {
                const tr = document.createElement('tr');
                const statusBadge = `<span class="badge ${setor.ativo ? 'bg-success' : 'bg-danger'}">${setor.ativo ? 'Ativo' : 'Inativo'}</span>`;

                let acoesHtml = `<button class="btn btn-sm btn-outline-primary btn-editar" data-id="${setor.id}" data-nome="${setor.nome}" title="Editar Nome"><i class="bi bi-pencil-fill"></i></button>`;

                if (setor.ativo) {
                    acoesHtml += ` <button class="btn btn-sm btn-outline-success btn-desativar ms-1" data-id="${setor.id}" title="Desativar"><i class="bi bi-toggle-off"></i></button>`;
                } else {
                    acoesHtml += ` <button class="btn btn-sm btn-outline-danger btn-ativar ms-1" data-id="${setor.id}" title="Ativar"><i class="bi bi-toggle-on"></i></button>`;
                }

                tr.innerHTML = `
                    <td>${setor.id}</td>
                    <td>${setor.nome}</td>
                    <td>${statusBadge}</td>
                    <td>${acoesHtml}</td>
                `;
                corpoTabela.appendChild(tr);
            });
        }

        function abrirModalParaCriar() {
            formSetor.reset();
            setorIdInput.value = '';
            modalSetorLabel.textContent = 'Adicionar Novo Setor';
            modalSetor.show();
        }

        function abrirModalParaEditar(id, nome) {
            formSetor.reset();
            setorIdInput.value = id;
            setorNomeInput.value = nome;
            modalSetorLabel.textContent = `Editar Setor: ${nome}`;
            modalSetor.show();
        }

        async function salvarSetor(event) {
            event.preventDefault();
            const id = setorIdInput.value;
            const isUpdate = !!id;
            const data = {
                nome: setorNomeInput.value
                // A API (SetorService) trata de definir 'ativo=true' na criação
            };

            try {
                if (isUpdate) {
                    await apiClient.put(`/setores/${id}`, data);
                    showToast('Setor atualizado!', 'Sucesso');
                } else {
                    await apiClient.post('/setores', data);
                    showToast('Setor criado!', 'Sucesso');
                }
                modalSetor.hide();
                await carregarSetores(); // Recarrega a tabela
            } catch(error) {
                showToast(error.response?.data?.message || 'Erro ao salvar.', 'Erro', true);
            }
        }

        // --- Inicialização e Event Listeners ---

        // Botão de Voltar para a página de funcionários
        if(btnVoltar) {
            btnVoltar.addEventListener('click', () => window.navigateTo('funcionarios.html'));
        }

        if(btnAdicionarSetor) {
            btnAdicionarSetor.addEventListener('click', abrirModalParaCriar);
        }

        if(formSetor) {
            formSetor.addEventListener('submit', salvarSetor);
        }

        // Listeners para a tabela (Editar, Ativar, Desativar)
        corpoTabela.addEventListener('click', async function(event) {
            const target = event.target.closest('button');
            if (!target) return;

            const id = target.dataset.id;

            if (target.classList.contains('btn-editar')) {
                abrirModalParaEditar(id, target.dataset.nome);
            }
            else if (target.classList.contains('btn-desativar')) {
                showConfirmModal('Desativar Setor', `Tem a certeza? (Utilizadores neste setor não poderão ter solicitações aprovadas por gestores).`, async () => {
                    try {
                        await apiClient.delete(`/setores/${id}`);
                        showToast('Setor desativado.', 'Sucesso');
                        await carregarSetores();
                    } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                });
            }
            else if (target.classList.contains('btn-ativar')) {
                showConfirmModal('Ativar Setor', `Tem a certeza?`, async () => {
                    try {
                        await apiClient.patch(`/setores/${id}/ativar`);
                        showToast('Setor ativado.', 'Sucesso');
                        await carregarSetores();
                    } catch(error) { showToast(error.response?.data?.message, 'Erro', true); }
                });
            }
        });

        // Carregamento inicial
        await carregarSetores();

    })();
}, 0);