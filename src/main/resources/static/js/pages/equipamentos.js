// equipamentos.js - Lógica para a página de gestão de equipamentos

(async function() {
    console.log("A executar o script da página de equipamentos...");

    // Seletores dos elementos principais
    const corpoTabela = document.getElementById('corpo-tabela-equipamentos');
    const formFiltros = document.getElementById('form-filtros-equipamentos');
    const filtroNome = document.getElementById('filtro-nome');
    const filtroCategoria = document.getElementById('filtro-categoria');
    const btnLimparFiltros = document.getElementById('btn-limpar-filtros-equipamentos');

    // Seletores do Modal
    const modalEquipamento = new bootstrap.Modal(document.getElementById('modalEquipamento'));
    const formEquipamento = document.getElementById('form-equipamento');
    const modalTitle = document.getElementById('modalEquipamentoLabel');
    const equipamentoIdInput = document.getElementById('equipamento-id');
    const equipamentoNomeInput = document.getElementById('equipamento-nome');
    const equipamentoDescricaoInput = document.getElementById('equipamento-descricao');
    const equipamentoQtdTotalInput = document.getElementById('equipamento-qtd-total');
    const equipamentoCategoriaSelect = document.getElementById('equipamento-categoria');


    // Função para buscar e renderizar os equipamentos na tabela
    async function carregarEquipamentos(params = {}) {
        corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center"><div class="spinner-border spinner-border-sm" role="status"></div></td></tr>';
        try {
            // A listagem de equipamentos é para todos, mas as ações de admin são protegidas
            const response = await apiClient.get('/equipamentos', { params });
            const equipamentos = response.data.content;
            corpoTabela.innerHTML = '';

            if (equipamentos.length === 0) {
                corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhum equipamento encontrado.</td></tr>';
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
                    <td class="admin-only">
                        <button class="btn btn-sm btn-warning" title="Editar">
                            <i class="bi bi-pencil-fill"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" title="Desativar">
                            <i class="bi bi-trash-fill"></i>
                        </button>
                    </td>
                `;
                corpoTabela.appendChild(tr);
            });
        } catch (error) {
            console.error("Erro ao carregar equipamentos:", error);
            corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Falha ao carregar equipamentos.</td></tr>';
        }
    }

    // Função para preencher os <select> de categorias
    async function carregarCategorias() {
        try {
            const response = await apiClient.get('/categorias?ativa=true');
            const categorias = response.data;

            // Limpa as opções existentes
            filtroCategoria.innerHTML = '<option value="">Todas</option>';
            equipamentoCategoriaSelect.innerHTML = '<option value="">Selecione...</option>';

            categorias.forEach(cat => {
                const optionFiltro = new Option(cat.nome, cat.id);
                const optionModal = new Option(cat.nome, cat.id);
                filtroCategoria.appendChild(optionFiltro);
                equipamentoCategoriaSelect.appendChild(optionModal);
            });
        } catch (error) {
            console.error("Erro ao carregar categorias:", error);
        }
    }

    // --- Lógica do Formulário do Modal ---
    async function salvarEquipamento(event) {
        event.preventDefault();

        const equipamentoData = {
            nome: equipamentoNomeInput.value,
            descricao: equipamentoDescricaoInput.value,
            quantidadeTotal: parseInt(equipamentoQtdTotalInput.value, 10),
            categoriaId: parseInt(equipamentoCategoriaSelect.value, 10),
        };

        try {
            // Por agora, estamos a implementar apenas a criação (POST)
            await apiClient.post('/equipamentos', equipamentoData);

            showToast('Equipamento salvo com sucesso!', 'Sucesso');
            modalEquipamento.hide(); // Fecha o modal
            carregarEquipamentos(); // Recarrega a tabela
        } catch (error) {
            console.error("Erro ao salvar equipamento:", error.response.data);
            showToast(error.response.data.message || 'Não foi possível salvar o equipamento.', 'Erro', true);
        }
    }

    // --- Event Listeners ---
    formFiltros.addEventListener('submit', function(event) {
        event.preventDefault();
        const params = {
            nome: filtroNome.value || null,
            categoriaId: filtroCategoria.value || null,
        };
        Object.keys(params).forEach(key => params[key] == null && delete params[key]);
        carregarEquipamentos(params);
    });

    btnLimparFiltros.addEventListener('click', function() {
        formFiltros.reset();
        carregarEquipamentos();
    });

    formEquipamento.addEventListener('submit', salvarEquipamento);

    // Limpa o formulário do modal sempre que ele é fechado
    document.getElementById('modalEquipamento').addEventListener('hidden.bs.modal', function () {
        formEquipamento.reset();
        modalTitle.textContent = 'Adicionar Novo Equipamento';
    });


    // --- Inicialização da Página ---
    carregarCategorias();
    carregarEquipamentos();

})();