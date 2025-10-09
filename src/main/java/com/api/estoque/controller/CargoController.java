package com.api.estoque.controller;

import com.api.estoque.dto.request.CargoRequest;
import com.api.estoque.dto.response.CargoResponse;
import com.api.estoque.service.CargoService; // <-- Import correto
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/cargos")
public class CargoController {

    private final CargoService cargoService; // <-- Injetar o SERVIÇO, não o repositório

    public CargoController(CargoService cargoService) { // <-- Receber o SERVIÇO
        this.cargoService = cargoService;
    }

    @PostMapping
    public ResponseEntity<CargoResponse> criar(@RequestBody @Valid CargoRequest request, UriComponentsBuilder uriBuilder) {
        // Chamar o método do serviço
        CargoResponse response = cargoService.criarCargo(request);
        URI uri = uriBuilder.path("/cargos/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CargoResponse>> listar() {
        // Chamar o método do serviço
        return ResponseEntity.ok(cargoService.listarCargos());
    }
}