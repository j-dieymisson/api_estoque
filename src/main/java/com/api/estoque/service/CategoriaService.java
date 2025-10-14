package com.api.estoque.service;

import com.api.estoque.dto.request.CategoriaRequest;
import com.api.estoque.dto.response.CategoriaResponse;
import com.api.estoque.exception.BusinessException;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.Categoria;
import com.api.estoque.repository.CategoriaRepository;
import com.api.estoque.repository.EquipamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Marca a classe como um componente de serviço gerenciado pelo Spring
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final EquipamentoRepository equipamentoRepository;

    // Injeção de dependência via construtor (melhor prática)
    public CategoriaService(CategoriaRepository categoriaRepository,
                            EquipamentoRepository equipamentoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.equipamentoRepository = equipamentoRepository;
    }

    @Transactional
    public CategoriaResponse criarCategoria(CategoriaRequest request) {
        // VERIFICAÇÃO DE NOME DUPLICADO
        if (categoriaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new BusinessException("Já existe uma categoria com este nome.");
        }

        Categoria categoria = new Categoria();
        categoria.setNome(request.nome());
        categoriaRepository.save(categoria);

        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isAtiva());
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas(Optional<Boolean> ativa) {
        List<Categoria> categorias;

        if (ativa.isPresent()) {
            // Se o parâmetro 'ativa' foi fornecido, usa o novo método de filtro
            categorias = categoriaRepository.findByAtiva(ativa.get());
        } else {
            // Se nenhum parâmetro foi fornecido, retorna todas as categorias, como antes
            categorias = categoriaRepository.findAll();
        }

        return categorias.stream()
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isAtiva()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaResponse atualizarCategoria(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));

        // VERIFICAÇÃO DE NOME DUPLICADO NA ATUALIZAÇÃO
        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(request.nome(), id)) {
            throw new BusinessException("Já existe outra categoria com este nome.");
        }

        categoria.setNome(request.nome());
        categoriaRepository.save(categoria);

        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isAtiva());
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isAtiva());
    }

    // MÉTODO PARA DESATIVAR
    @Transactional
    public void desativarCategoria(Long id) {
        // Busca a categoria para garantir que ela existe antes de tentar desativar.
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        // Verifica se existem equipamentos ativos associados a esta categoria
        long equipamentosAtivos = equipamentoRepository.countByCategoriaIdAndAtivoTrue(id);
        if (equipamentosAtivos > 0) {
            throw new BusinessException(
                    "Não é possível desativar a categoria. Existem " + equipamentosAtivos + " equipamentos ativos associados a ela."
            );
        }

        // Ação de negócio: em vez de apagar, mudamos o estado.
        categoria.setAtiva(false);

        // Não precisamos de retornar nada, a operação é apenas de modificação.
    }

    @Transactional
    public void ativarCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        // Ação de negócio: muda o estado para ativo
        categoria.setAtiva(true);
    }
}