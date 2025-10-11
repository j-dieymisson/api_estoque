// main.js - O 'cérebro' de todas as nossas páginas autenticadas

// --- 1. A "Guarda" de Segurança e Configuração (Executa imediatamente) ---
const token = localStorage.getItem('authToken');
if (!token) {
    // Se não há token, não há nada a fazer aqui. Redireciona para o login.
    window.location.href = '/login.html';
}
// Se há um token, a nossa apiClient (do api.js) já está configurada para o usar,
// graças ao interceptor que criámos.

// --- 2. Lógica Principal (Executa depois de o HTML estar completamente carregado) ---
document.addEventListener('DOMContentLoaded', function() {
    console.log("Página privada carregada. A inicializar a aplicação principal.");

    // --- Seletores de Elementos do DOM ---
    const nomeUsuarioSpan = document.getElementById('nome-usuario');
    const mainContentArea = document.getElementById('main-content-area');
    const btnDesktopToggle = document.getElementById('btn-desktop-toggle');
    const sidebar = document.querySelector('.sidebar');
    const btnLogout = document.getElementById('btn-logout');
    const menus = [document.getElementById('menu-principal'), document.getElementById('menu-mobile')];

    // --- Funções Principais ---

    // Função para carregar o conteúdo de uma "página parcial" HTML
    async function loadPage(pageUrl) {
        try {
            // Verifica se um URL foi fornecido
            if (!pageUrl || pageUrl === '#') {
                mainContentArea.innerHTML = `<div class="alert alert-secondary">Selecione uma opção no menu.</div>`;
                return;
            }
            console.log(`A carregar a página parcial: /app/partials/${pageUrl}`);
            // Usa o fetch API do browser para buscar o conteúdo HTML
            const response = await fetch(`/app/partials/${pageUrl}`);
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
            // Injeta o HTML recebido na área de conteúdo principal
            mainContentArea.innerHTML = await response.text();
        } catch (error) {
            console.error("Falha ao carregar o conteúdo da página:", error);
            mainContentArea.innerHTML = `<div class="alert alert-danger">Erro ao carregar conteúdo. Tente novamente.</div>`;
        }
    }

    // Função para configurar os links de navegação
    function setupMenuNavigation(menuElement) {
        if (!menuElement) return;
        menuElement.addEventListener('click', function(event) {
            const link = event.target.closest('a.nav-link');
            if (link) {
                event.preventDefault(); // Impede a navegação padrão do link
                const pageToLoad = link.getAttribute('data-page');

                // Sincroniza o estado 'active' em ambos os menus
                menus.forEach(menu => {
                    if (menu) {
                        menu.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
                        const correspondingLink = menu.querySelector(`a[data-page="${pageToLoad}"]`);
                        if (correspondingLink) correspondingLink.classList.add('active');
                    }
                });

                if (pageToLoad) loadPage(pageToLoad);

                // Fecha o menu offcanvas (se estiver aberto) no mobile após o clique
                const offcanvasElement = document.getElementById('sidebar-mobile');
                if (offcanvasElement) {
                    const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasElement);
                    if (offcanvas) offcanvas.hide();
                }
            }
        });
    }

    // Função para buscar dados do perfil e configurar a UI de acordo
    async function carregarPerfilEConfigurarUI() {
        try {
            const response = await apiClient.get('/perfil');
            const usuario = response.data;

            if (nomeUsuarioSpan) nomeUsuarioSpan.textContent = usuario.nome;

            // Lógica de visibilidade do menu baseada no cargo
            if (usuario.nomeCargo !== 'ADMIN') {
                document.querySelectorAll('.admin-only').forEach(item => {
                    item.style.display = 'none';
                });
            }
        } catch (error) {
            // O interceptor do api.js já trata o logout em caso de erro 401/403
            console.error("Erro ao carregar o perfil:", error);
            showToast('Não foi possível carregar os dados do seu perfil.', 'Erro', true);
        }
    }

    // Função de Logout
    function fazerLogout() {
        showToast('A terminar a sessão...', 'Logout');
        setTimeout(() => {
            localStorage.removeItem('authToken');
            window.location.href = '/login.html';
        }, 1000);
    }


    // --- 4. Inicialização da Página e Event Listeners ---
    if (btnDesktopToggle && sidebar) {
        btnDesktopToggle.addEventListener('click', () => sidebar.classList.toggle('collapsed'));
    }

    if (btnLogout) {
        btnLogout.addEventListener('click', fazerLogout);
    }

    menus.forEach(setupMenuNavigation);

    carregarPerfilEConfigurarUI();
    loadPage('dashboard.html');
});