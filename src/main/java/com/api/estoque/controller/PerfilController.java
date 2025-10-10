package com.api.estoque.controller;

import com.api.estoque.dto.response.UsuarioResponse;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<UsuarioResponse> detalharPerfil(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        // A anotação @AuthenticationPrincipal injeta o utilizador que fez o login.
        // Não precisamos de ir à base de dados, pois o Spring Security já o carregou para nós.

        // Reutilizamos o método de mapeamento que tornámos público no UsuarioService
        UsuarioResponse response = usuarioService.mapToUsuarioResponse(usuarioLogado);

        return ResponseEntity.ok(response);
    }
}