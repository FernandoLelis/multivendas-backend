package com.fernando.erp_vendas.dto;

import java.time.LocalDateTime;

public class ProdutoDTO {
    private Long id;
    private String nome;
    private String sku;
    private String asin;
    private String descricao;
    private Integer quantidadeEstoqueTotal;
    private Integer estoqueMinimo;
    private String dataCriacao;
    private Long quantidadeVendida;
    private Double custoMedio;
    private Double precoMedioVenda;
    private Double lucro;

    // Construtor atualizado usando Number para evitar erros de Cast do JPA
    public ProdutoDTO(Long id, String nome, String sku, String asin, String descricao,
                      Number quantidadeEstoqueTotal, Integer estoqueMinimo, LocalDateTime dataCriacao,
                      Number quantidadeVendida, Double custoMedio, Double precoMedioVenda) {
        this.id = id;
        this.nome = nome;
        this.sku = sku;
        this.asin = asin;
        this.descricao = descricao;
        this.quantidadeEstoqueTotal = quantidadeEstoqueTotal != null ? quantidadeEstoqueTotal.intValue() : 0;
        this.estoqueMinimo = estoqueMinimo != null ? estoqueMinimo : 0;
        this.dataCriacao = dataCriacao != null ? dataCriacao.toString() : null;

        this.quantidadeVendida = quantidadeVendida != null ? quantidadeVendida.longValue() : 0L;
        this.custoMedio = custoMedio != null ? custoMedio : 0.0;
        this.precoMedioVenda = precoMedioVenda != null ? precoMedioVenda : 0.0;
        this.lucro = this.precoMedioVenda - this.custoMedio;
    }

    // Getters e Setters (Mantenha todos os getters e setters que eu enviei na mensagem anterior)
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getSku() { return sku; }
    public String getAsin() { return asin; }
    public String getDescricao() { return descricao; }
    public Integer getQuantidadeEstoqueTotal() { return quantidadeEstoqueTotal; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public String getDataCriacao() { return dataCriacao; }
    public Long getQuantidadeVendida() { return quantidadeVendida; }
    public Double getCustoMedio() { return custoMedio; }
    public Double getPrecoMedioVenda() { return precoMedioVenda; }
    public Double getLucro() { return lucro; }

    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setSku(String sku) { this.sku = sku; }
    public void setAsin(String asin) { this.asin = asin; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setQuantidadeEstoqueTotal(Integer quantidadeEstoqueTotal) { this.quantidadeEstoqueTotal = quantidadeEstoqueTotal; }
    public void setEstoqueMinimo(Integer estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public void setDataCriacao(String dataCriacao) { this.dataCriacao = dataCriacao; }
    public void setQuantidadeVendida(Long quantidadeVendida) { this.quantidadeVendida = quantidadeVendida; }
    public void setCustoMedio(Double custoMedio) { this.custoMedio = custoMedio; }
    public void setPrecoMedioVenda(Double precoMedioVenda) { this.precoMedioVenda = precoMedioVenda; }
    public void setLucro(Double lucro) { this.lucro = lucro; }
}