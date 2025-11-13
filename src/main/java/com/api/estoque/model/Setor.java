package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity(name = "Setor")
@Table(name = "setores")
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    // Adicionamos um campo 'ativo' para que setores possam ser desativados
    private boolean ativo;

    // (Poderíamos adicionar @OneToMany List<Usuario> usuarios aqui,
    // mas vamos manter simples por agora e deixar a ligação apenas no Usuario)
}