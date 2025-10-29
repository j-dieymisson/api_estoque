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



                        // ===== Regras de Admin (Exigem login como Admin) =====
                        // Qualquer outra ação em /cargos (que não seja o GET público) exige ser ADMIN
                        .requestMatchers("/usuarios/**", "/cargos/**", "/dashboard/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/aprovar").hasAnyRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/recusar").hasAnyRole("ADMIN")


                        // ===== Regras de Gestor/Admin =====
                        .requestMatchers(HttpMethod.POST, "/solicitacoes/*/devolver-tudo").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers("/historico/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.POST, "/equipamentos", "/categorias").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PUT, "/equipamentos/**", "/categorias/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.DELETE, "/equipamentos/**", "/categorias/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PATCH, "/equipamentos/**").hasAnyRole("ADMIN", "GESTOR")

                        // ===== Regra Final =====
                        // Qualquer outra requisição (como criar uma solicitação) exige apenas autenticação
                        .requestMatchers(HttpMethod.GET, "/perfil").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/solicitacoes/*/cancelar").authenticated()

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