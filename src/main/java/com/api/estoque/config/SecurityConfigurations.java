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
import org.springframework.security.config.Customizer;
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
                        // ===== Endpoints Públicos Essenciais para o Arranque =====
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cargos").permitAll() // <-- TORNAR PÚBLICO para sabermos os IDs

                        // ===== Regras de Admin (Exigem login como Admin) =====
                        // Qualquer outra ação em /cargos (que não seja o GET público) exige ser ADMIN
                        .requestMatchers("/cargos/**").hasRole("ADMIN")
                        .requestMatchers("/equipamentos/**").hasRole("ADMIN")
                        .requestMatchers("/usuarios/**").hasRole("ADMIN") // Qualquer outra ação em /usuarios exige ser ADMIN

                        // ===== Regras de Gestor/Admin =====
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/aprovar").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/recusar").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/cancelar").hasRole("ADMIN") // Vamos proteger este também
                        .requestMatchers(HttpMethod.POST, "/solicitacoes/*/devolver-tudo").hasAnyRole("ADMIN", "GESTOR")

                                                // ===== Regra Final =====
                        // Qualquer outra requisição (como criar uma solicitação) exige apenas autenticação
                        .requestMatchers(HttpMethod.GET, "/perfil").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}