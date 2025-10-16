// main.js - Versão final com carregador de script dinâmico

document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = '/login.html';
        return;
    }
    console.log("Utilizador autenticado. A inicializar a aplicação principal.");

    const nomeUsuarioSpan = document.getElementById('nome-usuario');
    const mainContentArea = document.getElementById('main-content-area');
    const btnDesktopToggle = document.getElementById('btn-desktop-toggle');
    const sidebar = document.querySelector('.sidebar');
    const btnLogout = document.getElementById('btn-logout');
    const menus = [document.getElementById('menu-principal'), document.getElementById('menu-mobile')];
    const confirmModalEl = document.getElementById('confirmModal');
    const confirmModal = new bootstrap.Modal(confirmModalEl);
    const confirmModalTitle = document.getElementById('confirmModalLabel');
    const confirmModalBody = document.getElementById('confirmModalBody');
    const confirmModalButton = document.getElementById('confirmModalButton');

    let currentScript = null;

    window.showConfirmModal = function(title, message, onConfirmCallback) {
            confirmModalTitle.textContent = title;
            confirmModalBody.textContent = message;

            confirmModalButton.onclick = () => {
                onConfirmCallback();
                confirmModal.hide();
            };

            confirmModal.show();
        }

    window.navigateTo = function(pageUrl, context = {}) {
            // Guarda o 'contexto' (como o ID que queremos passar) numa variável global temporária
            window.pageContext = context;

            // Simula um clique no link do menu correspondente para reutilizar a nossa lógica
            const link = document.querySelector(`#menu-principal a[data-page='${pageUrl}']`);
            if (link) {
                link.click();
            } else {
                // Se não houver link no menu, carrega a página diretamente
                loadPage(pageUrl);
                marcarLinkAtivo(pageUrl);
            }
        }

    async function loadPage(pageUrl) {
        try {
            if (!pageUrl || pageUrl === '#') {
                mainContentArea.innerHTML = `<div class="alert alert-secondary">Selecione uma opção no menu.</div>`;
                return;
            }
            console.log(`A carregar a página parcial: ${pageUrl}`);
            const response = await fetch(`/app/partials/${pageUrl}`);
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);

            mainContentArea.innerHTML = await response.text();

            const scriptUrl = `/js/pages/${pageUrl.replace('.html', '.js')}`;
            if (currentScript) {
                document.body.removeChild(currentScript);
            }
            currentScript = document.createElement('script');
            currentScript.src = scriptUrl;
            currentScript.onerror = () => console.error(`Erro ao carregar o script: ${scriptUrl}`);
            document.body.appendChild(currentScript);

        } catch (error) {
            console.error("Falha ao carregar o conteúdo da página:", error);
            mainContentArea.innerHTML = `<div class="alert alert-danger">Erro ao carregar conteúdo.</div>`;
        }
    }

    function setupMenuNavigation(menuElement) {
        if (!menuElement) return;
        menuElement.addEventListener('click', function(event) {
            const link = event.target.closest('a.nav-link');
            if (link) {
                event.preventDefault();
                const pageToLoad = link.getAttribute('data-page');
                marcarLinkAtivo(pageToLoad);
                if (pageToLoad) loadPage(pageToLoad);

                const offcanvasElement = document.getElementById('sidebar-mobile');
                if (offcanvasElement) {
                    const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasElement);
                    if (offcanvas) offcanvas.hide();
                }
            }
        });
    }

    async function carregarPerfilEConfigurarUI() {
        try {
            const response = await apiClient.get('/perfil');
            const usuario = response.data;

            if (nomeUsuarioSpan) nomeUsuarioSpan.textContent = usuario.nome;
            const cargo = usuario.nomeCargo;
            if (cargo !== 'ADMIN') {
                document.querySelectorAll('.admin-only').forEach(item => {
                    item.style.display = 'none';
                });
            }

            let paginaInicial = 'solicitacoes.html';
            if (cargo === 'ADMIN' || cargo === 'GESTOR') {
                paginaInicial = 'dashboard.html';
            }

            loadPage(paginaInicial);
            marcarLinkAtivo(paginaInicial);

        } catch (error) {
            console.error("Erro ao carregar o perfil, a fazer logout.", error);
            fazerLogout();
        }
    }

    function marcarLinkAtivo(pageUrl) {
        menus.forEach(menu => {
            if (menu) {
                menu.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
                const linkAtivo = menu.querySelector(`a[data-page="${pageUrl}"]`);
                if (linkAtivo) linkAtivo.classList.add('active');
            }
        });
    }

    function fazerLogout() {
        localStorage.removeItem('authToken');
        window.location.href = '/login.html';
    }

    if (btnDesktopToggle && sidebar) {
        btnDesktopToggle.addEventListener('click', () => sidebar.classList.toggle('collapsed'));
    }
    if (btnLogout) {
        btnLogout.addEventListener('click', fazerLogout);
    }

    const btnLogoutMobile = document.getElementById('btn-logout-mobile');
        if (btnLogoutMobile) {
            btnLogoutMobile.addEventListener('click', fazerLogout);
        }
    menus.forEach(setupMenuNavigation);

    carregarPerfilEConfigurarUI();
})();