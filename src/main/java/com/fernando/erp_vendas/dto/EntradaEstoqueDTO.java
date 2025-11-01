package com.fernando.erp_vendas.dto;

import com.fernando.erp_vendas.model.EntradaEstoque;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EntradaEstoqueDTO {
    private Long id;
    private Long produtoId;
    private String produtoNome;
    private String produtoSku;
    private Integer quantidade;
    private Integer saldo;
    private BigDecimal custoTotal;
    private BigDecimal custoUnitario;
    private LocalDateTime dataEntrada;
    private String fornecedor;
    private String idPedidoCompra;
    private String categoria;
    private String observacoes;

    // Construtor que recebe EntradaEstoque
    public EntradaEstoqueDTO(EntradaEstoque entrada) {
        this.id = entrada.getId();
        if (entrada.getProduto() != null) {
            this.produtoId = entrada.getProduto().getId();
            this.produtoNome = entrada.getProduto().getNome();
            this.produtoSku = entrada.getProduto().getSku();
        }
        this.quantidade = entrada.getQuantidade();
        this.saldo = entrada.getSaldo();
        this.custoTotal = entrada.getCustoTotal();
        this.custoUnitario = entrada.getCustoUnitario();
        this.dataEntrada = entrada.getDataEntrada();
        this.fornecedor = entrada.getFornecedor();
        this.idPedidoCompra = entrada.getIdPedidoCompra();
        this.categoria = entrada.getCategoria();
        this.observacoes = entrada.getObservacoes();
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
}