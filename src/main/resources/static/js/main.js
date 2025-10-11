// src/main/resources/static/js/main.js

// --- A "Guarda" de Segurança ---
const token = localStorage.getItem('authToken');

// Se NÃO houver token, não há nada a fazer aqui. Redireciona para o login.
if (!token) {
    // Redireciona para a página de login, que está na raiz
    window.location.href = '/login.html';
} else {
    // Se HÁ um token, configura o Axios para o usar por padrão em todas as requisições
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}

// Lógica que corre depois de a página estar carregada
document.addEventListener('DOMContentLoaded', function() {
    // Por agora, não faz nada, mas aqui adicionaremos a lógica do dashboard, etc.
    console.log("Página privada carregada com sucesso. Token encontrado.");
});