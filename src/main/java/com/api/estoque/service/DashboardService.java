package com.api.estoque.service;

import com.api.estoque.model.DashboardWidget;
import com.api.estoque.model.PreferenciaDashboard;
import com.api.estoque.model.StatusSolicitacao;
import com.api.estoque.model.Usuario;
import com.api.estoque.repository.EquipamentoRepository;
import com.api.estoque.repository.PreferenciaDashboardRepository;
import com.api.estoque.repository.SolicitacaoRepository;
import com.api.estoque.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final PreferenciaDashboardRepository preferenciaRepository;

    public DashboardService(UsuarioRepository usuarioRepository,
                            SolicitacaoRepository solicitacaoRepository,
                            EquipamentoRepository equipamentoRepository,
                            PreferenciaDashboardRepository preferenciaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.preferenciaRepository = preferenciaRepository;
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
                    dashboardData.put("solicitacoesPendentes", solicitacaoRepository.countByStatus(StatusSolicitacao.PENDENTE));
                    break;
                case SOLICITACOES_APROVADAS_HOJE:
                    dashboardData.put("solicitacoesAprovadasHoje", solicitacaoRepository.countByStatusAndDataSolicitacaoAfter(StatusSolicitacao.APROVADA, LocalDate.now().atStartOfDay()));
                    break;
                case TOTAL_EQUIPAMENTOS_EM_USO:
                    Long emUso = equipamentoRepository.sumEquipamentosEmUso();
                    dashboardData.put("totalEquipamentosEmUso", emUso != null ? emUso : 0);
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
}