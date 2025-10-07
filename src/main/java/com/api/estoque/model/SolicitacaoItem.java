package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity(name = "SolicitacaoItem")
@Table(name = "solicitacao_itens")
public class SolicitacaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantidade_solicitada")
    private int quantidadeSolicitada;

    // --- Relacionamento com Solicitacao ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    // --- Relacionamento com Equipamento ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    // Um item de solicitação pode ter várias devoluções associadas a ele.
    @OneToMany(mappedBy = "solicitacaoItem", cascade = CascadeType.ALL)
    private List<Devolucao> devolucoes = new ArrayList<>();

    // Método utilitário para calcular o total já devolvido para este item
    public int getTotalDevolvido() {
        return devolucoes.stream()
                .mapToInt(Devolucao::getQuantidadeDevolvida)
                .sum();
    }

}