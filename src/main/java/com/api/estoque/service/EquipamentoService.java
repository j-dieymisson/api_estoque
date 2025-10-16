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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

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
    public Page<EquipamentoResponse> listarTodos(
            Optional<String> nome,
            Optional<Long> categoriaId,
            Optional<Long> id,
            Optional<LocalDate> dataInicioCriacao, // Novo
            Optional<LocalDate> dataFimCriacao,    // Novo
            Pageable pageable) {

        // Converte as datas para o início e fim do dia
        LocalDateTime inicio = dataInicioCriacao.map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime fim = dataFimCriacao.map(d -> d.atTime(23, 59, 59)).orElse(null);

        // Chama o novo método único, passando null para os filtros não usados
        Page<Equipamento> equipamentos = equipamentoRepository.findWithFilters(
                id.orElse(null),
                nome.orElse(null),
                categoriaId.orElse(null),
                inicio,
                fim,
                pageable
        );

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
                equipamento.getCategoria().getId(),
                equipamento.getCategoria().getNome(),
                equipamento.getDataCriacao()// Pega o nome da categoria associada
        );
    }

    @Transactional
    public EquipamentoResponse atualizarEquipamento(Long id, EquipamentoRequest request, Usuario usuarioLogado) {
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));

        // 1. Atualiza os dados principais
        equipamento.setNome(request.nome());
        equipamento.setDescricao(request.descricao());

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + request.categoriaId()));
        equipamento.setCategoria(categoria);

        // 2. Verifica SE a quantidade mudou
        int quantidadeAntiga = equipamento.getQuantidadeTotal();
        if (request.quantidadeTotal() != quantidadeAntiga) {
            // 3. Se mudou, CHAMA a lógica de ajuste de stock
            realizarAjusteEstoque(equipamento, request.quantidadeTotal(), usuarioLogado);
        }

        return mapToEquipamentoResponse(equipamento);
    }

    @Transactional
    public EquipamentoResponse ajustarEstoque(Long id, AjusteEstoqueRequest request, Usuario usuarioLogado) {
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));

        // Delega a lógica para o método privado
        realizarAjusteEstoque(equipamento, request.novaQuantidade(), usuarioLogado);

        return mapToEquipamentoResponse(equipamento);
    }

    // --- LÓGICA CENTRALIZADA (NOVO MÉTODO PRIVADO) ---
    private void realizarAjusteEstoque(Equipamento equipamento, int novaQuantidade, Usuario responsavel) {
        int quantidadeEmUso = equipamento.getQuantidadeTotal() - equipamento.getQuantidadeDisponivel();
        if (novaQuantidade < quantidadeEmUso) {
            throw new BusinessException("Ajuste de stock inválido. A nova quantidade (" + novaQuantidade + ") não pode ser menor que a quantidade em utilização (" + quantidadeEmUso + ").");
        }

        int quantidadeTotalAntiga = equipamento.getQuantidadeTotal();
        int quantidadeDisponivelAntiga = equipamento.getQuantidadeDisponivel();
        int diferenca = novaQuantidade - quantidadeTotalAntiga;

        equipamento.setQuantidadeTotal(novaQuantidade);
        equipamento.setQuantidadeDisponivel(quantidadeDisponivelAntiga + diferenca);

        // A criação do histórico só acontece aqui
        HistoricoMovimentacao registoHistorico = HistoricoMovimentacao.builder()
                .dataMovimentacao(LocalDateTime.now())
                .tipoMovimentacao(TipoMovimentacao.AJUSTE_MANUAL)
                .quantidade(diferenca)
                .quantidadeAnterior(quantidadeDisponivelAntiga)
                .quantidadePosterior(equipamento.getQuantidadeDisponivel())
                .equipamento(equipamento)
                .usuarioResponsavel(responsavel)
                .build();
        historicoRepository.save(registoHistorico);
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

    @Transactional(readOnly = true)
    public EquipamentoResponse buscarPorId(Long id) {
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com o ID: " + id));

        return mapToEquipamentoResponse(equipamento);
    }



    @Transactional(readOnly = true)
    public Page<EquipamentoResponse> listarDisponiveis(Pageable pageable) {
        // Chamamos o novo método do repositório, passando 0 como a quantidade mínima
        Page<Equipamento> equipamentosDisponiveis = equipamentoRepository
                .findAllByAtivoTrueAndQuantidadeDisponivelGreaterThan(0, pageable);

        // Reutilizamos o nosso mapeamento para DTOs
        return equipamentosDisponiveis.map(this::mapToEquipamentoResponse);
    }


}