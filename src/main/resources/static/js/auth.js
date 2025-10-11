// auth.js - Lógica exclusiva da página de login.

// O código dentro disto só corre depois de a página HTML estar completamente carregada.
document.addEventListener('DOMContentLoaded', function() {

    // Se um utilizador já logado acidentalmente for para a página de login,
    // redireciona-o para a página principal.
    if (localStorage.getItem('authToken')) {
        window.location.href = '/app/index.html';
        return;
    }

    const form = document.getElementById('form-login');
    const nomeInput = document.getElementById('nome');
    const senhaInput = document.getElementById('senha');
    const loginButton = form.querySelector('button[type="submit"]');
    const spinner = loginButton.querySelector('.spinner-border');

    if (form) {
        form.addEventListener('submit', async function(event) {
            event.preventDefault();

            // Ativa o spinner e desativa o botão para evitar cliques múltiplos
            loginButton.disabled = true;
            spinner.classList.remove('d-none');

            const nome = nomeInput.value;
            const senha = senhaInput.value;

            try {
                // Usa a nossa apiClient (do ficheiro api.js) para fazer a chamada POST
                const response = await apiClient.post('/login', {
                    nome: nome,
                    senha: senha
                });

                const token = response.data.token;

                // Guarda o token no localStorage do browser
                localStorage.setItem('authToken', token);

                // Mostra uma notificação de sucesso
                showToast('Login realizado com sucesso! A redirecionar...', 'Sucesso');

                // Espera um pouco para o utilizador ver o toast antes de redirecionar
                setTimeout(() => {
                    window.location.href = '/app/index.html';
                }, 1000);

            } catch (error) {
                console.error("Erro no login:", error.response);
                // Mostra uma notificação de erro
                showToast('Nome de utilizador ou senha inválidos.', 'Erro de Login', true);

                // Reativa o botão em caso de erro
                loginButton.disabled = false;
                spinner.classList.add('d-none');
            }
        });
    }
});