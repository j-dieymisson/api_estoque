package com.api.estoque.service;

import com.api.estoque.dto.request.EquipamentoRequest;
import com.api.estoque.dto.response.EquipamentoResponse;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.Categoria;
import com.api.estoque.model.Equipamento;
import com.api.estoque.repository.CategoriaRepository;
import com.api.estoque.repository.EquipamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final CategoriaRepository categoriaRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository, CategoriaRepository categoriaRepository) {
        this.equipamentoRepository = equipamentoRepository;
        this.categoriaRepository = categoriaRepository;
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
}