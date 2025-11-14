package com.api.estoque.service;

import com.api.estoque.dto.response.CargoResponse;
import com.api.estoque.model.Usuario;
import com.api.estoque.repository.CargoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> listarCargos(Usuario usuarioLogado) {

        String cargo = usuarioLogado.getCargo().getNome();

        // 1. ADMIN vê todos os cargos
        if ("ADMIN".equals(cargo)) {
            return cargoRepository.findAll().stream()
                    .map(c -> new CargoResponse(c.getId(), c.getNome()))
                    .collect(Collectors.toList());
        }
        // 2. GESTOR vê todos, EXCETO "ADMIN"
        else if ("GESTOR".equals(cargo)) {
            // Chamamos o novo método do repositório (ver próximo passo)
            return cargoRepository.findAllByNomeNot("ADMIN").stream()
                    .map(c -> new CargoResponse(c.getId(), c.getNome()))
                    .collect(Collectors.toList());
        }
        // 3. COLABORADOR (ou outros) não veem nada neste dropdown
        else {
            return Collections.emptyList();
        }
    }
}