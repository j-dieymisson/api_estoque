package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity(name = "Equipamento")
@Table(name = "equipamentos")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;

    // A anotação @Column é útil para manter o padrão camelCase no Java
    // enquanto o snake_case é usado no banco de dados.
    @Column(name = "quantidade_total")
    private int quantidadeTotal;

    @Column(name = "quantidade_disponivel")
    private int quantidadeDisponivel;

    private boolean ativo;

    // --- Relacionamento com Categoria ---
    @ManyToOne(fetch = FetchType.LAZY) // Define a relação: Muitos Equipamentos para UMA Categoria.
    @JoinColumn(name = "categoria_id") // Especifica qual coluna na tabela 'equipamentos' é a chave estrangeira.
    private Categoria categoria;

    @CreationTimestamp // Diz ao Hibernate para preencher este campo automaticamente na criação
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
}