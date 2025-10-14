// equipamento-form.js - Lógica para o formulário de criar/editar equipamento
setTimeout(() => {
    (async function() {
        console.log("A executar o script do formulário de equipamento...");

        // --- Seletores ---
        const equipamentoId = window.pageContext?.id;
        const isEditMode = !!equipamentoId;

        const tituloForm = document.getElementById('form-equipamento-titulo');
        const form = document.getElementById('form-equipamento');
        const idInput = document.getElementById('equipamento-id');
        const nomeInput = document.getElementById('equipamento-nome');
        const descricaoInput = document.getElementById('equipamento-descricao');
        const qtdTotalInput = document.getElementById('equipamento-qtd-total');
        const categoriaSelect = document.getElementById('equipamento-categoria');
        const btnVoltar = document.getElementById('btn-voltar-lista-equipamentos');

        // --- Funções ---
        async function carregarCategorias(categoriaSelecionadaId = null) {
            try {
                const response = await apiClient.get('/categorias?ativa=true');
                categoriaSelect.innerHTML = '<option value="">Selecione uma categoria...</option>';
                response.data.forEach(cat => {
                if (cat.ativa || cat.id === categoriaSelecionadaId) {
                    const option = new Option(cat.nome, cat.id);
                    categoriaSelect.appendChild(option);
                    }
                });
                if (categoriaSelecionadaId) {
                    categoriaSelect.value = categoriaSelecionadaId;
                }
            } catch (error) {
                console.error("Erro ao carregar categorias:", error);
                categoriaSelect.innerHTML = '<option value="">Erro ao carregar</option>';
            }
        }

        async function carregarDadosDoEquipamento() {
            tituloForm.textContent = `Editar Equipamento Cod:${equipamentoId}`;
            try {
                const response = await apiClient.get(`/equipamentos/${equipamentoId}`);
                const eq = response.data;

                idInput.value = eq.id;
                nomeInput.value = eq.nome;
                descricaoInput.value = eq.descricao;
                qtdTotalInput.value = eq.quantidadeTotal;

                // Carrega as categorias e já deixa a do equipamento selecionada
                await carregarCategorias(eq.categoriaId);

            } catch (error) {
                console.error("Erro ao carregar dados do equipamento:", error);
                showToast('Não foi possível carregar o equipamento.', 'Erro', true);
            }
        }

        async function salvarFormulario(event) {
            event.preventDefault();
            const data = {
                nome: nomeInput.value,
                descricao: descricaoInput.value,
                quantidadeTotal: parseInt(qtdTotalInput.value),
                categoriaId: parseInt(categoriaSelect.value),
            };

            try {
                if (isEditMode) {
                    await apiClient.put(`/equipamentos/${equipamentoId}`, data);
                    showToast('Equipamento atualizado com sucesso!', 'Sucesso');
                } else {
                    await apiClient.post('/equipamentos', data);
                    showToast('Equipamento criado com sucesso!', 'Sucesso');
                }
                // Volta para a página da lista
                window.navigateTo('equipamentos.html');
            } catch (error) {
                showToast(error.response?.data?.message || 'Erro ao salvar o equipamento.', 'Erro', true);
            }
        }

        // --- Inicialização e Event Listeners ---
        async function init() {
            if (isEditMode) {
                await carregarDadosDoEquipamento();
            } else {
                tituloForm.textContent = 'Adicionar Novo Equipamento';
                await carregarCategorias();
            }
            form.addEventListener('submit', salvarFormulario);
            btnVoltar.addEventListener('click', () => window.navigateTo('equipamentos.html'));
        }

        init();

    })();
}, 0);