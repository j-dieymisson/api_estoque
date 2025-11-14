package com.api.estoque.controller;

import com.api.estoque.dto.response.CargoResponse;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.CargoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cargos")
public class CargoController {

    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    @GetMapping
    public ResponseEntity<List<CargoResponse>> listar(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {

        // que irá tratar da lógica de filtragem.
        return ResponseEntity.ok(cargoService.listarCargos(usuarioLogado));
    }
}