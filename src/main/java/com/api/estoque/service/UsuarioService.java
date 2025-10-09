package com.api.estoque.service;

import com.api.estoque.dto.request.UsuarioRequest;
import com.api.estoque.dto.response.UsuarioResponse;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.Cargo;
import com.api.estoque.model.Solicitacao;
import com.api.estoque.model.StatusSolicitacao;
import com.api.estoque.model.Usuario;
import com.api.estoque.repository.CargoRepository;
import com.api.estoque.repository.SolicitacaoRepository;
import com.api.estoque.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final SolicitacaoService solicitacaoService;
    private final CargoRepository cargoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          SolicitacaoRepository solicitacaoRepository,
                          SolicitacaoService solicitacaoService,
                          CargoRepository cargoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.solicitacaoService = solicitacaoService;
        this.cargoRepository = cargoRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public UsuarioResponse criarUsuario(UsuarioRequest request) {
        // 1. Busca o cargo na base de dados a partir do ID recebido
        Cargo cargo = cargoRepository.findById(request.cargoId())
                .orElseThrow(() -> new ResourceNotFoundException("Cargo não encontrado com o ID: " + request.cargoId()));

        // 2. Cria a nova entidade de utilizador
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(request.nome());
        novoUsuario.setEmail(request.email());
        novoUsuario.setLogin(request.nome()); // Usando o nome como login, conforme o plano
        novoUsuario.setCargo(cargo);
        novoUsuario.setAtivo(true);

        // 3. O PASSO MAIS IMPORTANTE: Encripta a senha antes de a definir
        novoUsuario.setSenha(passwordEncoder.encode(request.senha()));

        // 4. Salva o novo utilizador na base de dados
        usuarioRepository.save(novoUsuario);

        // 5. Mapeia a entidade para o DTO de resposta
        return mapToUsuarioResponse(novoUsuario);
    }

    // Adicione também um método auxiliar para o mapeamento
    private UsuarioResponse mapToUsuarioResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCargo().getNome(), // Pega o nome do objeto Cargo associado
                usuario.isAtivo()
        );
    }
}