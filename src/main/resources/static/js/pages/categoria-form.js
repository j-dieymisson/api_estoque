// categoria-form.js - Lógica para o formulário de criar/editar categoria
setTimeout(() => {
    (async function() {
        console.log("A executar o script do formulário de categoria...");

        // --- Seletores ---
        const categoriaId = window.pageContext?.id;
        const isEditMode = !!categoriaId;

        const tituloForm = document.getElementById('form-categoria-titulo');
        const form = document.getElementById('form-categoria');
        const idInput = document.getElementById('categoria-id');
        const nomeInput = document.getElementById('categoria-nome');
        const btnVoltar = document.getElementById('btn-voltar-lista-categorias');

        // --- Funções ---
        async function carregarDadosDaCategoria() {
            tituloForm.textContent = `Editar Categoria #${categoriaId}`;
            try {
                const response = await apiClient.get(`/categorias/${categoriaId}`);
                const cat = response.data;
                idInput.value = cat.id;
                nomeInput.value = cat.nome;
            } catch (error) {
                console.error("Erro ao carregar dados da categoria:", error);
                showToast('Não foi possível carregar a categoria.', 'Erro', true);
            }
        }

        async function salvarFormulario(event) {
            event.preventDefault();
            const data = { nome: nomeInput.value };

            try {
                if (isEditMode) {
                    await apiClient.put(`/categorias/${categoriaId}`, data);
                    showToast('Categoria atualizada com sucesso!', 'Sucesso');
                } else {
                    await apiClient.post('/categorias', data);
                    showToast('Categoria criada com sucesso!', 'Sucesso');
                }
                // Volta para a página da lista de categorias
                window.navigateTo('categorias.html');
            } catch (error) {
                showToast(error.response?.data?.message || 'Erro ao salvar a categoria.', 'Erro', true);
            }
        }

        // --- Inicialização e Event Listeners ---
        async function init() {
            if (isEditMode) {
                await carregarDadosDaCategoria();
            } else {
                tituloForm.textContent = 'Adicionar Nova Categoria';
            }
            form.addEventListener('submit', salvarFormulario);
            btnVoltar.addEventListener('click', () => window.navigateTo('categorias.html'));
        }

        init();

    })();
}, 0);