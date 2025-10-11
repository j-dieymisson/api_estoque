// main.js - O 'cérebro' de todas as nossas páginas autenticadas

document.addEventListener('DOMContentLoaded', function() {
    // --- 1. A "Guarda" de Segurança e Configuração do Axios ---
    const token = localStorage.getItem('authToken');
    if (!token) {
        // Se não há token, não há nada a fazer aqui. Redireciona para o login.
        window.location.href = '/login.html';
        return; // Para a execução do script
    }
    // Se há um token, a nossa apiClient (do api.js) já o está a usar por padrão.
    console.log("Utilizador autenticado. A inicializar a aplicação principal.");


    // --- 2. Seletores de Elementos do DOM ---
    const nomeUsuarioSpan = document.getElementById('nome-usuario');
    const mainContentArea = document.getElementById('main-content-area');
    const btnDesktopToggle = document.getElementById('btn-desktop-toggle');
    const sidebar = document.querySelector('.sidebar');
    const btnLogout = document.getElementById('btn-logout');
    const menus = [document.getElementById('menu-principal'), document.getElementById('menu-mobile')];


    // --- 3. Funções Principais ---

    // Função para carregar o conteúdo de uma "página parcial" HTML
    async function loadPage(pageUrl) {
        try {
            console.log(`A carregar a página parcial: /app/partials/${pageUrl}`);
            const response = await fetch(`/app/partials/${pageUrl}`);
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
            mainContentArea.innerHTML = await response.text();
        } catch (error) {
            console.error("Falha ao carregar o conteúdo da página:", error);
            mainContentArea.innerHTML = `<div class="alert alert-danger">Erro ao carregar conteúdo.</div>`;
        }
    }

    // Função para configurar os links de navegação
    function setupMenuNavigation(menuElement) {
        if (!menuElement) return;
        menuElement.addEventListener('click', function(event) {
            const link = event.target.closest('a.nav-link');
            if (link) {
                event.preventDefault();
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

                // Fecha o menu offcanvas (se estiver aberto)
                const offcanvasElement = document.getElementById('sidebar-mobile');
                const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasElement);
                if (offcanvas) offcanvas.hide();
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
            console.error("Erro ao carregar o perfil, a fazer logout.", error);
            fazerLogout();
        }
    }

    // Função de Logout
    function fazerLogout() {
        localStorage.removeItem('authToken');
        window.location.href = '/login.html';
    }


    // --- 4. Inicialização e Event Listeners ---

    // Lógica do botão de colapsar a sidebar no desktop
    if (btnDesktopToggle && sidebar) {
        btnDesktopToggle.addEventListener('click', () => sidebar.classList.toggle('collapsed'));
    }

    // Lógica do botão de logout
    if (btnLogout) {
        btnLogout.addEventListener('click', fazerLogout);
    }

    // Configura os eventos de clique para ambos os menus (desktop e mobile)
    menus.forEach(setupMenuNavigation);

    // Inicia a aplicação
    carregarPerfilEConfigurarUI(); // Busca o perfil para mostrar o nome e ajustar o menu
    loadPage('dashboard.html');     // Carrega a página inicial por defeito
});