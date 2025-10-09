package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity(name = "Usuario")
@Table(name = "usuarios")
public class Usuario {

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

}