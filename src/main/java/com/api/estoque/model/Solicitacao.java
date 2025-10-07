package com.api.estoque.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity(name = "Solicitacao")
@Table(name = "solicitacoes")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_solicitacao")
    private LocalDateTime dataSolicitacao;

    // Usar um Enum para status é uma prática muito melhor do que usar Strings.
    // Vamos criar o Enum em breve. Por enquanto, a anotação @Enumerated já prepara o campo.
    @Enumerated(EnumType.STRING)
    private StatusSolicitacao status;

    private String justificativa;

    // --- Relacionamento com Usuario ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // --- Relacionamento com SolicitacaoItem ---
    // Uma solicitação tem uma lista de itens.
    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolicitacaoItem> itens = new ArrayList<>();

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL)
    private List<HistoricoStatusSolicitacao> historicoStatus = new ArrayList<>();

    // Método utilitário para adicionar itens de forma segura
    public void adicionarItem(SolicitacaoItem item) {
        itens.add(item);
        item.setSolicitacao(this);
    }
}