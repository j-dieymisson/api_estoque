// auth.js - Versão final e robusta
document.addEventListener('DOMContentLoaded', function() {

    if (localStorage.getItem('authToken')) {
        window.location.href = '/cepra/app/index.html';
        return;
    }

    const form = document.getElementById('form-login');

    //Lógica de toggle-senha
    const senhaInput = document.getElementById('senha');
        const toggleSenha = document.getElementById('toggle-senha');

        if (toggleSenha && senhaInput) {
            toggleSenha.addEventListener('click', function() {
                // Alterna o tipo do input de 'password' para 'text' e vice-versa
                const type = senhaInput.getAttribute('type') === 'password' ? 'text' : 'password';
                senhaInput.setAttribute('type', type);

                // Alterna o ícone do olho
                const icon = this.querySelector('i');
                if (type === 'password') {
                    icon.classList.remove('bi-eye-slash-fill');
                    icon.classList.add('bi-eye-fill');
                } else {
                    icon.classList.remove('bi-eye-fill');
                    icon.classList.add('bi-eye-slash-fill');
                }
            });
        }

    if (form) {
        form.addEventListener('submit', async function(event) {
            event.preventDefault();

            // Seletores de elementos feitos no momento do submit para garantir que existem
            const nomeInput = document.getElementById('nome');
            const senhaInput = document.getElementById('senha');
            const mensagemErro = document.getElementById('mensagem-erro');
            const loginButton = form.querySelector('button[type="submit"]');
            const spinner = loginButton.querySelector('.spinner-border');

            mensagemErro.classList.add('d-none');
            loginButton.disabled = true;
            spinner.classList.remove('d-none');

            const nome = nomeInput.value;
            const senha = senhaInput.value;

            try {
                const response = await apiClient.post('/login', { nome, senha });
                localStorage.setItem('authToken', response.data.token);
                window.location.href = '/cepra/app/index.html';
            } catch (error) {
                console.error("Erro no login:", error);

                mensagemErro.textContent = 'Nome de utilizador ou senha inválidos.';
                mensagemErro.classList.remove('d-none');

                loginButton.disabled = false;
                spinner.classList.add('d-none');
            }
        });
    }
});