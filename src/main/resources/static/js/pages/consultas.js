// consultas.js - Apenas para navegação
(function() {
    console.log("A executar o script da página de consultas...");

    const formBuscaEquipamento = document.getElementById('form-busca-equipamento');
    const inputBuscaEquipamento = document.getElementById('equipamento-id-busca');

    const formBuscaSolicitacaoUsuario = document.getElementById('form-busca-solicitacao-usuario');
    const inputBuscaUsuarioId = document.getElementById('usuario-id-busca');

    function navegarParaHistoricoEquipamento(event) {
        event.preventDefault();
        const id = inputBuscaEquipamento.value;
        if (id) {
            window.navigateTo('consulta-equipamento.html', { id: id });
        }
    }

    function navegarParaSolicitacoesUsuario(event) {
        event.preventDefault();
        const id = inputBuscaUsuarioId.value;
        if (id) {
            window.navigateTo('consulta-usuario.html', { id: id });
        }
    }

    if (formBuscaEquipamento) formBuscaEquipamento.addEventListener('submit', navegarParaHistoricoEquipamento);
    if (formBuscaSolicitacaoUsuario) formBuscaSolicitacaoUsuario.addEventListener('submit', navegarParaSolicitacoesUsuario);
})();