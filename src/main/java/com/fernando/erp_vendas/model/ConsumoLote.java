package com.fernando.erp_vendas.model;

import java.math.BigDecimal;

public class ConsumoLote {
    private Long loteId;
    private EntradaEstoque lote;
    private Integer quantidadeConsumida;
    private BigDecimal custoUnitario;

    // Construtores
    public ConsumoLote() {
    }

    public ConsumoLote(Long loteId, Integer quantidadeConsumida, BigDecimal custoUnitario) {
        this.loteId = loteId;
        this.quantidadeConsumida = quantidadeConsumida;
        this.custoUnitario = custoUnitario;
    }

    public ConsumoLote(EntradaEstoque lote, Integer quantidadeConsumida) {
        this.lote = lote;
        this.loteId = lote.getId();
        this.quantidadeConsumida = quantidadeConsumida;
        this.custoUnitario = lote.getCustoUnitario();
    }

    // Getters e Setters
    public Long getLoteId() {
        return loteId;
    }

    public void setLoteId(Long loteId) {
        this.loteId = loteId;
    }

    public EntradaEstoque getLote() {
        return lote;
    }

    public void setLote(EntradaEstoque lote) {
        this.lote = lote;
        this.loteId = lote != null ? lote.getId() : null;
        this.custoUnitario = lote != null ? lote.getCustoUnitario() : null;
    }

    public Integer getQuantidadeConsumida() {
        return quantidadeConsumida;
    }

    public void setQuantidadeConsumida(Integer quantidadeConsumida) {
        this.quantidadeConsumida = quantidadeConsumida;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
    }

    public BigDecimal getCustoTotal() {
        if (custoUnitario != null && quantidadeConsumida != null) {
            return custoUnitario.multiply(BigDecimal.valueOf(quantidadeConsumida));
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "ConsumoLote{" +
                "loteId=" + loteId +
                ", quantidadeConsumida=" + quantidadeConsumida +
                ", custoUnitario=" + custoUnitario +
                '}';
    }
}