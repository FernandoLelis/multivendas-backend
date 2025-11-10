package com.fernando.erp_vendas.dto;

import com.fernando.erp_vendas.model.Venda;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class VendaDTO {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime data;

    private String idPedido;
    private String plataforma;
    private Integer quantidade;

    // ✅ CORREÇÃO CRÍTICA: Adicionar produtoId para o frontend poder selecionar no select
    private Long produtoId;
    private String produtoNome;
    private String produtoSku;

    private Double precoVenda;
    private Double fretePagoPeloCliente;
    private Double custoEnvio;
    private Double tarifaPlataforma;
    private Double custoProdutoVendido;
    private Double despesasOperacionais;

    // ✅ CORREÇÃO: Adicionar campos de cálculo para o frontend
    private Double faturamento;
    private Double custoEfetivoTotal;
    private Double lucroBruto;
    private Double lucroLiquido;
    private Double roi;

    public VendaDTO(Venda venda) {
        this.id = venda.getId();

        // ✅ CORREÇÃO: Garantir que data nunca seja null
        this.data = venda.getData() != null ? venda.getData() : LocalDateTime.now();

        this.idPedido = venda.getIdPedido();
        this.plataforma = venda.getPlataforma();
        this.quantidade = venda.getQuantidade();

        // ✅ CORREÇÃO CRÍTICA: Incluir produtoId, produtoNome e produtoSku
        if (venda.getProduto() != null) {
            this.produtoId = venda.getProduto().getId();
            this.produtoNome = venda.getProduto().getNome();
            this.produtoSku = venda.getProduto().getSku();
        } else {
            this.produtoId = null;
            this.produtoNome = "Produto não encontrado";
            this.produtoSku = "";
        }

        // ✅ TRATAMENTO CRÍTICO: Garantir que nenhum campo Double seja null
        this.precoVenda = venda.getPrecoVenda() != null ? venda.getPrecoVenda() : 0.0;
        this.fretePagoPeloCliente = venda.getFretePagoPeloCliente() != null ? venda.getFretePagoPeloCliente() : 0.0;
        this.custoEnvio = venda.getCustoEnvio() != null ? venda.getCustoEnvio() : 0.0;
        this.tarifaPlataforma = venda.getTarifaPlataforma() != null ? venda.getTarifaPlataforma() : 0.0;
        this.custoProdutoVendido = venda.getCustoProdutoVendido() != null ? venda.getCustoProdutoVendido() : 0.0;
        this.despesasOperacionais = venda.getDespesasOperacionais() != null ? venda.getDespesasOperacionais() : 0.0;

        // ✅ CORREÇÃO: Calcular e incluir os campos financeiros
        this.faturamento = calcularFaturamento();
        this.custoEfetivoTotal = calcularCustoEfetivoTotal();
        this.lucroBruto = calcularLucroBruto();
        this.lucroLiquido = calcularLucroLiquido();
        this.roi = calcularROI();

        System.out.println("✅ VendaDTO criado - ID: " + this.id +
                ", Data: " + this.data +
                ", ProdutoId: " + this.produtoId +
                ", Produto: " + this.produtoNome);
    }

    // ✅ MÉTODOS DE CÁLCULO
    private Double calcularFaturamento() {
        Double precoVendaSafe = this.precoVenda != null ? this.precoVenda : 0.0;
        Double fretePagoSafe = this.fretePagoPeloCliente != null ? this.fretePagoPeloCliente : 0.0;
        return precoVendaSafe + fretePagoSafe;
    }

    private Double calcularCustoEfetivoTotal() {
        Double custoProdutoSafe = this.custoProdutoVendido != null ? this.custoProdutoVendido : 0.0;
        Double custoEnvioSafe = this.custoEnvio != null ? this.custoEnvio : 0.0;
        Double tarifaSafe = this.tarifaPlataforma != null ? this.tarifaPlataforma : 0.0;
        return custoProdutoSafe + custoEnvioSafe + tarifaSafe;
    }

    private Double calcularLucroBruto() {
        Double faturamentoSafe = this.faturamento != null ? this.faturamento : 0.0;
        Double custoEfetivoSafe = this.custoEfetivoTotal != null ? this.custoEfetivoTotal : 0.0;
        return faturamentoSafe - custoEfetivoSafe;
    }

    private Double calcularLucroLiquido() {
        Double lucroBrutoSafe = this.lucroBruto != null ? this.lucroBruto : 0.0;
        Double despesasSafe = this.despesasOperacionais != null ? this.despesasOperacionais : 0.0;
        return lucroBrutoSafe - despesasSafe;
    }

    private Double calcularROI() {
        Double custoEfetivoSafe = this.custoEfetivoTotal != null ? this.custoEfetivoTotal : 0.0;
        Double lucroLiquidoSafe = this.lucroLiquido != null ? this.lucroLiquido : 0.0;

        if (custoEfetivoSafe == 0.0) {
            return 0.0;
        }
        return (lucroLiquidoSafe / custoEfetivoSafe) * 100;
    }

    // GETTERS
    public Long getId() { return id; }
    public LocalDateTime getData() { return data; }
    public String getIdPedido() { return idPedido; }
    public String getPlataforma() { return plataforma; }
    public Integer getQuantidade() { return quantidade; }

    // ✅ CORREÇÃO: Getters para os novos campos
    public Long getProdutoId() { return produtoId != null ? produtoId : 0L; }
    public String getProdutoNome() { return produtoNome != null ? produtoNome : ""; }
    public String getProdutoSku() { return produtoSku != null ? produtoSku : ""; }

    public Double getPrecoVenda() { return precoVenda != null ? precoVenda : 0.0; }
    public Double getFretePagoPeloCliente() { return fretePagoPeloCliente != null ? fretePagoPeloCliente : 0.0; }
    public Double getCustoEnvio() { return custoEnvio != null ? custoEnvio : 0.0; }
    public Double getTarifaPlataforma() { return tarifaPlataforma != null ? tarifaPlataforma : 0.0; }
    public Double getCustoProdutoVendido() { return custoProdutoVendido != null ? custoProdutoVendido : 0.0; }
    public Double getDespesasOperacionais() { return despesasOperacionais != null ? despesasOperacionais : 0.0; }

    // ✅ CORREÇÃO: Getters para campos de cálculo
    public Double getFaturamento() { return faturamento != null ? faturamento : 0.0; }
    public Double getCustoEfetivoTotal() { return custoEfetivoTotal != null ? custoEfetivoTotal : 0.0; }
    public Double getLucroBruto() { return lucroBruto != null ? lucroBruto : 0.0; }
    public Double getLucroLiquido() { return lucroLiquido != null ? lucroLiquido : 0.0; }
    public Double getRoi() { return roi != null ? roi : 0.0; }

    // SETTERS (opcionais, mas úteis para testes)
    public void setId(Long id) { this.id = id; }
    public void setData(LocalDateTime data) { this.data = data; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }
    public void setProdutoSku(String produtoSku) { this.produtoSku = produtoSku; }
    public void setPrecoVenda(Double precoVenda) { this.precoVenda = precoVenda; }
    public void setFretePagoPeloCliente(Double fretePagoPeloCliente) { this.fretePagoPeloCliente = fretePagoPeloCliente; }
    public void setCustoEnvio(Double custoEnvio) { this.custoEnvio = custoEnvio; }
    public void setTarifaPlataforma(Double tarifaPlataforma) { this.tarifaPlataforma = tarifaPlataforma; }
    public void setCustoProdutoVendido(Double custoProdutoVendido) { this.custoProdutoVendido = custoProdutoVendido; }
    public void setDespesasOperacionais(Double despesasOperacionais) { this.despesasOperacionais = despesasOperacionais; }
}