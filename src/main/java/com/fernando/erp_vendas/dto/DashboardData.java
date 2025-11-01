package com.fernando.erp_vendas.dto;

import java.util.List;

public class DashboardData {
    private double faturamentoTotal;
    private double custoEfetivoTotal;
    private double lucroBrutoTotal;
    private double lucroLiquidoTotal;
    private double despesasOperacionaisTotal;
    private double roiTotal;
    private int totalVendas;

    // 🆕 NOVOS CAMPOS PARA DASHBOARD DETALHADO
    private double despesasVendas;
    private double despesasGerais;
    private int vendasMesAtual;
    private List<Object[]> faturamentoPorPlataforma;
    private List<Object[]> produtosMaisVendidos;
    private List<Object[]> topCategoriasDespesas;

    // Getters e Setters ORIGINAIS
    public double getFaturamentoTotal() { return faturamentoTotal; }
    public void setFaturamentoTotal(double faturamentoTotal) { this.faturamentoTotal = faturamentoTotal; }

    public double getCustoEfetivoTotal() { return custoEfetivoTotal; }
    public void setCustoEfetivoTotal(double custoEfetivoTotal) { this.custoEfetivoTotal = custoEfetivoTotal; }

    public double getLucroBrutoTotal() { return lucroBrutoTotal; }
    public void setLucroBrutoTotal(double lucroBrutoTotal) { this.lucroBrutoTotal = lucroBrutoTotal; }

    public double getLucroLiquidoTotal() { return lucroLiquidoTotal; }
    public void setLucroLiquidoTotal(double lucroLiquidoTotal) { this.lucroLiquidoTotal = lucroLiquidoTotal; }

    public double getDespesasOperacionaisTotal() { return despesasOperacionaisTotal; }
    public void setDespesasOperacionaisTotal(double despesasOperacionaisTotal) { this.despesasOperacionaisTotal = despesasOperacionaisTotal; }

    public double getRoiTotal() { return roiTotal; }
    public void setRoiTotal(double roiTotal) { this.roiTotal = roiTotal; }

    public int getTotalVendas() { return totalVendas; }
    public void setTotalVendas(int totalVendas) { this.totalVendas = totalVendas; }

    // 🆕 NOVOS GETTERS E SETTERS
    public double getDespesasVendas() { return despesasVendas; }
    public void setDespesasVendas(double despesasVendas) { this.despesasVendas = despesasVendas; }

    public double getDespesasGerais() { return despesasGerais; }
    public void setDespesasGerais(double despesasGerais) { this.despesasGerais = despesasGerais; }

    public int getVendasMesAtual() { return vendasMesAtual; }
    public void setVendasMesAtual(int vendasMesAtual) { this.vendasMesAtual = vendasMesAtual; }

    public List<Object[]> getFaturamentoPorPlataforma() { return faturamentoPorPlataforma; }
    public void setFaturamentoPorPlataforma(List<Object[]> faturamentoPorPlataforma) { this.faturamentoPorPlataforma = faturamentoPorPlataforma; }

    public List<Object[]> getProdutosMaisVendidos() { return produtosMaisVendidos; }
    public void setProdutosMaisVendidos(List<Object[]> produtosMaisVendidos) { this.produtosMaisVendidos = produtosMaisVendidos; }

    public List<Object[]> getTopCategoriasDespesas() { return topCategoriasDespesas; }
    public void setTopCategoriasDespesas(List<Object[]> topCategoriasDespesas) { this.topCategoriasDespesas = topCategoriasDespesas; }
}