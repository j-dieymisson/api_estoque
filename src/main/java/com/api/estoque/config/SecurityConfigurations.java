package com.api.estoque.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer; // Importar este
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurations {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // Esta é a forma mais simples e moderna de obter o AuthenticationManager
        // que o Spring Boot auto-configura para usar o nosso AuthenticationService.
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoints Públicos (não precisam de login)
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()


                        // Endpoints de Gestão (só para ADMIN)
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/cargos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/equipamentos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/equipamentos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/equipamentos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/equipamentos/**/ajustar-estoque").hasRole("ADMIN")

                        // Endpoints de Aprovação (ADMIN ou um futuro MANAGER)
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/**/aprovar").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/**/recusar").hasAnyRole("ADMIN", "MANAGER")

                        // Qualquer outra requisição (ex: GET /equipamentos, POST /solicitacoes)
                        // só precisa de que o utilizador esteja autenticado.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}