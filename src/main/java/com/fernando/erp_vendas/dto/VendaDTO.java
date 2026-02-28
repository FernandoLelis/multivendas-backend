package com.fernando.erp_vendas.dto;

import com.fernando.erp_vendas.model.ItemVenda;
import com.fernando.erp_vendas.model.Venda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VendaDTO {
    private Long id;
    private LocalDateTime data;
    private String idPedido;
    private String plataforma;
    private Double precoVenda;
    private Double fretePagoPeloCliente;
    private Double custoEnvio;
    private Double tarifaPlataforma;
    private Double despesasOperacionais;
    private Double custoProdutoVendido;

    // ✅ CAMPOS CALCULADOS
    private Double faturamento;
    private Double custoEfetivoTotal;
    private Double lucroBruto;
    private Double lucroLiquido;
    private Double roi;

    private List<ItemVendaDTO> itens;

    // ✅ CONSTRUTOR PADRÃO
    public VendaDTO() {
        this.itens = new ArrayList<>();
    }

    // ✅ CONSTRUTOR A PARTIR DA ENTIDADE VENDA
    public VendaDTO(Venda venda) {
        this.id = venda.getId();
        this.data = venda.getData();
        this.idPedido = venda.getIdPedido();
        this.plataforma = venda.getPlataforma();
        this.precoVenda = venda.getPrecoVenda();
        this.fretePagoPeloCliente = venda.getFretePagoPeloCliente();
        this.custoEnvio = venda.getCustoEnvio();
        this.tarifaPlataforma = venda.getTarifaPlataforma();
        this.despesasOperacionais = venda.getDespesasOperacionais();
        this.custoProdutoVendido = venda.getCustoProdutoVendido();

        // ✅✅✅ CORREÇÃO CRÍTICA v46.8.2: FILTRAR APENAS ITENS COM PRODUTO
        if (venda.getItens() != null) {
            this.itens = venda.getItens().stream()
                    .filter(item -> item.getProduto() != null) // ✅ FILTRAR ITENS COM PRODUTO
                    .map(item -> {
                        ItemVendaDTO dto = new ItemVendaDTO();
                        dto.setId(item.getId());

                        // ✅✅✅ GARANTIR QUE PRODUTO EXISTE
                        if (item.getProduto() != null) {
                            dto.setProdutoId(item.getProduto().getId());
                            dto.setProdutoNome(item.getProduto().getNome());
                            dto.setProdutoSku(item.getProduto().getSku());
                            // ✅ O SEGREDO REVELADO: Puxando a imagem do banco para o Angular!
                            dto.setImagemUrl(item.getProduto().getImagemUrl());
                        } else {
                            // Fallback seguro
                            dto.setProdutoId(0L);
                            dto.setProdutoNome("Produto não encontrado");
                            dto.setProdutoSku("N/A");
                            dto.setImagemUrl(null); // ✅ Garantir que fique vazio se não houver produto
                        }

                        dto.setQuantidade(item.getQuantidade());
                        dto.setCustoUnitario(item.getCustoUnitario());
                        dto.setPrecoUnitario(item.getPrecoUnitario());

                        if (item.getLote() != null) {
                            dto.setLoteId(item.getLote().getId());
                        }

                        dto.setCustoTotal(item.getCustoTotal());
                        return dto;
                    })
                    .collect(Collectors.toList());
        } else {
            this.itens = new ArrayList<>();
        }

        // ✅ CÁLCULOS
        this.faturamento = venda.calcularFaturamento();
        this.custoEfetivoTotal = venda.calcularCustoEfetivoTotal();
        this.lucroBruto = venda.calcularLucroBruto();
        this.lucroLiquido = venda.calcularLucroLiquido();
        this.roi = venda.calcularROI();
    }

    // ========== GETTERS E SETTERS ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public String getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(String idPedido) {
        this.idPedido = idPedido;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(String plataforma) {
        this.plataforma = plataforma;
    }

    public Double getPrecoVenda() {
        return precoVenda != null ? precoVenda : 0.0;
    }

    public void setPrecoVenda(Double precoVenda) {
        this.precoVenda = precoVenda;
    }

    public Double getFretePagoPeloCliente() {
        return fretePagoPeloCliente != null ? fretePagoPeloCliente : 0.0;
    }

    public void setFretePagoPeloCliente(Double fretePagoPeloCliente) {
        this.fretePagoPeloCliente = fretePagoPeloCliente;
    }

    public Double getCustoEnvio() {
        return custoEnvio != null ? custoEnvio : 0.0;
    }

    public void setCustoEnvio(Double custoEnvio) {
        this.custoEnvio = custoEnvio;
    }

    public Double getTarifaPlataforma() {
        return tarifaPlataforma != null ? tarifaPlataforma : 0.0;
    }

    public void setTarifaPlataforma(Double tarifaPlataforma) {
        this.tarifaPlataforma = tarifaPlataforma;
    }

    public Double getDespesasOperacionais() {
        return despesasOperacionais != null ? despesasOperacionais : 0.0;
    }

    public void setDespesasOperacionais(Double despesasOperacionais) {
        this.despesasOperacionais = despesasOperacionais;
    }

    public Double getCustoProdutoVendido() {
        return custoProdutoVendido != null ? custoProdutoVendido : 0.0;
    }

    public void setCustoProdutoVendido(Double custoProdutoVendido) {
        this.custoProdutoVendido = custoProdutoVendido;
    }

    public List<ItemVendaDTO> getItens() {
        return itens != null ? itens : new ArrayList<>();
    }

    public void setItens(List<ItemVendaDTO> itens) {
        this.itens = itens;
    }

    public Double getFaturamento() {
        return faturamento != null ? faturamento : 0.0;
    }

    public void setFaturamento(Double faturamento) {
        this.faturamento = faturamento;
    }

    public Double getCustoEfetivoTotal() {
        return custoEfetivoTotal != null ? custoEfetivoTotal : 0.0;
    }

    public void setCustoEfetivoTotal(Double custoEfetivoTotal) {
        this.custoEfetivoTotal = custoEfetivoTotal;
    }

    public Double getLucroBruto() {
        return lucroBruto != null ? lucroBruto : 0.0;
    }

    public void setLucroBruto(Double lucroBruto) {
        this.lucroBruto = lucroBruto;
    }

    public Double getLucroLiquido() {
        return lucroLiquido != null ? lucroLiquido : 0.0;
    }

    public void setLucroLiquido(Double lucroLiquido) {
        this.lucroLiquido = lucroLiquido;
    }

    public Double getRoi() {
        return roi != null ? roi : 0.0;
    }

    public void setRoi(Double roi) {
        this.roi = roi;
    }

    // ✅ MÉTODO PARA VERIFICAR SE TEM ITENS VÁLIDOS
    public boolean temItensValidos() {
        return getItens() != null && !getItens().isEmpty();
    }

    // ✅ MÉTODO PARA OBTER QUANTIDADE TOTAL DE PRODUTOS
    public Integer getQuantidadeTotalProdutos() {
        if (getItens() == null) return 0;
        return getItens().stream()
                .mapToInt(ItemVendaDTO::getQuantidade)
                .sum();
    }

    // ✅ MÉTODO PARA OBTER CUSTO TOTAL DOS PRODUTOS
    public BigDecimal getCustoTotalProdutos() {
        if (getItens() == null) return BigDecimal.ZERO;
        return getItens().stream()
                .map(ItemVendaDTO::getCustoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ✅ MÉTODO PARA OBTER FATURAMENTO TOTAL
    public BigDecimal getFaturamentoTotal() {
        if (getItens() == null) return BigDecimal.ZERO;
        return getItens().stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "VendaDTO{" +
                "id=" + id +
                ", idPedido='" + idPedido + '\'' +
                ", plataforma='" + plataforma + '\'' +
                ", precoVenda=" + precoVenda +
                ", itens=" + (itens != null ? itens.size() : 0) +
                ", faturamento=" + faturamento +
                ", lucroLiquido=" + lucroLiquido +
                '}';
    }
}