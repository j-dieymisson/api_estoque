// consultas.js - Versão CORRETA, apenas para navegação, seguindo o plano final.
setTimeout(() => {
    (function() {
        console.log("A executar o script da página de consultas...");

        // --- Seletores da Aba 1: Auditoria de Equipamentos ---
        const formRelatorioEquipamentos = document.getElementById('form-relatorio-equipamentos');
        const formBuscaEquipamento = document.getElementById('form-busca-equipamento');

        // --- Seletores da Aba 2: Auditoria de Solicitações ---
        const formBuscaSolicitacaoUsuario = document.getElementById('form-busca-solicitacao-usuario');
        const formBuscaSolicitacaoId = document.getElementById('form-busca-solicitacao-id');

        const filtroStatusSelect = document.getElementById('filtro-sol-status');

        async function carregarFiltroStatus() {
                    // (Verificamos se o seletor existe nesta página)
                    if (!filtroStatusSelect) return;

                    // Criamos a lista "limpa" de status para o utilizador
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
                        // (Assumindo que quem consulta pode ver tudo, incl. Rascunhos)
                        const option = new Option(status.text, status.value);
                        filtroStatusSelect.appendChild(option);
                    });
                }

                await carregarFiltroStatus();

        // --- Lógica de Navegação da Aba 1 ---

        // Ferramenta 1: Relatório de Equipamentos por Data
        if (formRelatorioEquipamentos) {
            formRelatorioEquipamentos.addEventListener('submit', function(event) {
                event.preventDefault();
                const dataInicio = document.getElementById('filtro-eq-data-inicio').value;
                const dataFim = document.getElementById('filtro-eq-data-fim').value;
                if (dataInicio && dataFim) {
                    window.navigateTo('consulta-equipamentos-lista.html', { dataInicioCriacao: dataInicio, dataFimCriacao: dataFim });
                }
            });
        }

        // Ferramenta 2: Rastrear Equipamento Específico
        if (formBuscaEquipamento) {
            formBuscaEquipamento.addEventListener('submit', function(event) {
                event.preventDefault();
                const id = document.getElementById('equipamento-id-busca').value;
                if (id) {
                    window.navigateTo('consulta-equipamento.html', { id: id });
                }
            });
        }

        // --- Lógica de Navegação da Aba 2 ---

        // Ferramenta 1: Relatório de Solicitações por Funcionário
       if (formBuscaSolicitacaoUsuario) {
                   formBuscaSolicitacaoUsuario.addEventListener('submit', function(event) {
                       event.preventDefault();

                       // Lê os filtros antigos
                       const usuarioId = document.getElementById('usuario-id-busca').value;
                       const dataInicio = document.getElementById('filtro-sol-data-inicio').value;
                       const dataFim = document.getElementById('filtro-sol-data-fim').value;

                       // --- LÊ OS NOVOS FILTROS ---
                       const statuses = document.getElementById('filtro-sol-status').value;
                       const indeterminada = document.getElementById('filtro-sol-indeterminada').checked;
                       // --- FIM ---

                       if (usuarioId) {
                           // Envia TUDO para a página de resultados
                           window.navigateTo('consulta-solicitacoes-lista.html', {
                               usuarioId: usuarioId,
                               dataInicio: dataInicio,
                               dataFim: dataFim,
                               // --- NOVAS PROPRIEDADES AQUI ---
                               statuses: statuses || null,
                               devolucaoIndeterminada: indeterminada ? true : null
                           });
                       }
                   });
               }

        // Ferramenta 2: Buscar Solicitação Específica
        if (formBuscaSolicitacaoId) {
            formBuscaSolicitacaoId.addEventListener('submit', function(event) {
                event.preventDefault();
                const id = document.getElementById('solicitacao-id-busca').value;
                if (id) {
                    window.navigateTo('solicitacao-detalhe.html', { id: id });
                }
            });
        }

    })();
}, 0);