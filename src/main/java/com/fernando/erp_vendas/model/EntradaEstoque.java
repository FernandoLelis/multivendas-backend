package com.fernando.erp_vendas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entrada_estoque")
public class EntradaEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    @JsonIgnore // ✅ MANTIDO: Evita loop de serialização
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private Integer saldo;

    @Column(name = "custo_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoTotal;

    @Column(name = "custo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoUnitario;

    @Column(name = "data_entrada", nullable = false)
    private LocalDateTime dataEntrada = LocalDateTime.now();

    @Column(name = "fornecedor")
    private String fornecedor;

    @Column(name = "id_pedido_compra")
    private String idPedidoCompra;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    // 🆕 RELAÇÃO COM USUÁRIO - MULTI-TENANCY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // ✅ NOVO: Relacionamento com ItensVenda
    @OneToMany(mappedBy = "lote", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ItemVenda> itensVenda = new ArrayList<>();

    // Construtor padrão
    public EntradaEstoque() {
    }

    // 🆕 CONSTRUTOR ATUALIZADO COM USER
    public EntradaEstoque(Produto produto, Integer quantidade, BigDecimal custoTotal,
                          String fornecedor, String idPedidoCompra, String categoria,
                          String observacoes, User user) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.saldo = quantidade;
        this.custoTotal = custoTotal;
        this.fornecedor = fornecedor;
        this.idPedidoCompra = idPedidoCompra;
        this.categoria = categoria;
        this.observacoes = observacoes;
        this.user = user;

        // ✅ CORREÇÃO: Cálculo SEGURO do custo unitário
        if (quantidade != null && quantidade > 0) {
            try {
                this.custoUnitario = custoTotal.divide(
                        BigDecimal.valueOf(quantidade),
                        2,
                        java.math.RoundingMode.HALF_UP
                );
            } catch (ArithmeticException e) {
                double custoUnitarioDouble = custoTotal.doubleValue() / quantidade;
                this.custoUnitario = BigDecimal.valueOf(custoUnitarioDouble)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
        } else {
            this.custoUnitario = BigDecimal.ZERO;
        }
    }

    // ... (todos os outros métodos permanecem IGUAIS - não mude nada além do que já está)

    // Getters e Setters (mantenha todos como estão)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Integer getSaldo() { return saldo; }
    public void setSaldo(Integer saldo) { this.saldo = saldo; }

    public BigDecimal getCustoTotal() { return custoTotal; }
    public void setCustoTotal(BigDecimal custoTotal) { this.custoTotal = custoTotal; }

    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public String getFornecedor() { return fornecedor; }
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }

    public String getIdPedidoCompra() { return idPedidoCompra; }
    public void setIdPedidoCompra(String idPedidoCompra) { this.idPedidoCompra = idPedidoCompra; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<ItemVenda> getItensVenda() { return itensVenda; }
    public void setItensVenda(List<ItemVenda> itensVenda) { this.itensVenda = itensVenda; }

    // Métodos de negócio (mantenha todos)
    public boolean baixarEstoque(Integer quantidadeBaixa) {
        if (saldo >= quantidadeBaixa) {
            saldo -= quantidadeBaixa;
            return true;
        }
        return false;
    }

    public void adicionarItemVenda(ItemVenda itemVenda) {
        if (this.itensVenda == null) {
            this.itensVenda = new ArrayList<>();
        }
        itemVenda.setLote(this);
        this.itensVenda.add(itemVenda);
    }

    @Override
    public String toString() {
        return "EntradaEstoque{" +
                "id=" + id +
                ", produto=" + (produto != null ? produto.getNome() : "null") +
                ", quantidade=" + quantidade +
                ", saldo=" + saldo +
                ", custoUnitario=" + custoUnitario +
                ", userId=" + (user != null ? user.getId() : "null") +
                ", dataEntrada=" + dataEntrada +
                ", fornecedor='" + fornecedor + '\'' +
                ", idPedidoCompra='" + idPedidoCompra + '\'' +
                ", categoria='" + categoria + '\'' +
                '}';
    }
}