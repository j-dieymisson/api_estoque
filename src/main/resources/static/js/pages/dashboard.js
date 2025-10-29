// dashboard.js - Lógica específica para a página de dashboard

// Usamos uma função anónima auto-executável (IIFE) para não poluir o escopo global
// e podermos usar async/await no nível superior.
(async function() {
    console.log("A executar o script do dashboard...");

    const widgetContainer = document.getElementById('dashboard-widgets-container');
    const checkboxesContainer = document.getElementById('widgets-checkboxes-container');
    const formPreferencias = document.getElementById('form-preferencias');
    const modalPreferenciasEl = document.getElementById('modal-preferencias');
    const modalPreferencias = new bootstrap.Modal(modalPreferenciasEl);
    const feedAtividadesContainer = document.getElementById('feed-atividades');

    if (!widgetContainer || !checkboxesContainer || !formPreferencias) {
        console.error("Um ou mais elementos essenciais do dashboard não foram encontrados!");
        return;
    }

    // Mapeamento de chaves da API para texto e ícones (para deixar os cartões mais bonitos)
    const widgetDetails = {
        totalUsuariosAtivos: { title: "Utilizadores Ativos", icon: "bi-people-fill" },
        solicitacoesPendentes: { title: "Solicitações Pendentes", icon: "bi-hourglass-split" },
        solicitacoesAprovadasHoje: { title: "Aprovadas Hoje", icon: "bi-check-circle-fill" },
        totalUnidadesEmUso: { title: "Unidades em Uso", icon: "bi-box-arrow-up-right" },
        totalTiposDeEquipamento: { title: "Tipos de Equipamento", icon: "bi-pc-display" },
        tiposDeEquipamentoAtivos: { title: "Equipamentos Ativos", icon: "bi-power" },
        totalUnidadesCadastradas: { title: "Total de Unidades", icon: "bi-box-seam" },
        solicitacoesFinalizadasMes: { title: "Finalizadas no Mês", icon: "bi-calendar2-check" },
        solicitacoesTotais: { title: "Total de Solicitações", icon: "bi-card-list" },
        totalCategorias: { title: "Total de Categorias", icon: "bi-tags-fill" },
    };

    function formatWidgetTitle(key) {
        // 1. Coloca espaço antes de letras maiúsculas (exceto a primeira)
        let formatted = key.replace(/([A-Z])/g, ' $1');
        // 2. Coloca a primeira letra em maiúscula
        formatted = formatted.charAt(0).toUpperCase() + formatted.slice(1);
            // 3. Remove espaços extras que podem surgir no início
        return formatted.trim();
    }

    // Função para criar o HTML de um "card" de estatística
    function createWidgetCard(key, value) {
        const details = widgetDetails[key] || { title: formatWidgetTitle(key), icon: "bi-question-circle" };
        return `
            <div class="col-xl-3 col-md-6 mb-4">
                <div class="card border-start-primary shadow h-100 py-2">
                    <div class="card-body">
                        <div class="row no-gutters align-items-center">
                            <div class="col me-2">
                                <div class="text-xs font-weight-bold text-gray text-uppercase mb-1">${details.title}</div>
                                <div class="h5 mb-0 font-weight-bold text-gray-800">${value}</div>
                            </div>
                            <div class="col-auto">
                                <i class="bi ${details.icon} h2 text-gray-300"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    // Formata uma entrada de histórico para uma string legível
        function formatarEntradaFeed(atividade) {
            const data = new Date(atividade.data).toLocaleString('pt-BR');
            const descricao = atividade.descricao;
            let icon = 'bi-bell-fill';

            // Define o ícone com base no tipo
            if (atividade.tipo === 'SOLICITACAO_PENDENTE') {
                icon = 'bi-file-earmark-plus-fill text-secondary';
            } else if (atividade.tipo === 'SAIDA') {
                icon = 'bi-arrow-up-right-square-fill text-secondary';
            } else if (atividade.tipo === 'DEVOLUCAO') {
                icon = 'bi-arrow-down-left-square-fill text';
            }

            // Retorna o HTML completo para o item da lista
            return `<li class="list-group-item d-flex align-items-center"><i class="bi ${icon} h5 me-3"></i><div>${descricao}<div class="text-muted small">${data}</div></div></li>`;
        }

      // Busca os dados da API e renderiza o feed
          async function carregarFeedDeAtividades() {
              if (!feedAtividadesContainer) return;

              // Adiciona o seletor para o novo botão de atualizar
              const btnAtualizarFeed = document.getElementById('btn-atualizar-feed');
              if(btnAtualizarFeed) {
                  btnAtualizarFeed.addEventListener('click', carregarFeedDeAtividades);
              }

              feedAtividadesContainer.innerHTML = '<li class="list-group-item text-center">A carregar atividades...</li>';
              try {

                  const response = await apiClient.get('/dashboard/feed-atividades');

                  const atividades = response.data;
                  feedAtividadesContainer.innerHTML = ''; // Limpa o "loading"

                  if (atividades.length === 0) {
                      feedAtividadesContainer.innerHTML = '<li class="list-group-item">Nenhuma atividade recente.</li>';
                      return;
                  }

                  atividades.forEach(atividade => {
                      feedAtividadesContainer.innerHTML += formatarEntradaFeed(atividade);
                  });

              } catch (error) {
                  console.error("Erro ao carregar o feed de atividades:", error);
                  feedAtividadesContainer.innerHTML = '<li class="list-group-item text-danger">Não foi possível carregar as atividades.</li>';
              }
          }

    // Função para renderizar os cartões do dashboard com base nos dados da API
    async function renderizarWidgets() {
        widgetContainer.innerHTML = `<div class="col-12 text-center"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">A carregar...</span></div></div>`;
        try {
            const response = await apiClient.get('/dashboard');
            const data = response.data;
            widgetContainer.innerHTML = '';

            if (Object.keys(data).length === 0) {
                widgetContainer.innerHTML = '<div class="col-12"><div class="alert alert-info">Nenhum widget selecionado. Escolha as suas preferências abaixo e salve.</div></div>';
                return;
            }

            for (const key in data) {
                if (data.hasOwnProperty(key)) {
                    widgetContainer.innerHTML += createWidgetCard(key, data[key]);
                }
            }
        } catch (error) {
            console.error("Erro ao carregar os dados do dashboard:", error);
            widgetContainer.innerHTML = '<div class="col-12"><div class="alert alert-danger">Não foi possível carregar os dados do dashboard.</div></div>';
        }
    }

    //Função para converter camelCase (Ex: "totalUsuariosAtivos" para "Total Usuarios Ativos")
   function formatWidgetTitle(key) {
       // 1. Converte a chave (que pode ser SNAKE_CASE ou camelCase) em minúsculas
       let formatted = key.toLowerCase();

       // 2. Substitui todos os underscores (_) por espaços (lidando com SNAKE_CASE)
       formatted = formatted.replace(/_/g, ' ');

       // 3. Coloca a primeira letra de cada palavra em maiúscula (para formar "Produto Exemplo")
       formatted = formatted.split(' ').map(word => {
           if (!word) return ''; // Ignora se a palavra estiver vazia
           return word.charAt(0).toUpperCase() + word.slice(1);
       }).join(' ');

       // 4. Se a chave original era camelCase (sem underscores), esta regra ainda garante que "produtoExemplo" vire "Produto Exemplo"
       if (key.indexOf('_') === -1) {
           formatted = key.replace(/([A-Z])/g, ' $1').trim();
           formatted = formatted.charAt(0).toUpperCase() + formatted.slice(1);
       }

       return formatted;
   }

    // Função para buscar e renderizar as checkboxes de preferências
    async function renderizarPreferencias() {
        checkboxesContainer.innerHTML = `<div class="col-12 text-center"><div class="spinner-border spinner-border-sm" role="status"><span class="visually-hidden">A carregar...</span></div></div>`;
        try {
            // Fazemos duas chamadas em paralelo para ser mais rápido
            const [widgetsDisponiveisRes, perfilRes] = await Promise.all([
                apiClient.get('/dashboard/widgets-disponiveis'),
                apiClient.get('/dashboard/preferencias')
            ]);

            const widgetsDisponiveis = widgetsDisponiveisRes.data;
             const preferenciasAtuais = perfilRes.data;
            checkboxesContainer.innerHTML = '';

            widgetsDisponiveis.forEach(widgetName => {
                const isChecked = preferenciasAtuais.includes(widgetName);
                 const details = widgetDetails[widgetName] || { title: formatWidgetTitle(widgetName) };
                 const displayTitle = details.title;
                const checkboxHtml = `
                    <div class="col-md-6 col-sm-6">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" value="${widgetName}" id="check-${widgetName}" ${isChecked ? 'checked' : ''}>
                            <label class="form-check-label" for="check-${widgetName}">
                                ${details.title}
                            </label>
                        </div>
                    </div>
                `;
                checkboxesContainer.innerHTML += checkboxHtml;
            });
        } catch (error) {
            console.error("Erro ao carregar as preferências:", error);
            checkboxesContainer.innerHTML = '<div class="col-12"><div class="alert alert-danger">Não foi possível carregar as opções de configuração.</div></div>';
        }
    }

    // Função para salvar as novas preferências
    async function salvarPreferencias(event) {
        event.preventDefault();
        const checkboxes = checkboxesContainer.querySelectorAll('input[type="checkbox"]');
        const widgetsSelecionados = [];
        checkboxes.forEach(cb => {
            if (cb.checked) {
                widgetsSelecionados.push(cb.value);
            }
        });

        try {
            await apiClient.put('/dashboard/preferencias', widgetsSelecionados);
            showToast('Preferências salvas com sucesso!', 'Sucesso');

             modalPreferencias.hide();

            // Após salvar, renderiza novamente os widgets para refletir a mudança
            await renderizarWidgets();
        } catch (error) {
            console.error("Erro ao salvar as preferências:", error);
            showToast('Não foi possível salvar as preferências.', 'Erro', true);
        }
    }

    // --- Inicialização ---
    formPreferencias.addEventListener('submit', salvarPreferencias);

    // Inicia o carregamento das duas secções da página
    renderizarWidgets();
    renderizarPreferencias();
    carregarFeedDeAtividades();

})();