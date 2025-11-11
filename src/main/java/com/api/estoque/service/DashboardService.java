package com.api.estoque.service;

import com.api.estoque.dto.response.AtividadeRecenteResponse;
import com.api.estoque.model.*;
import com.api.estoque.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final PreferenciaDashboardRepository preferenciaRepository;
    private final CategoriaRepository categoriaRepository;
    private final HistoricoMovimentacaoRepository historicoMovimentacaoRepository;


    public DashboardService(UsuarioRepository usuarioRepository,
                            SolicitacaoRepository solicitacaoRepository,
                            EquipamentoRepository equipamentoRepository,
                            PreferenciaDashboardRepository preferenciaRepository,
                            CategoriaRepository categoriaRepository,
                            HistoricoMovimentacaoRepository historicoMovimentacaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.categoriaRepository = categoriaRepository;
        this.historicoMovimentacaoRepository = historicoMovimentacaoRepository;
    }

    /**
     * Calcula e retorna os dados do dashboard com base nas preferências do utilizador logado.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(Usuario usuarioLogado) {
        // Busca as preferências guardadas para este utilizador
        Set<DashboardWidget> widgetsPreferidos = usuarioLogado.getPreferenciasDashboard().stream()
                .map(PreferenciaDashboard::getWidgetNome)
                .collect(Collectors.toSet());

        // Usamos um LinkedHashMap para manter a ordem de inserção
        Map<String, Object> dashboardData = new LinkedHashMap<>();

        // Para cada widget preferido, calcula o valor correspondente
        for (DashboardWidget widget : widgetsPreferidos) {
            switch (widget) {
                case TOTAL_USUARIOS_ATIVOS:
                    dashboardData.put("totalUsuariosAtivos", usuarioRepository.countByAtivoTrue());
                    break;
                case SOLICITACOES_PENDENTES:
                    dashboardData.put("solicitacoesPendentes", solicitacaoRepository.countByStatusIn(
                            List.of(StatusSolicitacao.PENDENTE_GESTOR, StatusSolicitacao.PENDENTE_ADMIN)
                    ));
                    break;
                case SOLICITACOES_APROVADAS_HOJE:
                    dashboardData.put("solicitacoesAprovadasHoje", solicitacaoRepository.countByStatusInAndDataSolicitacaoAfter(
                            List.of(StatusSolicitacao.APROVADA), LocalDate.now().atStartOfDay()
                    ));
                    break;
                case TOTAL_UNIDADES_EM_USO:
                    Long emUso = equipamentoRepository.sumEquipamentosEmUso();
                    dashboardData.put("totalUnidadesEmUso", emUso != null ? emUso : 0);
                    break;
                case TOTAL_EQUIPAMENTOS_CADASTRADOS:
                    dashboardData.put("totalEquipamentosCadastrados", equipamentoRepository.count());
                    break;
                case TOTAL_UNIDADES_CADASTRADAS:
                    Long totalUnidades = equipamentoRepository.sumQuantidadeTotal();
                    dashboardData.put("totalUnidadesCadastradas", totalUnidades != null ? totalUnidades : 0);
                    break;
                case SOLICITACOES_FINALIZADAS_MES:
                    YearMonth mesAtual = YearMonth.now();
                    LocalDateTime inicioDoMes = mesAtual.atDay(1).atStartOfDay();
                    LocalDateTime fimDoMes = mesAtual.atEndOfMonth().atTime(23, 59, 59);
                    dashboardData.put("solicitacoesFinalizadasMes", solicitacaoRepository.countByStatusAndDataSolicitacaoBetween(StatusSolicitacao.FINALIZADA, inicioDoMes, fimDoMes));
                    break;
                case SOLICITACOES_TOTAIS:
                    dashboardData.put("solicitacoesTotais", solicitacaoRepository.countByStatusNotIn(
                            List.of(StatusSolicitacao.RASCUNHO)
                    ));
                    break;
                case EQUIPAMENTOS_ATIVOS:
                    dashboardData.put("equipamentosAtivos", equipamentoRepository.countByAtivoTrue());
                    break;
                case TOTAL_CATEGORIAS:
                    dashboardData.put("totalCategorias", categoriaRepository.count());
                    break;

            }
        }
        return dashboardData;
    }

    /**
     * Atualiza as preferências de dashboard para o utilizador logado.
     */
    @Transactional
    public void atualizarPreferencias(Usuario usuarioLogado, List<DashboardWidget> novosWidgets) {
        // 1. Apaga todas as preferências antigas deste utilizador DIRETAMENTE no repositório.
        // Isto executa um 'DELETE FROM preferencias_dashboard WHERE usuario_id = ?'
        preferenciaRepository.deleteByUsuarioId(usuarioLogado.getId());

        // 2. Cria e salva as novas preferências.
        if (novosWidgets != null && !novosWidgets.isEmpty()) {
            List<PreferenciaDashboard> novasPreferencias = novosWidgets.stream()
                    .map(widgetNome -> {
                        PreferenciaDashboard pref = new PreferenciaDashboard();
                        // Precisamos de associar o utilizador gerenciado pela transação atual
                        pref.setUsuario(usuarioRepository.findById(usuarioLogado.getId()).get());
                        pref.setWidgetNome(widgetNome);
                        return pref;
                    })
                    .collect(Collectors.toList());
            preferenciaRepository.saveAll(novasPreferencias);
        }
    }

    /**
     * Retorna a lista de todos os widgets de dashboard disponíveis no sistema.
     */
    public List<String> listarWidgetsDisponiveis() {
        return Arrays.stream(DashboardWidget.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    // NOVO MÉTODO PARA O FEED DE ATIVIDADES
    @Transactional(readOnly = true)
    public List<AtividadeRecenteResponse> getFeedAtividades() {
        // 1. Criamos a nossa lista final que irá combinar tudo
        List<AtividadeRecenteResponse> feedCompleto = new ArrayList<>();

        // 2. Buscamos as 5 solicitações pendentes mais recentes
        List<Solicitacao> pendentes = solicitacaoRepository
                .findTop5ByStatusInOrderByDataSolicitacaoDesc(
                        List.of(StatusSolicitacao.PENDENTE_GESTOR, StatusSolicitacao.PENDENTE_ADMIN)
                );

        // 3. Mapeamos as pendentes para o nosso DTO de resposta
        List<AtividadeRecenteResponse> atividadesPendentes = pendentes.stream()
                .map(sol -> new AtividadeRecenteResponse(
                        sol.getId(),
                        "SOLICITACAO_PENDENTE",
                        sol.getUsuario().getNome() + " criou uma nova solicitação.",
                        sol.getDataSolicitacao()
                ))
                .collect(Collectors.toList());

        feedCompleto.addAll(atividadesPendentes);

        // 4. Buscamos as 5 movimentações mais recentes (ignorando ajustes)
        List<HistoricoMovimentacao> movimentacoes = historicoMovimentacaoRepository
                .findTop5ByTipoMovimentacaoNotOrderByDataMovimentacaoDesc(TipoMovimentacao.AJUSTE_MANUAL);

        // 5. Mapeamos as movimentações para o nosso DTO de resposta
        List<AtividadeRecenteResponse> atividadesMovimentacoes = movimentacoes.stream()
                .map(mov -> {
                    String tipo = mov.getTipoMovimentacao().name(); // SAIDA ou DEVOLUCAO
                    String descricao = "";

                    // Criamos a descrição formatada como você pediu
                    if (tipo.equals("SAIDA")) {
                        descricao = mov.getUsuarioResponsavel().getNome() + " fez a SAÍDA (Solicitação #" + mov.getSolicitacao().getId() + ").";
                    } else if (tipo.equals("DEVOLUCAO")) {
                        descricao = mov.getUsuarioResponsavel().getNome() + " fez uma DEVOLUÇÃO (Solicitação #" + mov.getSolicitacao().getId() + ").";
                    }

                    return new AtividadeRecenteResponse(
                            mov.getSolicitacao().getId(),
                            tipo,
                            descricao,
                            mov.getDataMovimentacao()
                    );
                })
                .collect(Collectors.toList());

        feedCompleto.addAll(atividadesMovimentacoes);

        // 6. Ordenamos a lista combinada pela data (do mais recente para o mais antigo)
        //    e pegamos apenas os 5 itens mais recentes no total.
        return feedCompleto.stream()
                .sorted(Comparator.comparing(AtividadeRecenteResponse::data).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}