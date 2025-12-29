package com.fernando.erp_vendas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_compra")
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "custo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoUnitario;

    @Column(name = "custo_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoTotal;

    @OneToOne(mappedBy = "itemCompra", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EntradaEstoque lote;  // Lote criado a partir deste item

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Construtor padrão
    public ItemCompra() {
    }

    // Construtor com parâmetros
    public ItemCompra(Compra compra, Produto produto, Integer quantidade,
                      BigDecimal custoUnitario, User user) {
        this.compra = compra;
        this.produto = produto;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.custoTotal = custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        this.user = user;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        calcularCustoTotal();
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
        calcularCustoTotal();
    }

    public BigDecimal getCustoTotal() {
        return custoTotal;
    }

    public void setCustoTotal(BigDecimal custoTotal) {
        this.custoTotal = custoTotal;
    }

    public EntradaEstoque getLote() {
        return lote;
    }

    public void setLote(EntradaEstoque lote) {
        this.lote = lote;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Método auxiliar para calcular custo total
    private void calcularCustoTotal() {
        if (custoUnitario != null && quantidade != null) {
            this.custoTotal = custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    @Override
    public String toString() {
        return "ItemCompra{" +
                "id=" + id +
                ", produto=" + (produto != null ? produto.getNome() : "null") +
                ", quantidade=" + quantidade +
                ", custoUnitario=" + custoUnitario +
                ", custoTotal=" + custoTotal +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }
}