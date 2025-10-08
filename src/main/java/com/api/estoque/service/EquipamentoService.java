package com.api.estoque.service;

import com.api.estoque.dto.request.AjusteEstoqueRequest;
import com.api.estoque.dto.request.EquipamentoRequest;
import com.api.estoque.dto.response.EquipamentoResponse;
import com.api.estoque.exception.BusinessException;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.*;
import com.api.estoque.repository.CategoriaRepository;
import com.api.estoque.repository.EquipamentoRepository;
import com.api.estoque.repository.HistoricoMovimentacaoRepository;
import com.api.estoque.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final HistoricoMovimentacaoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository,
                              CategoriaRepository categoriaRepository,
                              HistoricoMovimentacaoRepository historicoRepository,
                              UsuarioRepository usuarioRepository) {
        this.equipamentoRepository = equipamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public EquipamentoResponse criarEquipamento(EquipamentoRequest request) {
        // 1. Busca a entidade Categoria a partir do ID recebido no DTO
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + request.categoriaId()));

        // 2. Cria a nova entidade Equipamento
        Equipamento novoEquipamento = new Equipamento();
        novoEquipamento.setNome(request.nome());
        novoEquipamento.setDescricao(request.descricao());
        novoEquipamento.setQuantidadeTotal(request.quantidadeTotal());
        novoEquipamento.setQuantidadeDisponivel(request.quantidadeTotal()); // Estoque disponível começa igual ao total
        novoEquipamento.setAtivo(true);
        novoEquipamento.setCategoria(categoria); // Associa a categoria que buscamos

        Equipamento equipamentoSalvo = equipamentoRepository.save(novoEquipamento);

        return mapToEquipamentoResponse(equipamentoSalvo);
    }

    @Transactional(readOnly = true)
    public Page<EquipamentoResponse> listarTodos(Pageable pageable) {
        Page<Equipamento> equipamentos = equipamentoRepository.findAllByAtivoTrue(pageable);
        return equipamentos.map(this::mapToEquipamentoResponse);
    }

    // Método auxiliar privado para evitar repetição de código no mapeamento
    private EquipamentoResponse mapToEquipamentoResponse(Equipamento equipamento) {
        return new EquipamentoResponse(
                equipamento.getId(),
                equipamento.getNome(),
                equipamento.getDescricao(),
                equipamento.getQuantidadeTotal(),
                equipamento.getQuantidadeDisponivel(),
                equipamento.isAtivo(),
                equipamento.getCategoria().getNome() // Pega o nome da categoria associada
        );
    }


    @Transactional
    public EquipamentoResponse atualizarEquipamento(Long id, EquipamentoRequest request) {
        // 1. Busca o equipamento que queremos atualizar.
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));

        // 2. Busca a nova categoria, se o ID da categoria foi alterado.
        // Usamos o CategoriaRepository, que já temos injetado.
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + request.categoriaId()));

        // 3. Atualiza os dados do equipamento com as informações do DTO.
        // Note que não permitimos a alteração das quantidades aqui.
        // Isso será feito pelo método de "ajuste de stock".
        equipamento.setNome(request.nome());
        equipamento.setDescricao(request.descricao());
        equipamento.setCategoria(categoria); // Associa a nova categoria

        // O JPA guardará as alterações automaticamente no final da transação.

        // 4. Mapeia a entidade atualizada para a resposta. Reutilizamos o nosso método auxiliar.
        return mapToEquipamentoResponse(equipamento);
    }

    @Transactional
    public void desativarEquipamento(Long id) {
        // 1. Busca o equipamento para garantir que ele existe.
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));

        // 2. REGRA DE NEGÓCIO: Só desativamos se não houver stock "em circulação".
        // Se a quantidade disponível não for igual à total, significa que há itens emprestados.
        if (equipamento.getQuantidadeDisponivel() != equipamento.getQuantidadeTotal()) {
            throw new BusinessException(
                    "Não é possível desativar o equipamento. Existem unidades em utilização." +
                            " Total: " + equipamento.getQuantidadeTotal() +
                            ", Disponível: " + equipamento.getQuantidadeDisponivel()
            );
        }

        // 3. Altera o estado para inativo.
        equipamento.setAtivo(false);
    }

    @Transactional
    public void ativarEquipamento(Long id) {
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));

        // Ação de negócio: muda o estado para ativo
        equipamento.setAtivo(true);
    }

    @Transactional(readOnly = true)
    public Page<EquipamentoResponse> listarTodosAdmin(Pageable pageable) {
        // Usa o método padrão findAll(), que não filtra por 'ativo'
        Page<Equipamento> equipamentos = equipamentoRepository.findAll(pageable);
        return equipamentos.map(this::mapToEquipamentoResponse);
    }

    @Transactional
    public EquipamentoResponse ajustarEstoque(Long id, AjusteEstoqueRequest request) {
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));

        int quantidadeEmUso = equipamento.getQuantidadeTotal() - equipamento.getQuantidadeDisponivel();
        if (request.novaQuantidade() < quantidadeEmUso) {
            throw new BusinessException(
                    "Ajuste de stock inválido. A nova quantidade total (" + request.novaQuantidade() + ") " +
                            "não pode ser menor que a quantidade de itens atualmente em utilização (" + quantidadeEmUso + ")."
            );
        }
        // 1. BUSCAR UM RESPONSÁVEL
        Usuario responsavel = usuarioRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador padrão (ID 1) não encontrado..."));

        // Regra de Negócio: Calcula a diferença entre o stock antigo e o novo
        int quantidadeAntiga = equipamento.getQuantidadeTotal();
        int diferenca = request.novaQuantidade() - quantidadeAntiga;

        // Atualiza as quantidades no equipamento
        equipamento.setQuantidadeTotal(request.novaQuantidade());
        equipamento.setQuantidadeDisponivel(equipamento.getQuantidadeDisponivel() + diferenca);

        // Regista a movimentação de AJUSTE no histórico
        // Nota: O utilizador responsável aqui será o que estiver autenticado no futuro. Por agora, podemos deixar nulo ou usar um utilizador padrão.
        HistoricoMovimentacao registoHistorico = HistoricoMovimentacao.builder()
                .dataMovimentacao(LocalDateTime.now())
                .tipoMovimentacao(TipoMovimentacao.AJUSTE_MANUAL)
                .quantidade(diferenca) // Regista a diferença (positiva ou negativa)
                .equipamento(equipamento)
                .usuarioResponsavel(responsavel) // TODO: Substituir pelo utilizador logado no futuro
                .build();
        historicoRepository.save(registoHistorico);

        return mapToEquipamentoResponse(equipamento);
    }
}