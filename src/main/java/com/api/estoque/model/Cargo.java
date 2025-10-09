package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "cargos")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    // Relação Muitos-para-Muitos
    @ManyToMany(fetch = FetchType.EAGER) // EAGER para que as permissões venham sempre junto com o cargo
    @JoinTable(
            name = "cargos_permissoes", // Nome da tabela de ligação
            joinColumns = @JoinColumn(name = "cargo_id"), // Coluna que aponta para esta entidade (Cargo)
            inverseJoinColumns = @JoinColumn(name = "permissao_id") // Coluna que aponta para a outra entidade (Permissao)
    )
    private Set<Permissao> permissoes = new HashSet<>();
}