package com.api.estoque.service;

import com.api.estoque.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements UserDetailsService {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // O Spring Security chama este método quando um utilizador tenta fazer login.
        // A nossa lógica é simplesmente procurar o utilizador na nossa tabela 'usuarios' pelo nome.
        UserDetails user = repository.findByNome(username);
        if (user == null) {
            throw new UsernameNotFoundException("Utilizador não encontrado: " + username);
        }
        return user;
    }
}