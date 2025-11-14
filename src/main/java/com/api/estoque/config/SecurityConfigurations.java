package com.api.estoque.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Autowired
    private SecurityFilter securityFilter;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // ===== Endpoints Públicos Essenciais para o Arranque =====
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/login.html", "/app/**", "/css/**", "/js/**").permitAll()

                        // =========================================================
                        // === NOVAS REGRAS DE SETORES (CORREÇÃO DO BUG) ===
                        // =========================================================
                        // 1. GESTOR/ADMIN podem LER (GET) a lista de setores (para os dropdowns)
                        .requestMatchers(HttpMethod.GET, "/setores", "/setores/**").hasAnyRole("ADMIN", "GESTOR")
                        // 2. Apenas ADMIN pode ESCREVER (Criar, Editar, Apagar)
                        .requestMatchers("/setores", "/setores/**").hasRole("ADMIN")
                        // =========================================================

                        // ===== Regras de Admin (Exigem login como Admin) =====
                        .requestMatchers("/dashboard/**").hasRole("ADMIN")
                        // --- REGRAS DE APROVAÇÃO ANTIGAS (CORRIGIDAS) ---
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/aprovar-admin").hasRole("ADMIN") // <-- ALTERAÇÃO
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/recusar").hasAnyRole("ADMIN", "GESTOR") // <-- ALTERAÇÃO (Gestor agora pode recusar)
                        .requestMatchers(HttpMethod.GET, "/solicitacoes/pendentes/contagem").hasAnyRole("ADMIN", "GESTOR") // <-- ALTERAÇÃO (Gestor agora vê a contagem dele)

                        // --- NOVA REGRA DE APROVAÇÃO (GESTOR) ---
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/aprovar-gestor").hasRole("GESTOR") // <-- ADICIONADO

                        // ===== Regras de Gestor/Admin =====
                        .requestMatchers("/usuarios/**", "/cargos/**", "/historico/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.POST, "/equipamentos", "/categorias").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PUT, "/equipamentos/**", "/categorias/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.DELETE, "/equipamentos/**", "/categorias/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PATCH, "/equipamentos/**").hasAnyRole("ADMIN", "GESTOR")

                        // ===== Regra Final =====
                        // Qualquer outra requisição (como criar uma solicitação) exige apenas autenticação
                        .requestMatchers(HttpMethod.GET, "/perfil").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/cancelar").authenticated()
                        .requestMatchers(HttpMethod.POST, "/solicitacoes/*/devolver-tudo").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}