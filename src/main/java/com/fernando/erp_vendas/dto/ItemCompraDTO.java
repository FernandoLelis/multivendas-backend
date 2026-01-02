package com.fernando.erp_vendas.dto;

import java.math.BigDecimal;

public class ItemCompraDTO {
    private Long id; // ID do ItemCompra (opcional para criação)
    private Long produtoId;
    private String produtoNome;
    private String produtoSku;
    private Integer quantidade;
    private BigDecimal custoUnitario;
    private BigDecimal custoTotal;
    private Long loteId; // ID do lote criado (EntradaEstoque) - opcional na criação
    private Long userId;

    // Construtor padrão (para criação)
    public ItemCompraDTO() {
    }

    // Construtor para criação (sem id do ItemCompra)
    public ItemCompraDTO(Long produtoId, String produtoNome, String produtoSku,
                         Integer quantidade, BigDecimal custoUnitario, Long loteId, Long userId) {
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.produtoSku = produtoSku;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.loteId = loteId;
        this.userId = userId;
        this.custoTotal = custoUnitario != null && quantidade != null
                ? custoUnitario.multiply(BigDecimal.valueOf(quantidade))
                : BigDecimal.ZERO;
    }

    // Construtor completo (com id do ItemCompra)
    public ItemCompraDTO(Long id, Long produtoId, String produtoNome, String produtoSku,
                         Integer quantidade, BigDecimal custoUnitario, BigDecimal custoTotal,
                         Long loteId, Long userId) {
        this.id = id;
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.produtoSku = produtoSku;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.custoTotal = custoTotal;
        this.loteId = loteId;
        this.userId = userId;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }

    public String getProdutoSku() { return produtoSku; }
    public void setProdutoSku(String produtoSku) { this.produtoSku = produtoSku; }

    public Integer getQuantidade() { return quantidade != null ? quantidade : 0; }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        calcularCustoTotal();
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario != null ? custoUnitario : BigDecimal.ZERO;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
        calcularCustoTotal();
    }

    public BigDecimal getCustoTotal() {
        if (custoTotal == null) {
            calcularCustoTotal();
        }
        return custoTotal != null ? custoTotal : BigDecimal.ZERO;
    }

    public void setCustoTotal(BigDecimal custoTotal) { this.custoTotal = custoTotal; }

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    // Método auxiliar para calcular custo total
    public void calcularCustoTotal() {
        if (custoUnitario != null && quantidade != null && quantidade > 0) {
            this.custoTotal = custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    @Override
    public String toString() {
        return "ItemCompraDTO{" +
                "id=" + id +
                ", produtoId=" + produtoId +
                ", produtoNome='" + produtoNome + '\'' +
                ", produtoSku='" + produtoSku + '\'' +
                ", quantidade=" + quantidade +
                ", custoUnitario=" + custoUnitario +
                ", custoTotal=" + custoTotal +
                ", loteId=" + loteId +
                ", userId=" + userId +
                '}';
    }
}