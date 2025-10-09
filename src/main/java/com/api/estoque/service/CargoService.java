package com.api.estoque.service;

import com.api.estoque.dto.request.CargoRequest;
import com.api.estoque.dto.response.CargoResponse;
import com.api.estoque.dto.response.PermissaoResponse;
import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.Cargo;
import com.api.estoque.model.Permissao;
import com.api.estoque.repository.CargoRepository;
import com.api.estoque.repository.PermissaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;
    private final PermissaoRepository permissaoRepository;

    public CargoService(CargoRepository cargoRepository, PermissaoRepository permissaoRepository) {
        this.cargoRepository = cargoRepository;
        this.permissaoRepository = permissaoRepository;
    }

    @Transactional
    public CargoResponse criarCargo(CargoRequest request) {
        Set<Permissao> permissoes = request.permissoesIds().stream()
                .map(id -> permissaoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada com o ID: " + id)))
                .collect(Collectors.toSet());

        Cargo novoCargo = new Cargo();
        novoCargo.setNome(request.nome());
        novoCargo.setPermissoes(permissoes);

        cargoRepository.save(novoCargo);
        return mapToCargoResponse(novoCargo);
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> listarCargos() {
        return cargoRepository.findAll().stream()
                .map(this::mapToCargoResponse)
                .collect(Collectors.toList());
    }

    // Método auxiliar de mapeamento
    private CargoResponse mapToCargoResponse(Cargo cargo) {
        Set<PermissaoResponse> permissoesResponse = cargo.getPermissoes().stream()
                .map(p -> new PermissaoResponse(p.getId(), p.getNome()))
                .collect(Collectors.toSet());
        return new CargoResponse(cargo.getId(), cargo.getNome(), permissoesResponse);
    }
}