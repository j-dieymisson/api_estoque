package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Set;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity(name = "Usuario")
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String login;
    private String senha;
    private boolean ativo;
    private String email;
    @ManyToOne(fetch = FetchType.EAGER) // Muitos Utilizadores podem ter UM Cargo
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<PreferenciaDashboard> preferenciasDashboard;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.cargo == null) {
            return List.of(); // Se não tiver cargo, não tem nenhuma permissão.
        }
        // A autoridade do utilizador é o seu cargo, com o prefixo ROLE_ que o Spring espera.
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.cargo.getNome().toUpperCase()));
    }


    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        // O nosso sistema de login usa o 'nome', então retornamos o nome.
        return this.nome;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Podemos adicionar lógicas de expiração no futuro
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Podemos adicionar lógicas de bloqueio no futuro
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Podemos adicionar lógicas de expiração de senha
    }

    @Override
    public boolean isEnabled() {
        // Usamos o nosso campo 'ativo' para controlar se o utilizador está habilitado.
        return this.ativo;
    }

}