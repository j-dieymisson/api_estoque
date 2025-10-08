package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
@Entity(name = "HistoricoMovimentacao")
@Table(name = "historico_movimentacoes")
public class HistoricoMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_movimentacao")
    private LocalDateTime dataMovimentacao;

    @Enumerated(EnumType.STRING) // Diz ao JPA para salvar o nome do Enum (ex: "SAIDA") no banco
    @Column(name = "tipo_movimentacao")
    private TipoMovimentacao tipoMovimentacao;

    private int quantidade;

    @Column(name = "quantidade_anterior")
    private int quantidadeAnterior;

    @Column(name = "quantidade_posterior")
    private int quantidadePosterior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsavel_id")
    private Usuario usuarioResponsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucao_id")
    private Devolucao devolucao;
}