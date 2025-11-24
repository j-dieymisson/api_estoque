// main.js - Versão final com carregador de script dinâmico
const BASE_URL = '/cepra';

document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = BASE_URL + '/login.html';
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
    let navigationHistory = [];
    let currentUserRole = null;

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
            // Passo 1: Guarda o contexto (como o ID)
            window.pageContext = context;

            // Passo 2: Adiciona a nova página à nossa "memória"
            navigationHistory.push({ pageUrl, context });

            // Passo 3: Faz as duas coisas que o seu código antigo fazia
            marcarLinkAtivo(pageUrl);
            loadPage(pageUrl);
        }

    window.navigateBack = function() {
            // Remove a página atual da nossa memória
            navigationHistory.pop();

            // Pega na página anterior que sobrou
            const previousState = navigationHistory[navigationHistory.length - 1];

            if (previousState) {
                // Navega para a página anterior, restaurando o seu contexto
                window.pageContext = previousState.context;
                loadPage(previousState.pageUrl); // Carrega a página sem adicionar ao histórico novamente
                marcarLinkAtivo(previousState.pageUrl);
            } else {
                // Se não houver mais nada no histórico, volta para a página inicial
                carregarPerfilEConfigurarUI();
            }
        }

        /**
             * Atualiza o 'context' da página ATUAL no histórico de navegação.
             * Isto é usado para guardar o estado (ex: página de paginação, filtros)
             * sem navegar para uma nova página.
             */
    window.updateCurrentHistoryContext = function(newContextData) {
        if (navigationHistory.length > 0) {
            // Pega na página atual (a última do array)
            const currentState = navigationHistory[navigationHistory.length - 1];

            // Funde o contexto antigo com os novos dados
            // (ex: { id: 123 } + { page: 3 } = { id: 123, page: 3 })
            currentState.context = { ...currentState.context, ...newContextData };

            // Atualiza também o contexto global
            window.pageContext = currentState.context;
        }
    }

    async function loadPage(pageUrl) {
        try {
            if (!pageUrl || pageUrl === '#') {
                mainContentArea.innerHTML = `<div class="alert alert-secondary">Selecione uma opção no menu.</div>`;
                return;
            }
            console.log(`A carregar a página parcial: ${pageUrl}`);
            // 1. Garante que removemos o .html se ele já vier no nome
                        const nomePaginaLimpo = pageUrl.replace('.html', '');

                        // 2. Adicionamos o .html manualmente UMA VEZ
                        const response = await fetch(`${BASE_URL}/app/partials/${nomePaginaLimpo}.html`);

                        if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);

            mainContentArea.innerHTML = await response.text();

            aplicarPermissoesUI();

           // 3. CORREÇÃO: Usamos o nome limpo para buscar o JS
                      const scriptUrl = `${BASE_URL}/js/pages/${nomePaginaLimpo}.js`;

                       if (currentScript) {
                           document.body.removeChild(currentScript);
                       }
                       currentScript = document.createElement('script');
                       currentScript.src = scriptUrl;
                       currentScript.onerror = () => console.error(`Erro ao carregar o script: ${scriptUrl}`);
                       document.body.appendChild(currentScript);

                       window.atualizarNotificacaoPendentes();

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

                    // 1. Limpa o histórico de navegação anterior
                    navigationHistory = [];

                    // 2. Chama a nossa função "super-herói" que faz tudo
                    window.navigateTo(pageToLoad);

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

                                currentUserRole = usuario.nomeCargo;


                                aplicarPermissoesUI();


                let paginaInicial = 'solicitacoes.html';

                if (currentUserRole === 'ADMIN' ) {
                    paginaInicial = 'dashboard.html';
                    window.atualizarNotificacaoPendentes();
                }

                if (currentUserRole === 'GESTOR'){
                    paginaInicial = 'equipamentos.html';
                }

                // Usamos a nossa função de navegação para carregar a página inicial
                window.navigateTo(paginaInicial);

            } catch (error) {
                console.error("Erro ao carregar o perfil, a fazer logout.", error);
                fazerLogout();
            }
        }

    function aplicarPermissoesUI() {
                    // Se o cargo ainda não foi carregado, não faz nada
                    if (!currentUserRole) return;

                    const cargo = currentUserRole;

                    // Esta função agora aplica permissões a TUDO (sidebar E conteúdo)
                    document.querySelectorAll('.admin-only, .gestor-only').forEach(item => {
                        item.style.display = ''; // Reseta (mostra) tudo
                    });

                    if (cargo === 'COLABORADOR') {
                        document.querySelectorAll('.admin-only, .gestor-only').forEach(item => {
                            item.style.display = 'none';
                        });
                    } else if (cargo === 'GESTOR') {
                        document.querySelectorAll('.admin-only:not(.gestor-only)').forEach(item => {
                             item.style.display = 'none';
                        });
                    }
                    // Se for ADMIN, não escondemos nada.
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
        window.location.href = BASE_URL + '/login.html';
    }

    // --- LÓGICA DE NOTIFICAÇÃO (AGORA GLOBAL E UNIFICADA) ---

      window.atualizarNotificacaoPendentes = async function() {
                  if (currentUserRole !== 'ADMIN' && currentUserRole !== 'GESTOR') {
                    mostrarNotificacao(false);
                    return;
                  }

                  try {
                      // 1. Chamamos o novo endpoint leve (isto está correto)
                      const response = await apiClient.get('/solicitacoes/pendentes/contagem', {
                      params: {
                          _: new Date().getTime() // Adiciona um carimbo de data/hora (ex: ?_=123456789)
                      }
                    });

                     // 2. Devemos ler a resposta (ex: { contagem: 3 })
                      const dadosContagem = response.data;


                    // 3. Pega a contagem (ou 0 se for nulo)
                   const contagem = dadosContagem?.contagem || 0;

                   // 2. Passa a contagem para a função 'mostrarNotificacao'
                   if (contagem > 0) {
                       mostrarNotificacao(true, contagem);
                   } else {
                       mostrarNotificacao(false, 0);
                   }
                  } catch (error) {
                      console.error("Erro ao verificar solicitações pendentes:", error);
                  }
      }
        // Esta função agora aceita a contagem e escreve o número
        function mostrarNotificacao(show, contagem = 0) { // <-- 1. Aceita a contagem
            const notificacoes = document.querySelectorAll('.notificacao-pendentes');
            notificacoes.forEach(notificacao => {
                if (show) {
                    // 2. Escreve o número dentro do badge
                    notificacao.textContent = contagem;
                    notificacao.classList.remove('d-none');
                } else {
                    // 3. Limpa o texto e esconde
                    notificacao.textContent = '';
                    notificacao.classList.add('d-none');
                }
            });
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
});