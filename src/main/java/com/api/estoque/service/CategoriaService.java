package com.api.estoque.service;

import com.api.estoque.dto.request.CategoriaRequest;
import com.api.estoque.dto.response.CategoriaResponse;
import com.api.estoque.model.Categoria;
import com.api.estoque.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service // Marca a classe como um componente de serviço gerenciado pelo Spring
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // Injeção de dependência via construtor (melhor prática)
    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional // Garante que a operação seja atômica (ou tudo funciona, ou nada é salvo)
    public CategoriaResponse criarCategoria(CategoriaRequest request) {
        Categoria novaCategoria = new Categoria();
        novaCategoria.setNome(request.nome());
        novaCategoria.setAtiva(true); // Por padrão, uma nova categoria sempre nasce ativa

        Categoria categoriaSalva = categoriaRepository.save(novaCategoria);

        return new CategoriaResponse(categoriaSalva.getId(), categoriaSalva.getNome(), categoriaSalva.isAtiva());
    }

    @Transactional(readOnly = true) // Otimização para operações que são apenas de leitura
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                // Mapeia cada Entidade Categoria para um DTO CategoriaResponse
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isAtiva()))
                .collect(Collectors.toList());
    }

    // Futuramente, podemos adicionar métodos para buscar por ID, atualizar e desativar.
}