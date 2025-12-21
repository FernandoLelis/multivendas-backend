package com.fernando.erp_vendas.dto;

import com.fernando.erp_vendas.model.Venda;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VendaDTO {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime data;

    private String idPedido;
    private String plataforma;

    // ✅ NOVO: Lista de produtos da venda
    private List<ItemVendaDTO> itens = new ArrayList<>();

    // Preços e custos da venda (agregados de todos os itens)
    private Double precoVenda; // ⚠️ Preço TOTAL da venda (soma de todos os produtos)
    private Double fretePagoPeloCliente;
    private Double custoEnvio;
    private Double tarifaPlataforma;
    private Double custoProdutoVendido; // ✅ Soma dos custos de todos os itens (calculado pelo PEPS)
    private Double despesasOperacionais;

    // Campos de cálculo para o frontend
    private Double faturamento;
    private Double custoEfetivoTotal;
    private Double lucroBruto;
    private Double lucroLiquido;
    private Double roi;

    // Construtor a partir da entidade Venda
    public VendaDTO(Venda venda) {
        this.id = venda.getId();

        // ✅ CORREÇÃO: Garantir que data nunca seja null
        this.data = venda.getData() != null ? venda.getData() : LocalDateTime.now();

        this.idPedido = venda.getIdPedido();
        this.plataforma = venda.getPlataforma();

        // ✅ NOVO: Converter itens da venda para ItemVendaDTO
        if (venda.getItens() != null && !venda.getItens().isEmpty()) {
            this.itens = venda.getItens().stream()
                    .map(item -> new ItemVendaDTO(
                            item.getId(),
                            item.getLote() != null && item.getLote().getProduto() != null
                                    ? item.getLote().getProduto().getId() : null,
                            item.getLote() != null && item.getLote().getProduto() != null
                                    ? item.getLote().getProduto().getNome() : "Produto não encontrado",
                            item.getLote() != null && item.getLote().getProduto() != null
                                    ? item.getLote().getProduto().getSku() : "",
                            item.getQuantidade(),
                            item.getCustoUnitario(),
                            item.getLote() != null ? item.getLote().getId() : null
                    ))
                    .collect(Collectors.toList());
        }

        // ✅ TRATAMENTO CRÍTICO: Garantir que nenhum campo Double seja null
        this.precoVenda = venda.getPrecoVenda() != null ? venda.getPrecoVenda() : 0.0;
        this.fretePagoPeloCliente = venda.getFretePagoPeloCliente() != null ? venda.getFretePagoPeloCliente() : 0.0;
        this.custoEnvio = venda.getCustoEnvio() != null ? venda.getCustoEnvio() : 0.0;
        this.tarifaPlataforma = venda.getTarifaPlataforma() != null ? venda.getTarifaPlataforma() : 0.0;
        this.custoProdutoVendido = venda.getCustoProdutoVendido() != null ? venda.getCustoProdutoVendido() : 0.0;
        this.despesasOperacionais = venda.getDespesasOperacionais() != null ? venda.getDespesasOperacionais() : 0.0;

        // ✅ CORREÇÃO: Usar métodos da entidade para cálculos consistentes
        this.faturamento = venda.calcularFaturamento();
        this.custoEfetivoTotal = venda.calcularCustoEfetivoTotal();
        this.lucroBruto = venda.calcularLucroBruto();
        this.lucroLiquido = venda.calcularLucroLiquido();
        this.roi = venda.calcularROI();

        // DEBUG: Verificar cálculo
        System.out.println("✅ VendaDTO - Venda com " + this.itens.size() + " produtos:");
        System.out.println("  Preço Venda (TOTAL): " + this.precoVenda);
        System.out.println("  Quantidade Total: " + this.getQuantidadeTotal());
        System.out.println("  Custo Produtos: " + this.custoProdutoVendido);
        System.out.println("  Faturamento Calculado: " + this.faturamento);
    }

    // Construtor para criação (sem id)
    public VendaDTO() {
    }

    // ✅ NOVO: Métodos auxiliares para quantidade e produtos

    // Quantidade total (soma de todos os itens)
    public Integer getQuantidadeTotal() {
        return itens.stream()
                .mapToInt(ItemVendaDTO::getQuantidade)
                .sum();
    }

    // Custo total dos produtos (soma de todos os itens)
    public BigDecimal getCustoProdutosTotal() {
        return itens.stream()
                .map(ItemVendaDTO::getCustoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Lista de IDs de produtos únicos
    public List<Long> getProdutosIds() {
        return itens.stream()
                .map(ItemVendaDTO::getProdutoId)
                .distinct()
                .collect(Collectors.toList());
    }

    // GETTERS
    public Long getId() { return id; }
    public LocalDateTime getData() { return data; }
    public String getIdPedido() { return idPedido; }
    public String getPlataforma() { return plataforma; }

    // ✅ NOVO: Getter para itens
    public List<ItemVendaDTO> getItens() { return itens; }

    // Getters para campos financeiros (com tratamento null)
    public Double getPrecoVenda() { return precoVenda != null ? precoVenda : 0.0; }
    public Double getFretePagoPeloCliente() { return fretePagoPeloCliente != null ? fretePagoPeloCliente : 0.0; }
    public Double getCustoEnvio() { return custoEnvio != null ? custoEnvio : 0.0; }
    public Double getTarifaPlataforma() { return tarifaPlataforma != null ? tarifaPlataforma : 0.0; }
    public Double getCustoProdutoVendido() { return custoProdutoVendido != null ? custoProdutoVendido : 0.0; }
    public Double getDespesasOperacionais() { return despesasOperacionais != null ? despesasOperacionais : 0.0; }

    // Getters para campos de cálculo
    public Double getFaturamento() { return faturamento != null ? faturamento : 0.0; }
    public Double getCustoEfetivoTotal() { return custoEfetivoTotal != null ? custoEfetivoTotal : 0.0; }
    public Double getLucroBruto() { return lucroBruto != null ? lucroBruto : 0.0; }
    public Double getLucroLiquido() { return lucroLiquido != null ? lucroLiquido : 0.0; }
    public Double getRoi() { return roi != null ? roi : 0.0; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setData(LocalDateTime data) { this.data = data; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    // ✅ NOVO: Setter para itens
    public void setItens(List<ItemVendaDTO> itens) { this.itens = itens; }

    public void setPrecoVenda(Double precoVenda) { this.precoVenda = precoVenda; }
    public void setFretePagoPeloCliente(Double fretePagoPeloCliente) { this.fretePagoPeloCliente = fretePagoPeloCliente; }
    public void setCustoEnvio(Double custoEnvio) { this.custoEnvio = custoEnvio; }
    public void setTarifaPlataforma(Double tarifaPlataforma) { this.tarifaPlataforma = tarifaPlataforma; }
    public void setCustoProdutoVendido(Double custoProdutoVendido) { this.custoProdutoVendido = custoProdutoVendido; }
    public void setDespesasOperacionais(Double despesasOperacionais) { this.despesasOperacionais = despesasOperacionais; }

    // Setters para campos de cálculo (opcional, normalmente calculados)
    public void setFaturamento(Double faturamento) { this.faturamento = faturamento; }
    public void setCustoEfetivoTotal(Double custoEfetivoTotal) { this.custoEfetivoTotal = custoEfetivoTotal; }
    public void setLucroBruto(Double lucroBruto) { this.lucroBruto = lucroBruto; }
    public void setLucroLiquido(Double lucroLiquido) { this.lucroLiquido = lucroLiquido; }
    public void setRoi(Double roi) { this.roi = roi; }

    // ✅ MÉTODOS DEPRECIADOS (COM @JsonIgnore PARA EVITAR SERIALIZAÇÃO)

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public Integer getQuantidade() {
        return getQuantidadeTotal();
    }

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public void setQuantidade(Integer quantidade) {
        // Não faz nada - quantidade agora é derivada dos itens
        System.out.println("⚠️ AVISO: setQuantidade() depreciado. Use setItens() em vez disso.");
    }

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public Long getProdutoId() {
        // Retorna o primeiro produto se houver itens
        return !itens.isEmpty() ? itens.get(0).getProdutoId() : null;
    }

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public void setProdutoId(Long produtoId) {
        System.out.println("⚠️ AVISO: setProdutoId() depreciado. Use setItens() em vez disso.");
    }

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public String getProdutoNome() {
        return !itens.isEmpty() ? itens.get(0).getProdutoNome() : "";
    }

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public void setProdutoNome(String produtoNome) {
        System.out.println("⚠️ AVISO: setProdutoNome() depreciado. Use setItens() em vez disso.");
    }

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public String getProdutoSku() {
        return !itens.isEmpty() ? itens.get(0).getProdutoSku() : "";
    }

    @Deprecated
    @JsonIgnore  // ✅ IMPEDE QUE APAREÇA NO JSON
    public void setProdutoSku(String produtoSku) {
        System.out.println("⚠️ AVISO: setProdutoSku() depreciado. Use setItens() em vez disso.");
    }
}