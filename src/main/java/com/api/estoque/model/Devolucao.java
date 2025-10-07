package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity(name = "Devolucao")
@Table(name = "devolucoes")
public class Devolucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantidade_devolvida")
    private int quantidadeDevolvida;

    @Column(name = "data_devolucao")
    private LocalDateTime dataDevolucao;

    private String observacao;

    // --- Relacionamento com SolicitacaoItem ---
    // Muitas devoluções podem estar associadas a um único item de solicitação
    // (Ex: devolver 2 itens hoje e 3 amanhã do mesmo pedido)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_item_id")
    private SolicitacaoItem solicitacaoItem;

}