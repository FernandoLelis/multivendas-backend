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
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private Venda venda;

    @ManyToOne
    @JoinColumn(name = "lote_id", nullable = false)
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private EntradaEstoque lote;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "custo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoUnitario;

    // 🆕 RELAÇÃO COM USUÁRIO - MULTI-TENANCY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private User user;

    // Construtor padrão
    public ItemVenda() {
    }

    // 🆕 CONSTRUTOR ATUALIZADO COM USER
    public ItemVenda(Venda venda, EntradaEstoque lote, Integer quantidade,
                     BigDecimal custoUnitario, User user) {
        this.venda = venda;
        this.lote = lote;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.user = user;
    }

    // Getters e Setters
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

    public EntradaEstoque getLote() {
        return lote;
    }

    public void setLote(EntradaEstoque lote) {
        this.lote = lote;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
    }

    // 🆕 GETTER E SETTER PARA USER
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Método para calcular custo total do item
    public BigDecimal getCustoTotal() {
        return custoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    @Override
    public String toString() {
        return "ItemVenda{" +
                "id=" + id +
                ", venda=" + (venda != null ? venda.getIdPedido() : "null") +
                ", lote=" + (lote != null ? lote.getId() : "null") +
                ", quantidade=" + quantidade +
                ", custoUnitario=" + custoUnitario +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }
}