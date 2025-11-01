package com.fernando.erp_vendas.dto;

import com.fernando.erp_vendas.model.Venda;
import java.time.LocalDateTime;

public class VendaDTO {
    private Long id;
    private LocalDateTime data;
    private String idPedido;
    private String plataforma;
    private int quantidade;
    private double precoVenda;
    private double custoProdutoVendido;
    private double fretePagoPeloCliente;
    private double custoEnvio;
    private double tarifaPlataforma;
    private double despesasOperacionais;

    // ✅ DADOS DO PRODUTO
    private Long produtoId;
    private String produtoNome;
    private String produtoSku;

    // ✅ CÁLCULOS FINANCEIROS
    private double faturamento;
    private double custoEfetivoTotal;
    private double lucroBruto;
    private double lucroLiquido;
    private double roi;

    // Construtor que recebe Venda
    public VendaDTO(Venda venda) {
        this.id = venda.getId();
        this.data = venda.getData();
        this.idPedido = venda.getIdPedido();
        this.plataforma = venda.getPlataforma();
        this.quantidade = venda.getQuantidade();
        this.precoVenda = venda.getPrecoVenda();
        this.custoProdutoVendido = venda.getCustoProdutoVendido();
        this.fretePagoPeloCliente = venda.getFretePagoPeloCliente();
        this.custoEnvio = venda.getCustoEnvio();
        this.tarifaPlataforma = venda.getTarifaPlataforma();
        this.despesasOperacionais = venda.getDespesasOperacionais();

        // ✅ DADOS DO PRODUTO
        if (venda.getProduto() != null) {
            this.produtoId = venda.getProduto().getId();
            this.produtoNome = venda.getProduto().getNome();
            this.produtoSku = venda.getProduto().getSku();
        }

        // ✅ CÁLCULOS FINANCEIROS
        this.faturamento = venda.calcularFaturamento();
        this.custoEfetivoTotal = venda.calcularCustoEfetivoTotal();
        this.lucroBruto = venda.calcularLucroBruto();
        this.lucroLiquido = venda.calcularLucroLiquido();
        this.roi = venda.calcularROI();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(double precoVenda) { this.precoVenda = precoVenda; }

    public double getCustoProdutoVendido() { return custoProdutoVendido; }
    public void setCustoProdutoVendido(double custoProdutoVendido) { this.custoProdutoVendido = custoProdutoVendido; }

    public double getFretePagoPeloCliente() { return fretePagoPeloCliente; }
    public void setFretePagoPeloCliente(double fretePagoPeloCliente) { this.fretePagoPeloCliente = fretePagoPeloCliente; }

    public double getCustoEnvio() { return custoEnvio; }
    public void setCustoEnvio(double custoEnvio) { this.custoEnvio = custoEnvio; }

    public double getTarifaPlataforma() { return tarifaPlataforma; }
    public void setTarifaPlataforma(double tarifaPlataforma) { this.tarifaPlataforma = tarifaPlataforma; }

    public double getDespesasOperacionais() { return despesasOperacionais; }
    public void setDespesasOperacionais(double despesasOperacionais) { this.despesasOperacionais = despesasOperacionais; }

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }

    public String getProdutoSku() { return produtoSku; }
    public void setProdutoSku(String produtoSku) { this.produtoSku = produtoSku; }

    public double getFaturamento() { return faturamento; }
    public void setFaturamento(double faturamento) { this.faturamento = faturamento; }

    public double getCustoEfetivoTotal() { return custoEfetivoTotal; }
    public void setCustoEfetivoTotal(double custoEfetivoTotal) { this.custoEfetivoTotal = custoEfetivoTotal; }

    public double getLucroBruto() { return lucroBruto; }
    public void setLucroBruto(double lucroBruto) { this.lucroBruto = lucroBruto; }

    public double getLucroLiquido() { return lucroLiquido; }
    public void setLucroLiquido(double lucroLiquido) { this.lucroLiquido = lucroLiquido; }

    public double getRoi() { return roi; }
    public void setRoi(double roi) { this.roi = roi; }
}