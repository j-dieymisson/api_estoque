// historico.js - Lógica para a página de busca de históricos

(function() {
    console.log("A executar o script da página de histórico...");

    // Seletores da Aba de Equipamento
    const formBuscaEquipamento = document.getElementById('form-busca-equipamento');
    const inputEquipamentoId = document.getElementById('equipamento-id-busca');
    const resultadoEquipamento = document.getElementById('resultado-historico-equipamento');

    // Seletores da Aba de Solicitação
    const formBuscaSolicitacao = document.getElementById('form-busca-solicitacao');
    const inputSolicitacaoId = document.getElementById('solicitacao-id-busca');
    const resultadoSolicitacao = document.getElementById('resultado-historico-solicitacao');

    if (!formBuscaEquipamento || !formBuscaSolicitacao) {
        console.error("Elementos dos formulários de busca não encontrados!");
        return;
    }

    // --- Lógica para Histórico de Equipamento ---
    async function buscarHistoricoEquipamento(event) {
        event.preventDefault();
        const id = inputEquipamentoId.value;
        resultadoEquipamento.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"><span class="visually-hidden">A carregar...</span></div>';

        try {
            const response = await apiClient.get(`/historico/equipamento/${id}`);
            const historico = response.data.content;

            if (historico.length === 0) {
                resultadoEquipamento.innerHTML = '<p class="text-info">Nenhuma movimentação encontrada para este equipamento.</p>';
                return;
            }

            let tableHtml = `
                <div class="table-responsive">
                    <table class="table table-sm table-bordered">
                        <thead>
                            <tr>
                                <th>Data</th>
                                <th>Tipo</th>
                                <th>Qtd.</th>
                                <th>Stock Anterior</th>
                                <th>Stock Posterior</th>
                                <th>Responsável</th>
                            </tr>
                        </thead>
                        <tbody>
            `;
            historico.forEach(h => {
                tableHtml += `
                    <tr>
                        <td>${new Date(h.dataMovimentacao).toLocaleString('pt-BR')}</td>
                        <td>${h.tipoMovimentacao}</td>
                        <td>${h.quantidade}</td>
                        <td>${h.quantidadeAnterior}</td>
                        <td>${h.quantidadePosterior}</td>
                        <td>${h.usuarioResponsavel}</td>
                    </tr>
                `;
            });
            tableHtml += '</tbody></table></div>';
            resultadoEquipamento.innerHTML = tableHtml;

        } catch (error) {
            console.error("Erro ao buscar histórico do equipamento:", error);
            showToast(`Não foi possível encontrar o histórico para o equipamento ID ${id}.`, 'Erro', true);
            resultadoEquipamento.innerHTML = `<p class="text-danger">Equipamento não encontrado ou erro na busca.</p>`;
        }
    }

    // --- Lógica para Histórico de Solicitação ---
    async function buscarHistoricoSolicitacao(event) {
        event.preventDefault();
        const id = inputSolicitacaoId.value;
        resultadoSolicitacao.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"><span class="visually-hidden">A carregar...</span></div>';

        try {
            const response = await apiClient.get(`/solicitacoes/${id}/historico`);
            const historico = response.data;

            if (historico.length === 0) {
                resultadoSolicitacao.innerHTML = '<p class="text-info">Nenhum histórico de status encontrado para esta solicitação.</p>';
                return;
            }

            let listHtml = '<ul class="list-group">';
            historico.forEach(h => {
                listHtml += `
                    <li class="list-group-item">
                        Em ${new Date(h.dataAlteracao).toLocaleString('pt-BR')}, o status mudou de
                        <strong>${h.statusAnterior}</strong> para <strong>${h.statusNovo}</strong>.
                        (Responsável: ${h.usuarioResponsavel})
                    </li>
                `;
            });
            listHtml += '</ul>';
            resultadoSolicitacao.innerHTML = listHtml;

        } catch (error) {
            console.error("Erro ao buscar histórico da solicitação:", error);
            showToast(`Não foi possível encontrar o histórico para a solicitação ID ${id}.`, 'Erro', true);
            resultadoSolicitacao.innerHTML = `<p class="text-danger">Solicitação não encontrada ou erro na busca.</p>`;
        }
    }


    // --- Event Listeners ---
    formBuscaEquipamento.addEventListener('submit', buscarHistoricoEquipamento);
    formBuscaSolicitacao.addEventListener('submit', buscarHistoricoSolicitacao);

})();