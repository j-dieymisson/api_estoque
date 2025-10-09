package com.api.estoque.service;

import com.api.estoque.dto.response.CargoResponse;
import com.api.estoque.repository.CargoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> listarCargos() {
        return cargoRepository.findAll().stream()
                .map(cargo -> new CargoResponse(cargo.getId(), cargo.getNome()))
                .collect(Collectors.toList());
    }
}