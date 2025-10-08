package com.api.estoque.service;

import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.StatusSolicitacao;
import com.api.estoque.model.Usuario;
import com.api.estoque.repository.SolicitacaoRepository;
import com.api.estoque.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    // Precisaremos do SolicitacaoService para reutilizar a lógica de cancelamento
    private final SolicitacaoService solicitacaoService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          SolicitacaoRepository solicitacaoRepository,
                          SolicitacaoService solicitacaoService) {
        this.usuarioRepository = usuarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.solicitacaoService = solicitacaoService;
    }

    @Transactional
    public void desativarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com o ID: " + id));

        // 1. Encontra todas as solicitações PENDENTES deste utilizador.
        // Reutilizamos o método que já tínhamos no repositório.
        List<Solicitacao> solicitacoesPendentes = solicitacaoRepository
                .findAllByUsuarioIdAndStatus(id, StatusSolicitacao.PENDENTE);

        // 2. Itera e cancela cada uma delas.
        for (Solicitacao solicitacao : solicitacoesPendentes) {
            // Reutilizamos o nosso método de cancelamento para garantir que
            // o histórico de status também é registado corretamente.
            solicitacaoService.cancelarSolicitacao(solicitacao.getId());
        }

        // 3. Finalmente, desativa o utilizador.
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void ativarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com o ID: " + id));

        // Ação de negócio: muda o estado para ativo.
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }
}