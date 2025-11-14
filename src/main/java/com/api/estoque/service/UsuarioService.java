package com.api.estoque.service;

import com.api.estoque.dto.request.AlterarSenhaRequest;
import com.api.estoque.dto.request.UsuarioRequest;
import com.api.estoque.dto.request.UsuarioUpdateRequest;
import com.api.estoque.dto.response.UsuarioResponse;
import com.api.estoque.exception.BusinessException;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.*;
import com.api.estoque.repository.CargoRepository;
import com.api.estoque.repository.SetorRepository;
import com.api.estoque.repository.SolicitacaoRepository;
import com.api.estoque.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final SolicitacaoService solicitacaoService;
    private final CargoRepository cargoRepository;
    private final PasswordEncoder passwordEncoder;
    private final SetorRepository setorRepository;

    @Value("${api.security.super-admin.id}") // Injeta o valor do application.properties
    private Long superAdminId;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          SolicitacaoRepository solicitacaoRepository,
                          SolicitacaoService solicitacaoService,
                          CargoRepository cargoRepository,
                          PasswordEncoder passwordEncoder,
                          SetorRepository setorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.solicitacaoService = solicitacaoService;
        this.cargoRepository = cargoRepository;
        this.passwordEncoder = passwordEncoder;
        this.setorRepository = setorRepository;
    }

    @Transactional
    public void desativarUsuario(Long id,Usuario adminLogado) {

        if (id.equals(superAdminId)) {
            throw new BusinessException("O administrador principal não pode ser desativado.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com o ID: " + id));

        // 1. Define a lista de status que consideramos "pendentes"
        List<StatusSolicitacao> statusesPendentes = List.of(
                StatusSolicitacao.PENDENTE_GESTOR,
                StatusSolicitacao.PENDENTE_ADMIN
        );

        // 2. Encontra todas as solicitações deste utilizador que estão em qualquer um desses status
        List<Solicitacao> solicitacoesPendentes = solicitacaoRepository
                .findAllByUsuarioIdAndStatusIn(id, statusesPendentes);

        // 2. Itera e cancela cada uma delas.
        for (Solicitacao solicitacao : solicitacoesPendentes) {
            // Reutilizamos o nosso método de cancelamento para garantir que
            // o histórico de status também é registado corretamente.
            solicitacaoService.cancelarSolicitacao(solicitacao.getId(), adminLogado);
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

        if (usuarioRepository.findByNome(request.nome()).isPresent()) {
            throw new BusinessException("Nome de utilizador já em uso.");
        }

        Cargo cargo = cargoRepository.findById(request.cargoId())
                .orElseThrow(() -> new ResourceNotFoundException("Cargo não encontrado com o ID: " + request.cargoId()));

        // 2. Cria a nova entidade de utilizador
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(request.nome());
        novoUsuario.setEmail(request.email());
        novoUsuario.setLogin(request.nome()); // Usando o nome como login, conforme o plano
        novoUsuario.setCargo(cargo);
        novoUsuario.setAtivo(true);

        //Associa o Setor, se um ID for fornecido
        if (request.setorId() != null) {
            Setor setor = setorRepository.findById(request.setorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado com o ID: " + request.setorId()));
            novoUsuario.setSetor(setor);
        }
        novoUsuario.setFuncao(request.funcao()); // Salva a nova função
        // 3. O PASSO MAIS IMPORTANTE: Encripta a senha antes de a definir
        novoUsuario.setSenha(passwordEncoder.encode(request.senha()));

        // 4. Salva o novo utilizador na base de dados
        usuarioRepository.save(novoUsuario);

        // 5. Mapeia a entidade para o DTO de resposta
        return mapToUsuarioResponse(novoUsuario);
    }

    // MÉTODO PARA LISTAR TODOS (com paginação)
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarTodos(Pageable pageable) {
        Page<Usuario> usuarios = usuarioRepository.findAll(pageable);
        return usuarios.map(this::mapToUsuarioResponse);
    }

    // MÉTODO PARA BUSCAR POR ID
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        // Usamos uma query que força o JOIN FETCH
        Usuario usuario = usuarioRepository.findByIdWithCargoAndSetor(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com o ID: " + id));
        return mapToUsuarioResponse(usuario);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarTodos(Optional<String> nome, Pageable pageable) {
        Page<Usuario> usuarios;
        if (nome.isPresent()) {
            // Usa a nova busca por nome que exclui o super admin
            usuarios = usuarioRepository.findByNomeContainingIgnoreCaseAndIdNot(nome.get(), superAdminId, pageable);
        } else {
            // Usa a nova listagem geral que exclui o super admin
            usuarios = usuarioRepository.findByIdNot(superAdminId, pageable);
        }
        return usuarios.map(this::mapToUsuarioResponse);
    }

    @Transactional
    public UsuarioResponse atualizarUsuario(Long id,
                                            UsuarioUpdateRequest request,
                                            Usuario usuarioLogado) {

        if (id.equals(superAdminId)) {
            // Se for, verifica se a pessoa a fazer a alteração é o próprio super admin
            if (!usuarioLogado.getId().equals(superAdminId)) {
                // Se não for, bloqueia.
                throw new BusinessException("Outros administradores não podem alterar os dados do administrador principal.");
            }
        }

        Optional<UserDetails> usuarioExistente = usuarioRepository.findByNome(request.nome());
        if (usuarioExistente.isPresent() && !((Usuario) usuarioExistente.get()).getId().equals(id)) {
            // Lança erro se encontrou um utilizador com o mesmo nome E o ID é diferente do nosso
            throw new BusinessException("Nome de utilizador já em uso.");
        }
        // 1. Busca o utilizador que será atualizado.
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com o ID: " + id));

        if (usuario.getId().equals(superAdminId)) {
            // Verifica se a requisição está a tentar alterar o cargo do super admin
            if (!usuario.getCargo().getId().equals(request.cargoId())) {
                throw new BusinessException("O cargo do administrador principal não pode ser alterado.");
            }
        }

        // 2. Busca o novo cargo que será associado.
        Cargo novoCargo = cargoRepository.findById(request.cargoId())
                .orElseThrow(() -> new ResourceNotFoundException("Cargo não encontrado com o ID: " + request.cargoId()));

        // 3. Atualiza os dados da entidade com as informações do DTO.
        usuario.setNome(request.nome());
        usuario.setLogin(request.nome());
        usuario.setEmail(request.email());
        usuario.setCargo(novoCargo); // A importante alteração de cargo acontece aqui.

        // 4. Atualiza o Setor
        if (request.setorId() != null) {
            Setor setor = setorRepository.findById(request.setorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado com o ID: " + request.setorId()));
            usuario.setSetor(setor);
        } else {
            // Permite remover o gestor (definir como null)
            usuario.setSetor(null);
        }
        usuario.setFuncao(request.funcao());

        return mapToUsuarioResponse(usuario);
    }

    @Transactional
    public void alterarSenha(Long id,
                             AlterarSenhaRequest request,
                             Usuario usuarioLogado) {

        if (id.equals(superAdminId) && !usuarioLogado.getId().equals(superAdminId)) {
            throw new BusinessException("A senha do administrador principal só pode ser alterada por ele mesmo.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com o ID: " + id));

        // Encripta a nova senha antes de a salvar
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));

        // O JPA guarda a alteração automaticamente no fim da transação.
    }


    // Adicione também um método auxiliar para o mapeamento
    public UsuarioResponse mapToUsuarioResponse(Usuario usuario) {

        String cargoNome = (usuario.getCargo() != null) ? usuario.getCargo().getNome() : null;
        Long cargoId = (usuario.getCargo() != null) ? usuario.getCargo().getId() : null;

        // --- LÓGICA DE GESTOR REMOVIDA E SUBSTITUÍDA ---
        Setor setor = usuario.getSetor(); // Este é o campo LAZY
        Long setorId = null;
        String setorNome = null;

        if (setor != null) {
            // Usamos o 'proxy' para pegar o ID, o que é seguro
            setorId = setor.getId();

            // Verificamos se o nome já foi carregado (para evitar LazyInitializationException)
            if (org.hibernate.Hibernate.isInitialized(setor)) {
                setorNome = setor.getNome();
            }
            // Se não estiver inicializado, setorNome permanece null
        }

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCargo().getNome(), // Pega o nome do objeto Cargo associado
                usuario.getCargo().getId(),
                usuario.isAtivo(),
                setorId,
                setorNome,
                usuario.getFuncao()
        );
    }
}