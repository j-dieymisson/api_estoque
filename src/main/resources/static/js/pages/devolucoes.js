// devolucoes.js - Lógica para a página de histórico de devoluções

(async function() {

    const corpoTabela = document.getElementById('corpo-tabela-devolucoes');

    if (!corpoTabela) {
        console.error("Elemento da tabela de devoluções não encontrado!");
        return;
    }

    // Função para buscar e renderizar as devoluções na tabela
    async function carregarDevolucoes(page = 0, size = 10) {
        corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center">A carregar...</td></tr>';
        try {
            const params = { page, size, sort: 'dataDevolucao,desc' };
            // Usa a apiClient para buscar a lista paginada de devoluções
            const response = await apiClient.get('/devolucoes', { params });
            const devolucoes = response.data.content;

            corpoTabela.innerHTML = ''; // Limpa a tabela

            if (devolucoes.length === 0) {
                corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhuma devolução registada.</td></tr>';
                return;
            }

            devolucoes.forEach(dev => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${dev.id}</td>
                    <td>${dev.solicitacaoId}</td>
                    <td>${dev.nomeEquipamento}</td>
                    <td>${dev.quantidadeDevolvida}</td>
                    <td>${new Date(dev.dataDevolucao).toLocaleString('pt-BR')}</td>
                    <td>${dev.observacao || ''}</td>
                `;
                corpoTabela.appendChild(tr);
            });

            // Adicionar lógica de paginação aqui no futuro
        } catch (error) {
            console.error("Erro ao carregar devoluções:", error);
            showToast("Não foi possível carregar o histórico de devoluções.", "Erro", true);
            corpoTabela.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Falha ao carregar devoluções.</td></tr>';
        }
    }

    // --- Inicialização ---
    carregarDevolucoes();

})();