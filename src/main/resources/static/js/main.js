// Este é o 'cérebro' de todas as nossas páginas autenticadas

// --- A "Guarda" de Segurança (Executa imediatamente) ---
const token = localStorage.getItem('authToken');
if (!token) {
    // Se não há token, redireciona para a página de login
    window.location.href = '/login.html';
} else {
    // Se há um token, configura o Axios para o usar por padrão em todas as requisições
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}

// --- Lógica Principal (Executa depois de o HTML estar completamente carregado) ---
document.addEventListener('DOMContentLoaded', function() {
    console.log("Página privada carregada. A inicializar...");
    
    // --- Seletores de Elementos do DOM ---
    const nomeUsuarioSpan = document.getElementById('nome-usuario');
    const mainContentArea = document.getElementById('main-content-area');
    const btnDesktopToggle = document.getElementById('btn-desktop-toggle');
    const sidebar = document.querySelector('.sidebar');
    const btnLogout = document.getElementById('btn-logout');
    // Seleciona ambos os menus (desktop e mobile)
    const menus = [document.getElementById('menu-principal'), document.getElementById('menu-mobile')];

    // --- Função para Carregar Conteúdo de "Partials" HTML ---
    async function loadPage(pageUrl) {
        try {
            console.log(`A carregar a página parcial: ${pageUrl}`);
            const response = await fetch(`/app/partials/${pageUrl}`);
            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }
            mainContentArea.innerHTML = await response.text();
        } catch (error) {
            console.error("Falha ao carregar o conteúdo da página:", error);
            mainContentArea.innerHTML = `<div class="alert alert-danger">Erro ao carregar conteúdo. Tente navegar para outra página.</div>`;
        }
    }

    // --- Função para Configurar a Navegação dos Menus ---
    function setupMenuNavigation(menuElement) {
        if (!menuElement) return;

        menuElement.addEventListener('click', function(event) {
            const link = event.target.closest('a.nav-link');
            if (link) {
                event.preventDefault(); // Impede a navegação padrão do link

                // Remove a classe 'active' de todos os links em AMBOS os menus
                menus.forEach(menu => {
                    menu.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
                });
                
                const pageToLoad = link.getAttribute('data-page');
                
                // Adiciona a classe 'active' aos links correspondentes em AMBOS os menus
                 menus.forEach(menu => {
                    const correspondingLink = menu.querySelector(`a[data-page="${pageToLoad}"]`);
                    if (correspondingLink) {
                        correspondingLink.classList.add('active');
                    }
                });

                if (pageToLoad) {
                    loadPage(pageToLoad);
                }

                // Fecha o menu offcanvas no mobile após o clique
                const offcanvasElement = document.getElementById('sidebar-mobile');
                const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasElement);
                if (offcanvas) {
                    offcanvas.hide();
                }
            }
        });
    }

    // --- Lógica de Controlo da Sidebar ---
    if (btnDesktopToggle && sidebar) {
        btnDesktopToggle.addEventListener('click', function() {
            sidebar.classList.toggle('collapsed');
        });
    }

    // --- Funções de Utilizador ---
    async function carregarPerfil() {
        try {
            const response = await axios.get('/perfil');
            const nomeUsuario = response.data.nome;
            if (nomeUsuarioSpan) {
                nomeUsuarioSpan.textContent = nomeUsuario;
            }
        } catch (error) {
            console.error("Erro ao carregar o perfil:", error);
            if (error.response && (error.response.status === 401 || error.response.status === 403)) {
                fazerLogout(); // Se o token for inválido, faz logout
            }
        }
    }

    function fazerLogout() {
        console.log("A fazer logout...");
        localStorage.removeItem('authToken');
        window.location.href = '/login.html';
    }

    // --- Inicialização da Página ---
    if (btnLogout) {
        btnLogout.addEventListener('click', fazerLogout);
    }
    menus.forEach(setupMenuNavigation); // Configura os eventos de clique para ambos os menus
    
    carregarPerfil(); // Busca os dados do perfil do utilizador
    loadPage('dashboard.html'); // Carrega a página inicial por defeito
});