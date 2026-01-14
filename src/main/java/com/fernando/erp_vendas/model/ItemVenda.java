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

    // ✅✅✅ CORREÇÃO v46.6: ADICIONAR CAMPO precoUnitario (PERMITE NULL PARA COMPATIBILIDADE)
    @Column(name = "preco_unitario", precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    // 🆕 RELAÇÃO COM USUÁRIO - MULTI-TENANCY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private User user;

    // Construtor padrão - MANTIDO EXATAMENTE COMO ESTAVA
    public ItemVenda() {
    }

    // 🆕 CONSTRUTOR ATUALIZADO COM USER - MANTIDO EXATAMENTE COMO ESTAVA
    // (NÃO MODIFICAR PARA NÃO QUEBRAR CÓDIGO EXISTENTE)
    public ItemVenda(Venda venda, EntradaEstoque lote, Integer quantidade,
                     BigDecimal custoUnitario, User user) {
        this.venda = venda;
        this.lote = lote;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.user = user;
    }

    // Getters e Setters EXISTENTES - MANTIDOS INTACTOS
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

    // 🆕 GETTER E SETTER PARA USER - MANTIDO
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // ✅✅✅ ADICIONAR: GETTER E SETTER PARA precoUnitario (NOVO)
    public BigDecimal getPrecoUnitario() {
        return precoUnitario != null ? precoUnitario : BigDecimal.ZERO;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    // Método para calcular custo total do item - MANTIDO
    public BigDecimal getCustoTotal() {
        return custoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    // ✅✅✅ ADICIONAR: Método para calcular preço total do item (NOVO)
    public BigDecimal getPrecoTotal() {
        if (precoUnitario != null && quantidade != null) {
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "ItemVenda{" +
                "id=" + id +
                ", venda=" + (venda != null ? venda.getIdPedido() : "null") +
                ", lote=" + (lote != null ? lote.getId() : "null") +
                ", quantidade=" + quantidade +
                ", custoUnitario=" + custoUnitario +
                ", precoUnitario=" + precoUnitario + // ✅ ADICIONADO
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }
}