package com.fernando.erp_vendas.dto;

import java.math.BigDecimal;

public class ItemVendaDTO {
    private Long id; // ID do ItemVenda (opcional para criação)
    private Long produtoId;
    private String produtoNome;
    private String produtoSku;
    private Integer quantidade;
    private BigDecimal custoUnitario; // Custo unitário do lote usado
    private Long loteId; // ID do lote específico (EntradaEstoque) - opcional na criação
    private BigDecimal custoTotal; // quantidade × custoUnitario (calculado)

    // ✅✅✅ CORREÇÃO v46.6: ADICIONAR CAMPO precoUnitario (PERMITE NULL PARA COMPATIBILIDADE)
    private BigDecimal precoUnitario; // Preço unitário de VENDA (o que o cliente pagou)

    // ✅✅✅ NOVO CAMPO: Imagem do Produto para aparecer na listagem de Vendas
    private String imagemUrl;

    // Construtor padrão - MANTIDO EXATAMENTE COMO ESTAVA
    public ItemVendaDTO() {
    }

    // Construtor para criação (sem id do ItemVenda, com lote opcional) - MANTIDO
    // (NÃO MODIFICAR PARA NÃO QUEBRAR CÓDIGO EXISTENTE)
    public ItemVendaDTO(Long produtoId, String produtoNome, String produtoSku,
                        Integer quantidade, BigDecimal custoUnitario, Long loteId) {
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.produtoSku = produtoSku;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.loteId = loteId;
        this.custoTotal = custoUnitario != null && quantidade != null
                ? custoUnitario.multiply(BigDecimal.valueOf(quantidade))
                : BigDecimal.ZERO;
    }

    // Construtor completo (com id do ItemVenda) - MANTIDO
    // (NÃO MODIFICAR PARA NÃO QUEBRAR CÓDIGO EXISTENTE)
    public ItemVendaDTO(Long id, Long produtoId, String produtoNome, String produtoSku,
                        Integer quantidade, BigDecimal custoUnitario, Long loteId) {
        this.id = id;
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.produtoSku = produtoSku;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.loteId = loteId;
        this.custoTotal = custoUnitario != null && quantidade != null
                ? custoUnitario.multiply(BigDecimal.valueOf(quantidade))
                : BigDecimal.ZERO;
    }

    // ✅✅✅ ADICIONAR: Construtor COMPATÍVEL com precoUnitario (NOVO - para uso futuro)
    public ItemVendaDTO(Long id, Long produtoId, String produtoNome, String produtoSku,
                        Integer quantidade, BigDecimal custoUnitario,
                        BigDecimal precoUnitario, Long loteId) {
        this.id = id;
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.produtoSku = produtoSku;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
        this.precoUnitario = precoUnitario; // ✅ NOVO PARÂMETRO
        this.loteId = loteId;
        this.custoTotal = custoUnitario != null && quantidade != null
                ? custoUnitario.multiply(BigDecimal.valueOf(quantidade))
                : BigDecimal.ZERO;
    }

    // Getters e Setters EXISTENTES - MANTIDOS INTACTOS
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public String getProdutoSku() {
        return produtoSku;
    }

    public void setProdutoSku(String produtoSku) {
        this.produtoSku = produtoSku;
    }

    public Integer getQuantidade() {
        return quantidade != null ? quantidade : 0;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        // Atualizar custoTotal quando quantidade mudar
        if (this.custoUnitario != null && quantidade != null) {
            this.custoTotal = this.custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario != null ? custoUnitario : BigDecimal.ZERO;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
        // Atualizar custoTotal quando custoUnitario mudar
        if (custoUnitario != null && this.quantidade != null) {
            this.custoTotal = custoUnitario.multiply(BigDecimal.valueOf(this.quantidade));
        }
    }

    public Long getLoteId() {
        return loteId;
    }

    public void setLoteId(Long loteId) {
        this.loteId = loteId;
    }

    public BigDecimal getCustoTotal() {
        // Calcular se não estiver calculado
        if (custoTotal == null && custoUnitario != null && quantidade != null) {
            custoTotal = custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
        return custoTotal != null ? custoTotal : BigDecimal.ZERO;
    }

    public void setCustoTotal(BigDecimal custoTotal) {
        this.custoTotal = custoTotal;
    }

    // ✅✅✅ ADICIONAR: GETTER E SETTER PARA precoUnitario (NOVO)
    public BigDecimal getPrecoUnitario() {
        return precoUnitario != null ? precoUnitario : BigDecimal.ZERO;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    // ✅✅✅ ADICIONAR: GETTER E SETTER PARA A IMAGEM
    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }


    // Método auxiliar para calcular/atualizar custoTotal - MANTIDO
    public void calcularCustoTotal() {
        if (custoUnitario != null && quantidade != null) {
            this.custoTotal = custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    @Override
    public String toString() {
        return "ItemVendaDTO{" +
                "id=" + id +
                ", produtoId=" + produtoId +
                ", produtoNome='" + produtoNome + '\'' +
                ", produtoSku='" + produtoSku + '\'' +
                ", quantidade=" + quantidade +
                ", custoUnitario=" + custoUnitario +
                ", precoUnitario=" + precoUnitario + // ✅ ADICIONADO
                ", loteId=" + loteId +
                ", custoTotal=" + custoTotal +
                '}';
    }
}