// src/main/resources/static/js/login.js

document.addEventListener('DOMContentLoaded', function() {

    // Pega nos elementos do formulário
    const form = document.getElementById('form-login');
    const nomeInput = document.getElementById('nome');
    const senhaInput = document.getElementById('senha');
    const mensagemErro = document.getElementById('mensagem-erro');

    // Adiciona um "ouvinte" para quando o formulário for submetido
    form.addEventListener('submit', async function(event) {
        // Previne o comportamento padrão do formulário (que é recarregar a página)
        event.preventDefault();

        // Esconde a mensagem de erro antiga
        mensagemErro.classList.add('d-none');

        // Pega nos valores dos campos de input
        const nome = nomeInput.value;
        const senha = senhaInput.value;

        console.log(`A tentar fazer login como: ${nome}`);

        try {
            // Usa o Axios para fazer a chamada POST para a nossa API
            const response = await axios.post('/login', {
                nome: nome,
                senha: senha
            });

            // Se a chamada for bem-sucedida (status 200 OK)...
            console.log("Login bem-sucedido!", response.data);

            // 1. Pega no token que a API devolveu
            const token = response.data.token;

            // 2. Guarda o token no localStorage do browser
            localStorage.setItem('authToken', token);

            // 3. Redireciona para a página principal (que ainda não existe)
            alert("Login realizado com sucesso! A redirecionar...");
            window.location.href = '/app/index.html'; // Vamos criar esta página a seguir

        } catch (error) {
            // Se a API devolver um erro (como 401 ou 403)...
            console.error("Erro no login:", error.response);

            // Mostra a mensagem de erro na página
            mensagemErro.classList.remove('d-none');
        }
    });
});