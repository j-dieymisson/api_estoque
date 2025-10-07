package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

// Anotações do Lombok para gerar getters, setters, construtores e equals/hashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id") // Usa apenas o 'id' para diferenciar objetos

// Anotações do JPA para mapeamento da entidade
@Entity(name = "Categoria") // Diz ao JPA que esta classe é uma entidade
@Table(name = "categorias") // Especifica o nome da tabela no banco de dados
public class Categoria {

    @Id // Marca o campo como a chave primária da tabela
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Diz ao banco para autoincrementar o ID
    private Long id;

    @Column(name = "nome") // Mapeia o campo para a coluna 'nome'
    private String nome;

    @Column(name = "ativa") // Mapeia o campo para a coluna 'ativa'
    private boolean ativa;

}
