package com.api.estoque.controller;

import com.api.estoque.dto.request.DadosAutenticacao;
import com.api.estoque.dto.response.DadosTokenJWT;
import com.api.estoque.model.Usuario;
import com.api.estoque.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AutenticacaoController {

    @Autowired
    private AuthenticationManager manager; // Objeto do Spring que executa a autenticação

    @Autowired
    private TokenService tokenService;

    @PostMapping
    public ResponseEntity<DadosTokenJWT> efetuarLogin(@RequestBody DadosAutenticacao dados) {
        // 1. Cria um objeto para guardar as credenciais (ainda não estão validadas)
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.nome(), dados.senha());

        // 2. Dispara o processo de autenticação do Spring Security
        // O Spring vai chamar o nosso AuthenticationService para validar o utilizador e a senha
        Authentication authentication = manager.authenticate(authenticationToken);

        // 3. Se a autenticação for bem-sucedida, pega no objeto do utilizador autenticado
        var usuario = (Usuario) authentication.getPrincipal();

        // 4. Chama o nosso TokenService para gerar o token JWT
        var tokenJWT = tokenService.gerarToken(usuario);

        // 5. Devolve o token num DTO de resposta
        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
    }
}