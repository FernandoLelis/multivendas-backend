package com.fernando.erp_vendas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_venda")
public class ItemVenda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venda_id", nullable = false)
    @JsonIgnore
    private Venda venda;

    // ✅✅✅ NOVO CAMPO v46.8: RELAÇÃO DIRETA COM PRODUTO
    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    @JsonIgnore
    private Produto produto;

    // ⚠️ CAMPO MANTIDO: O lote ainda existe, mas pode ser nulo inicialmente
    @ManyToOne
    @JoinColumn(name = "lote_id") // ✅ REMOVIDO nullable = false (agora pode ser nulo)
    @JsonIgnore
    private EntradaEstoque lote;

    @Column(nullable = false)
    private Integer quantidade;

    // ✅✅✅ CORREÇÃO CRÍTICA: REMOVIDO nullable = false E INICIALIZADO COM ZERO
    @Column(name = "custo_unitario", precision = 10, scale = 2)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(name = "preco_unitario", precision = 10, scale = 2)
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // ✅✅✅ NOVO CAMPO v46.8: PARA RASTREAR SE JÁ FOI PROCESSADO PELO PEPS
    @Column(name = "processado_peps", nullable = false)
    private Boolean processadoPeps = false;

    // ✅ CONSTRUTOR PADRÃO (OBRIGATÓRIO PARA JPA)
    public ItemVenda() {
        // ✅ INICIALIZAR TODOS OS BigDecimals PARA EVITAR NULL
        this.custoUnitario = BigDecimal.ZERO;
        this.precoUnitario = BigDecimal.ZERO;
        this.processadoPeps = false;
    }

    // ✅✅✅ CONSTRUTOR ATUALIZADO v46.8: Recebe Produto em vez de EntradaEstoque
    public ItemVenda(Venda venda, Produto produto, Integer quantidade,
                     BigDecimal precoUnitario, User user) {
        this(); // Chama construtor padrão para inicializar

        this.venda = venda;
        this.produto = produto;
        this.quantidade = quantidade;

        if (precoUnitario != null) {
            this.precoUnitario = precoUnitario;
        }

        this.user = user;
        this.processadoPeps = false; // Ainda não processado pelo PEPS
        // custoUnitario já é BigDecimal.ZERO pelo construtor padrão
    }

    // ✅ CONSTRUTOR COMPATÍVEL (para código existente)
    public ItemVenda(Venda venda, EntradaEstoque lote, Integer quantidade,
                     BigDecimal custoUnitario, User user) {
        this(); // Chama construtor padrão

        this.venda = venda;
        this.lote = lote;
        this.produto = lote != null ? lote.getProduto() : null;
        this.quantidade = quantidade;

        if (custoUnitario != null) {
            this.custoUnitario = custoUnitario;
        }

        this.user = user;
        this.processadoPeps = true; // Já vem com lote, considerado processado
    }

    // ✅ CONSTRUTOR COMPLETO (para testes e flexibilidade)
    public ItemVenda(Venda venda, Produto produto, EntradaEstoque lote,
                     Integer quantidade, BigDecimal custoUnitario,
                     BigDecimal precoUnitario, User user, Boolean processadoPeps) {
        this(); // Chama construtor padrão

        this.venda = venda;
        this.produto = produto;
        this.lote = lote;
        this.quantidade = quantidade;

        if (custoUnitario != null) {
            this.custoUnitario = custoUnitario;
        }

        if (precoUnitario != null) {
            this.precoUnitario = precoUnitario;
        }

        this.user = user;

        if (processadoPeps != null) {
            this.processadoPeps = processadoPeps;
        }
    }

    // ========== GETTERS E SETTERS ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Venda getVenda() {
        return venda;
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
    }

    // ✅ NOVO: Getter/Setter para produto
    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public EntradaEstoque getLote() {
        return lote;
    }

    public void setLote(EntradaEstoque lote) {
        this.lote = lote;
        // Se definir lote e produto for null, define automaticamente
        if (this.produto == null && lote != null && lote.getProduto() != null) {
            this.produto = lote.getProduto();
        }
    }

    public Integer getQuantidade() {
        return quantidade != null ? quantidade : 0;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getCustoUnitario() {
        // ✅ GARANTIR QUE NUNCA RETORNE NULL
        if (custoUnitario == null) {
            custoUnitario = BigDecimal.ZERO;
        }
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario != null ? custoUnitario : BigDecimal.ZERO;
    }

    public BigDecimal getPrecoUnitario() {
        // ✅ GARANTIR QUE NUNCA RETORNE NULL
        if (precoUnitario == null) {
            precoUnitario = BigDecimal.ZERO;
        }
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario != null ? precoUnitario : BigDecimal.ZERO;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getProcessadoPeps() {
        return processadoPeps != null ? processadoPeps : false;
    }

    public void setProcessadoPeps(Boolean processadoPeps) {
        this.processadoPeps = processadoPeps != null ? processadoPeps : false;
    }

    // ========== MÉTODOS DE NEGÓCIO ==========

    // ✅ MÉTODO PARA MARCAR COMO PROCESSADO PELO PEPS
    public void marcarComoProcessado(EntradaEstoque lote, BigDecimal custoUnitario) {
        this.lote = lote;

        if (custoUnitario != null) {
            this.custoUnitario = custoUnitario;
        }

        this.processadoPeps = true;

        // Garantir que produto está definido
        if (this.produto == null && lote != null) {
            this.produto = lote.getProduto();
        }
    }

    // ✅ MÉTODO PARA CALCULAR CUSTO TOTAL DO ITEM
    public BigDecimal getCustoTotal() {
        return getCustoUnitario().multiply(BigDecimal.valueOf(getQuantidade()));
    }

    // ✅ MÉTODO PARA CALCULAR PREÇO TOTAL DO ITEM
    public BigDecimal getPrecoTotal() {
        return getPrecoUnitario().multiply(BigDecimal.valueOf(getQuantidade()));
    }

    // ✅ MÉTODO PARA CALCULAR LUCRO DO ITEM
    public BigDecimal getLucro() {
        return getPrecoTotal().subtract(getCustoTotal());
    }

    // ✅ MÉTODO PARA VERIFICAR SE É VÁLIDO
    public boolean isValid() {
        return getQuantidade() > 0 &&
                produto != null &&
                user != null &&
                venda != null;
    }

    // ✅ MÉTODO PARA VERIFICAR SE TEM TODOS OS DADOS DO PEPS
    public boolean temDadosPepsCompletos() {
        return getProcessadoPeps() &&
                lote != null &&
                getCustoUnitario().compareTo(BigDecimal.ZERO) > 0;
    }

    // ✅ MÉTODO PARA CLONAR (ÚTIL PARA TESTES)
    public ItemVenda clone() {
        ItemVenda clone = new ItemVenda();
        clone.setId(this.id);
        clone.setVenda(this.venda);
        clone.setProduto(this.produto);
        clone.setLote(this.lote);
        clone.setQuantidade(this.quantidade);
        clone.setCustoUnitario(this.custoUnitario);
        clone.setPrecoUnitario(this.precoUnitario);
        clone.setUser(this.user);
        clone.setProcessadoPeps(this.processadoPeps);
        return clone;
    }

    @Override
    public String toString() {
        return "ItemVenda{" +
                "id=" + id +
                ", venda=" + (venda != null ? venda.getIdPedido() : "null") +
                ", produto=" + (produto != null ? produto.getNome() : "null") +
                ", lote=" + (lote != null ? lote.getId() : "null") +
                ", quantidade=" + getQuantidade() +
                ", custoUnitario=" + getCustoUnitario() +
                ", precoUnitario=" + getPrecoUnitario() +
                ", processadoPeps=" + getProcessadoPeps() +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemVenda itemVenda = (ItemVenda) o;
        return id != null && id.equals(itemVenda.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}