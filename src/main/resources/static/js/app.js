// Este é o nosso ficheiro app.js
const BASE_URL = '/cepra';
// --- A "Guarda" de Segurança ---
// 1. Pega no token que guardámos no localStorage
const token = localStorage.getItem('authToken');

// 2. Se NÃO houver token, redireciona imediatamente para a página de login
if (!token) {
    window.location.href = BASE_URL + '/login.html';
} else {
    // 3. Se HÁ um token, configura o Axios para o usar em todas as futuras requisições
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}

// --- Lógica da Página ---
document.addEventListener('DOMContentLoaded', function() {

    const nomeUsuarioSpan = document.getElementById('nome-usuario');
    const btnLogout = document.getElementById('btn-logout');

    // Função para buscar os dados do perfil do utilizador
    async function carregarPerfil() {
        try {
            // Usa o Axios (já configurado com o token) para chamar o endpoint /perfil
            const response = await axios.get('/perfil');

            // Mostra o nome do utilizador na página
            nomeUsuarioSpan.textContent = response.data.nome;

        } catch (error) {
            console.error("Erro ao carregar o perfil:", error.response);
            // Se o token for inválido/expirado, a API dará erro. Então, fazemos logout.
            fazerLogout();
        }
    }

    // Função para fazer logout
    function fazerLogout() {
        // 1. Apaga o token do localStorage
        localStorage.removeItem('authToken');
        // 2. Redireciona para a página de login
        window.location.href = API_CONTEXT + '/login.html';
    }

    // Adiciona o "ouvinte" para o clique no botão de logout
    btnLogout.addEventListener('click', fazerLogout);

    // Carrega os dados do perfil assim que a página estiver pronta
    carregarPerfil();
});