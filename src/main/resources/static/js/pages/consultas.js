// consultas.js
setTimeout(() => {
    (function() {
        console.log("A executar o script da página de consultas...");

        // --- Seletores da Aba 1 ---
        const formRelatorioEquipamentos = document.getElementById('form-relatorio-equipamentos');
        const formBuscaEquipamento = document.getElementById('form-busca-equipamento');

        // --- Seletores da Aba 2 ---
        const formBuscaSolicitacaoUsuario = document.getElementById('form-busca-solicitacao-usuario');
        const formBuscaSolicitacaoId = document.getElementById('form-busca-solicitacao-id');
        const auditoriaTab = document.getElementById('auditoriaTab');
        const filtroStatusSelect = document.getElementById('filtro-sol-status');

        // --- Lógica para Salvar e Restaurar Abas ---

        // 1. Restaurar a aba ativa (ao voltar)
        const savedTabId = window.pageContext?.activeTab;
        if (savedTabId) {
            const tabParaAtivar = document.getElementById(savedTabId);
            if (tabParaAtivar) {
                // Usa a API do Bootstrap para mostrar a aba correta
                const tab = new bootstrap.Tab(tabParaAtivar);
                tab.show();
            }
        }

        // 2. Salvar a aba no histórico sempre que o utilizador clicar nela
        if (auditoriaTab) {
            auditoriaTab.addEventListener('click', function(event) {
                const target = event.target.closest('button[data-bs-toggle="tab"]');
                if (target) {
                    // Salva o ID da aba que foi clicada (ex: 'solicitacoes-tab')
                    window.updateCurrentHistoryContext({ activeTab: target.id });
                }
            });
        }

        // --- Lógica de Navegação da Aba 1 ---
        if (formRelatorioEquipamentos) {
            formRelatorioEquipamentos.addEventListener('submit', function(event) {
                event.preventDefault(); // AGORA VAI FUNCIONAR
                const dataInicio = document.getElementById('filtro-eq-data-inicio').value;
                const dataFim = document.getElementById('filtro-eq-data-fim').value;
                if (dataInicio && dataFim) {
                    window.navigateTo('consulta-equipamentos-lista.html', { dataInicioCriacao: dataInicio, dataFimCriacao: dataFim });
                }
            });
        }

        if (formBuscaEquipamento) {
            formBuscaEquipamento.addEventListener('submit', function(event) {
                event.preventDefault(); // AGORA VAI FUNCIONAR
                const id = document.getElementById('equipamento-id-busca').value;
                if (id) {
                    window.navigateTo('consulta-equipamento.html', { id: id });
                }
            });
        }

        // --- Lógica de Navegação da Aba 2 ---
        if (formBuscaSolicitacaoUsuario) {
           formBuscaSolicitacaoUsuario.addEventListener('submit', function(event) {
               event.preventDefault(); // AGORA VAI FUNCIONAR

               const usuarioId = document.getElementById('usuario-id-busca').value;
               const dataInicio = document.getElementById('filtro-sol-data-inicio').value;
               const dataFim = document.getElementById('filtro-sol-data-fim').value;
               const statuses = document.getElementById('filtro-sol-status').value;
               const indeterminada = document.getElementById('filtro-sol-indeterminada').checked;

               if (usuarioId) {
                   window.navigateTo('consulta-solicitacoes-lista.html', {
                       usuarioId: usuarioId,
                       dataInicio: dataInicio,
                       dataFim: dataFim,
                       statuses: statuses || null,
                       devolucaoIndeterminada: indeterminada ? true : null
                   });
               }
           });
        }

        if (formBuscaSolicitacaoId) {
            formBuscaSolicitacaoId.addEventListener('submit', function(event) {
                event.preventDefault(); // AGORA VAI FUNCIONAR
                const id = document.getElementById('solicitacao-id-busca').value;
                if (id) {
                    window.navigateTo('solicitacao-detalhe.html', { id: id });
                }
            });
        }

        async function carregarFiltroStatus() {
            if (!filtroStatusSelect) return;

            const statusVisiveis = [
                { value: 'PENDENTE_GESTOR,PENDENTE_ADMIN', text: 'Pendente' },
                { value: 'APROVADA',   text: 'Aprovada' },
                { value: 'FINALIZADA', text: 'Finalizada' },
                { value: 'RECUSADA',   text: 'Recusada' },
                { value: 'CANCELADA',  text: 'Cancelada' },
                { value: 'RASCUNHO',   text: 'Rascunho' }
            ];

            filtroStatusSelect.innerHTML = '<option value="">Todos</option>';
            statusVisiveis.forEach(status => {
                const option = new Option(status.text, status.value);
                filtroStatusSelect.appendChild(option);
            });
        }

        // Função 'init' separada para carregar dados assíncronos
        async function init() {
            try {
                // Agora o 'await' está num try...catch e não bloqueia
                // os event listeners se falhar.
                await carregarFiltroStatus();
            } catch (error) {
                console.error("Erro ao carregar filtros de status:", error);
                if (filtroStatusSelect) {
                    filtroStatusSelect.innerHTML = '<option value="">Erro ao carregar</option>';
                    filtroStatusSelect.disabled = true;
                }
            }
        }

        // Inicia o carregamento dos dados
        init();

    })();
}, 0);