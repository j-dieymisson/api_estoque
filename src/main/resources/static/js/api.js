// api.js

// Cria uma instância central do Axios com configurações padrão
const apiClient = axios.create({
    baseURL: 'http://localhost:8080', // A URL base da nossa API
});

// --- Interceptor de Requisição ---
// Este código é executado ANTES de cada requisição ser enviada.
apiClient.interceptors.request.use(
    (config) => {
        // Pega no token do localStorage
        const token = localStorage.getItem('authToken');
        // Se o token existir, adiciona-o ao cabeçalho de Authorization
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        // Faz algo com o erro da requisição
        return Promise.reject(error);
    }
);

// --- Interceptor de Resposta ---
// Este código é executado sempre que recebemos uma resposta da API.
apiClient.interceptors.response.use(
    (response) => {
        // Qualquer status code que esteja dentro de 2xx causa a execução desta função
        return response;
    },
    (error) => {
        // Qualquer status code que esteja fora de 2xx causa a execução desta função
        // Verificamos se o erro é de autenticação (401 ou 403)
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.error("Erro de autenticação/autorização. A redirecionar para o login.");
            // Se o token for inválido ou o utilizador não tiver permissão para uma ação específica,
            // a forma mais segura é forçar o logout.
            localStorage.removeItem('authToken');
            window.location.href = '/login.html';
        }
        return Promise.reject(error);
    }
);

// --- Função Global para Notificações (Toasts) ---
function showToast(message, title = 'Notificação', isError = false) {
    const toastElement = document.getElementById('liveToast');
    const toastTitle = document.getElementById('toast-title');
    const toastBody = document.getElementById('toast-body');

    if (toastElement && toastTitle && toastBody) {
        toastTitle.textContent = title;
        toastBody.textContent = message;

        // Adiciona/remove classes de cor com base no tipo de notificação
        if (isError) {
            toastElement.classList.add('bg-danger', 'text-white');
            toastElement.classList.remove('bg-success');
        } else {
            toastElement.classList.add('bg-success', 'text-white');
            toastElement.classList.remove('bg-danger');
        }

        const toast = new bootstrap.Toast(toastElement);
        toast.show();
    }
}