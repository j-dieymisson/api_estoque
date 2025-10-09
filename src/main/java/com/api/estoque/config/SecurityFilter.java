package com.api.estoque.config;

import com.api.estoque.repository.UsuarioRepository;
import com.api.estoque.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Marca esta classe como um componente gerenciado pelo Spring
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // 1. Recupera o token do cabeçalho da requisição
        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            // 2. Se um token foi enviado, valida-o e extrai o nome do utilizador
            var subject = tokenService.getSubject(tokenJWT);
            // 3. Busca o utilizador completo na base de dados
            UserDetails usuario = usuarioRepository.findByNome(subject);

            // 4. Cria o objeto de autenticação para o Spring
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());

            // 5. Define o utilizador como autenticado no contexto de segurança do Spring
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 6. Continua a cadeia de filtros, quer o utilizador esteja autenticado ou não
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            // O token vem depois do prefixo "Bearer "
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }
}