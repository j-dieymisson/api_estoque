// auth.js - Lógica exclusiva da página de login.

// O código dentro disto só corre depois de a página HTML estar completamente carregada.
document.addEventListener('DOMContentLoaded', function() {

    // Verifica se já existe um token. Se sim, o utilizador já está logado.
    // Redireciona-o para a página principal para não o forçar a fazer login novamente.
    if (localStorage.getItem('authToken')) {
        window.location.href = '/app/index.html';
        return; // Para a execução do script
    }

    const form = document.getElementById('form-login');
    const nomeInput = document.getElementById('nome');
    const senhaInput = document.getElementById('senha');

    if (form) {
        form.addEventListener('submit', async function(event) {
            // Previne o recarregamento da página
            event.preventDefault();

            const nome = nomeInput.value;
            const senha = senhaInput.value;

            console.log(`A tentar fazer login como: ${nome}`);

            try {
                // Usa a nossa apiClient (do ficheiro api.js) para fazer a chamada POST
                const response = await apiClient.post('/login', {
                    nome: nome,
                    senha: senha
                });

                console.log("Login bem-sucedido!", response.data);

                const token = response.data.token;

                // Guarda o token no localStorage do browser
                localStorage.setItem('authToken', token);

                // Mostra uma notificação de sucesso com o nosso novo Toast
                showToast('Login realizado com sucesso! A redirecionar...', 'Sucesso');

                // Espera um pouco para o utilizador ver o toast antes de redirecionar
                setTimeout(() => {
                    window.location.href = '/app/index.html';
                }, 1000); // 1 segundo

            } catch (error) {
                console.error("Erro no login:", error.response);
                // Mostra uma notificação de erro com o nosso Toast
                showToast('Nome de utilizador ou senha inválidos.', 'Erro de Login', true);
            }
        });
    }
});