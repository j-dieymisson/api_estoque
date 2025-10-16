// solicitacao-form.js - Versão final e correta
setTimeout(() => {
    (async function() {
        console.log("A executar o script do formulário de solicitação...");

        // --- Estado e Seletores ---
        const rascunhoId = window.pageContext?.rascunhoId; // O ID vem do clique em "Editar Rascunho"
        const isEditMode = !!rascunhoId;
        let itensDaSolicitacao = [];
        let itemSelecionadoParaAdicionar = null;

        // --- Seletores do Formulário Principal ---
        const formSolicitacao = document.getElementById('form-solicitacao');
        const tituloForm = document.getElementById('form-solicitacao-titulo');
        const tabelaItensSelecionados = document.getElementById('tabela-itens-selecionados');
        const btnVoltar = document.getElementById('btn-voltar-lista-solicitacoes');
        const acoesCriarDiv = document.getElementById('acoes-criar');
        const acoesEditarRascunhoDiv = document.getElementById('acoes-editar-rascunho');

        // --- Seletores do Modal "Adicionar Item" ---
        const modalAdicionarItemEl = document.getElementById('modal-adicionar-item');
        const modalAdicionarItem = new bootstrap.Modal(modalAdicionarItemEl);
        const formBuscaItemModal = document.getElementById('form-busca-item-modal');
        const buscaItemNome = document.getElementById('busca-item-nome');
        const buscaItemCategoria = document.getElementById('busca-item-categoria');
        const listaBuscaEquipamentos = document.getElementById('lista-busca-equipamentos');
        const areaAdicionarQuantidade = document.getElementById('area-adicionar-quantidade');
        const itemSelecionadoNome = document.getElementById('item-selecionado-nome');
        const itemSelecionadoDisponivel = document.getElementById('item-selecionado-disponivel');
        const itemQuantidadeInput = document.getElementById('item-quantidade');
        const btnConfirmarAdicionarItem = document.getElementById('btn-confirmar-adicionar-item');

        // --- Funções de Renderização e Lógica ---
        function renderizarTabelaItens() {
            tabelaItensSelecionados.innerHTML = '';
            if (itensDaSolicitacao.length === 0) {
                tabelaItensSelecionados.innerHTML = '<tr><td colspan="3" class="text-center text-muted">Nenhum item adicionado.</td></tr>';
                return;
            }
            itensDaSolicitacao.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `<td>${item.nome}</td><td>${item.quantidade}</td><td><button type="button" class="btn btn-sm btn-outline-danger btn-remover-item" data-id="${item.equipamentoId}"><i class="bi bi-trash"></i></button></td>`;
                tabelaItensSelecionados.appendChild(tr);
            });
        }

        async function carregarCategoriasModal() {
            try {
                const response = await apiClient.get('/categorias?ativa=true');
                buscaItemCategoria.innerHTML = '<option value="">Todas as Categorias</option>';
                response.data.forEach(cat => buscaItemCategoria.appendChild(new Option(cat.nome, cat.id)));
            } catch (error) { console.error("Erro ao carregar categorias no modal:", error); }
        }

        async function buscarEquipamentosModal(event) {
            if (event) event.preventDefault();
            const params = { nome: buscaItemNome.value || null, categoriaId: buscaItemCategoria.value || null, sort: 'nome,asc' };
            Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
            listaBuscaEquipamentos.innerHTML = '<li class="list-group-item text-center">A buscar...</li>';
            try {
                const response = await apiClient.get('/equipamentos', { params });
                const equipamentos = response.data.content;
                listaBuscaEquipamentos.innerHTML = '';
                if (equipamentos.length === 0) {
                    listaBuscaEquipamentos.innerHTML = '<li class="list-group-item">Nenhum equipamento encontrado.</li>';
                } else {
                    equipamentos.forEach(eq => {
                        const li = document.createElement('li');
                        li.className = 'list-group-item list-group-item-action';
                        li.style.cursor = 'pointer';
                        li.textContent = `${eq.nome} (Disponível: ${eq.quantidadeDisponivel})`;
                        li.dataset.equipamento = JSON.stringify(eq);
                        listaBuscaEquipamentos.appendChild(li);
                    });
                }
            } catch (error) { listaBuscaEquipamentos.innerHTML = '<li class="list-group-item text-danger">Erro ao buscar.</li>'; }
        }

        function selecionarEquipamento(equipamento) {
            itemSelecionadoParaAdicionar = equipamento;
            itemSelecionadoNome.textContent = equipamento.nome;
            itemSelecionadoDisponivel.textContent = equipamento.quantidadeDisponivel;
            itemQuantidadeInput.max = equipamento.quantidadeDisponivel;
            areaAdicionarQuantidade.classList.remove('d-none');
        }

        function adicionarItemAoCarrinho() {
            const quantidade = parseInt(itemQuantidadeInput.value);
            if (!itemSelecionadoParaAdicionar || !quantidade || quantidade <= 0) {
                showToast("Selecione um equipamento e insira uma quantidade válida.", "Aviso", true); return;
            }
            if (quantidade > itemSelecionadoParaAdicionar.quantidadeDisponivel) {
                showToast(`Quantidade solicitada (${quantidade}) excede o stock disponível (${itemSelecionadoParaAdicionar.quantidadeDisponivel}).`, "Stock Insuficiente", true); return;
            }
            if (itensDaSolicitacao.find(i => i.equipamentoId === itemSelecionadoParaAdicionar.id)) {
                 showToast('Este equipamento já foi adicionado.', 'Aviso', true); return;
            }
            itensDaSolicitacao.push({ equipamentoId: itemSelecionadoParaAdicionar.id, nome: itemSelecionadoParaAdicionar.nome, quantidade: quantidade });
            renderizarTabelaItens();
            modalAdicionarItem.hide();
        }

        async function carregarDadosDoRascunho() {
            try {
                const response = await apiClient.get(`/solicitacoes/${rascunhoId}`);
                const rascunho = response.data;
                document.getElementById('solicitacao-justificativa').value = rascunho.justificativa;
                document.getElementById('solicitacao-data-entrega').value = rascunho.dataPrevisaoEntrega;
                document.getElementById('solicitacao-data-devolucao').value = rascunho.dataPrevisaoDevolucao;
                itensDaSolicitacao = rascunho.itens.map(item => ({
                                    equipamentoId: item.equipamentoId, // O ID do equipamento está em 'item.id' no DTO de detalhes
                                    nome: item.nomeEquipamento,
                                    quantidade: item.quantidadeSolicitada
                                }));
                                renderizarTabelaItens();
            } catch (error) {
                showToast("Não foi possível carregar o rascunho.", "Erro", true);
                window.navigateTo('solicitacoes.html');
            }
        }

        async function enviarFormulario(endpoint, metodo = 'post') {
            const data = {
                justificativa: document.getElementById('solicitacao-justificativa').value,
                dataPrevisaoEntrega: document.getElementById('solicitacao-data-entrega').value || null,
                dataPrevisaoDevolucao: document.getElementById('solicitacao-data-devolucao').value || null,
                itens: itensDaSolicitacao.map(item => ({ equipamentoId: item.equipamentoId, quantidade: item.quantidade }))
            };

            if (endpoint === '/solicitacoes') {
                            if (!data.dataPrevisaoEntrega || !data.dataPrevisaoDevolucao) {
                                showToast('As datas de previsão são obrigatórias para enviar a solicitação.', 'Erro de Validação', true);
                                return; // Para a execução
                            }

                            const hoje = new Date();
                            hoje.setHours(0, 0, 0, 0); // Zera a hora para comparar apenas a data

                            // Adiciona 'T00:00:00' para evitar problemas de fuso horário na conversão
                            const entrega = new Date(data.dataPrevisaoEntrega + 'T00:00:00');
                            const devolucao = new Date(data.dataPrevisaoDevolucao + 'T00:00:00');

                            if (entrega < hoje) {
                                showToast('A data de previsão de entrega não pode ser no passado.', 'Erro de Validação', true);
                                return; // Para a execução
                            }

                            if (devolucao < hoje) {
                                 showToast('A data de previsão de devolução não pode ser no passado.', 'Erro de Validação', true);
                                return; // Para a execução
                            }

                            if (devolucao < entrega) {
                                showToast('A data de devolução não pode ser anterior à de entrega.', 'Erro de Validação', true);
                                return; // Para a execução
                            }
                        }
            if (itensDaSolicitacao.length === 0) { showToast('Adicione pelo menos um item.', 'Erro', true); return; }
            if (endpoint === '/solicitacoes' && (!data.dataPrevisaoEntrega || !data.dataPrevisaoDevolucao)) {
                showToast('As datas de previsão são obrigatórias para enviar a solicitação.', 'Erro', true); return;
            }
            try {
                await apiClient[metodo](endpoint, data);
                showToast('Operação realizada com sucesso!', 'Sucesso');
                window.navigateTo('solicitacoes.html');
            } catch (error) { showToast(error.response?.data?.message || 'Não foi possível salvar.', 'Erro', true); }
        }

        // --- Inicialização e Event Listeners ---
        async function init() {
            await carregarCategoriasModal();
            renderizarTabelaItens();

            if (isEditMode) {
                tituloForm.textContent = `Editar Rascunho #${rascunhoId}`;
                acoesCriarDiv.classList.add('d-none');
                acoesEditarRascunhoDiv.classList.remove('d-none');
                await carregarDadosDoRascunho();
            } else {
                tituloForm.textContent = 'Nova Solicitação de Equipamento';
            }

            // Listeners Modal
            formBuscaItemModal.addEventListener('submit', buscarEquipamentosModal);
            listaBuscaEquipamentos.addEventListener('click', (e) => { if (e.target?.dataset.equipamento) selecionarEquipamento(JSON.parse(e.target.dataset.equipamento)); });
            btnConfirmarAdicionarItem.addEventListener('click', adicionarItemAoCarrinho);
            modalAdicionarItemEl.addEventListener('hidden.bs.modal', () => {
                areaAdicionarQuantidade.classList.add('d-none');
                formBuscaItemModal.reset();
                listaBuscaEquipamentos.innerHTML = '';
            });

            // Listeners Tabela de Itens
            tabelaItensSelecionados.addEventListener('click', (e) => {
                const target = e.target.closest('.btn-remover-item');
                if (target) {
                    const idParaRemover = parseInt(target.dataset.id);
                    itensDaSolicitacao = itensDaSolicitacao.filter(i => i.equipamentoId !== idParaRemover);
                    renderizarTabelaItens();
                }
            });

            // Listeners Botões Principais
            btnVoltar.addEventListener('click', () => window.navigateTo('solicitacoes.html'));
            document.getElementById('btn-salvar-rascunho').addEventListener('click', () => enviarFormulario('/rascunhos'));
            formSolicitacao.addEventListener('submit', (e) => { e.preventDefault(); enviarFormulario('/solicitacoes'); });
            document.getElementById('btn-atualizar-rascunho').addEventListener('click', () => enviarFormulario(`/rascunhos/${rascunhoId}`, 'put'));
            document.getElementById('btn-enviar-rascunho').addEventListener('click', () => {
                            showConfirmModal('Enviar Solicitação', 'O rascunho será convertido numa solicitação pendente. Deseja continuar?', async () => {
                                 // Validação de datas
                                 if (!document.getElementById('solicitacao-data-entrega').value || !document.getElementById('solicitacao-data-devolucao').value) {
                                     showToast('Para enviar a solicitação, as datas de previsão são obrigatórias.', 'Erro', true);
                                     return;
                                 }
                                 try {
                                    // Passo 1: Salva as últimas alterações no rascunho (continua a ser PUT, como antes)
                                    await apiClient.put(`/rascunhos/${rascunhoId}`, {
                                        justificativa: document.getElementById('solicitacao-justificativa').value,
                                        dataPrevisaoEntrega: document.getElementById('solicitacao-data-entrega').value || null,
                                        dataPrevisaoDevolucão: document.getElementById('solicitacao-data-devolucao').value || null,
                                        itens: itensDaSolicitacao.map(item => ({ equipamentoId: item.equipamentoId, quantidade: item.quantidade }))
                                    });

                                    await apiClient.patch(`/rascunhos/${rascunhoId}/enviar`);

                                    showToast('Rascunho enviado com sucesso!', 'Sucesso');
                                    window.navigateTo('solicitacoes.html');
                                } catch (error) {
                                    showToast(error.response?.data?.message || 'Não foi possível enviar o rascunho.', 'Erro', true);
                                }
                            });
                        });
            document.getElementById('btn-apagar-rascunho').addEventListener('click', () => {
                showConfirmModal('Apagar Rascunho', 'Tem a certeza?', async () => {
                    try {
                        await apiClient.delete(`/rascunhos/${rascunhoId}`);
                        showToast('Rascunho apagado!', 'Sucesso');
                        window.navigateTo('solicitacoes.html');
                    } catch (error) { showToast(error.response?.data?.message || 'Não foi possível apagar.', 'Erro', true); }
                });
            });
        }

        init();
    })();
}, 0);