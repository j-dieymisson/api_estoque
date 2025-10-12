// consultas.js - Versão refatorada, apenas para navegação

(function() {
    console.log("A executar o script da página de consultas...");

    // --- Seletores da Ferramenta 1: Rastrear Equipamento ---
    const formBuscaEquipamento = document.getElementById('form-busca-equipamento');
    const inputBuscaEquipamento = document.getElementById('equipamento-id-busca');

    // --- Seletores da Ferramenta 2: Consultar Solicitações por Funcionário ---
    const formBuscaSolicitacaoUsuario = document.getElementById('form-busca-solicitacao-usuario');
    const inputBuscaUsuarioId = document.getElementById('usuario-id-busca');


    // --- Lógica da Ferramenta 1 ---
    function navegarParaHistoricoEquipamento(event) {
        event.preventDefault();
        const id = inputBuscaEquipamento.value;
        if (id) {
            // Chama a nossa função global para navegar para a nova página de resultados
            window.navigateTo('consulta-equipamento.html', { id: id });
        }
    }

    // --- Lógica da Ferramenta 2 ---
    function navegarParaSolicitacoesUsuario(event) {
        event.preventDefault();
        const id = inputBuscaUsuarioId.value;
        if (id) {
            // Chama a nossa função global para navegar para a nova página de resultados
            window.navigateTo('consulta-usuario.html', { id: id });
        }
    }

    // --- Event Listeners ---
    if (formBuscaEquipamento) {
        formBuscaEquipamento.addEventListener('submit', navegarParaHistoricoEquipamento);
    }
    if (formBuscaSolicitacaoUsuario) {
        formBuscaSolicitacaoUsuario.addEventListener('submit', navegarParaSolicitacoesUsuario);
    }

})();