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

    // ✅ NOVOS CAMPOS
    private Double peso;
    private Double comprimento;
    private Double largura;
    private Double altura;
    private String imagemUrl;

    public ProdutoDTO() {}

    public ProdutoDTO(Long id, String nome, String sku, String asin, String descricao,
                      Number quantidadeEstoqueTotal, Integer estoqueMinimo, LocalDateTime dataCriacao,
                      Number quantidadeVendida, Double custoMedio, Double precoMedioVenda,
                      Double peso, Double comprimento, Double largura, Double altura, String imagemUrl) {
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

        // ✅ INICIALIZANDO NOVOS CAMPOS
        this.peso = peso;
        this.comprimento = comprimento;
        this.largura = largura;
        this.altura = altura;
        this.imagemUrl = imagemUrl;
    }

    // Getters e Setters Originais
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getAsin() { return asin; }
    public void setAsin(String asin) { this.asin = asin; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getQuantidadeEstoqueTotal() { return quantidadeEstoqueTotal; }
    public void setQuantidadeEstoqueTotal(Integer quantidadeEstoqueTotal) { this.quantidadeEstoqueTotal = quantidadeEstoqueTotal; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public String getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(String dataCriacao) { this.dataCriacao = dataCriacao; }
    public Long getQuantidadeVendida() { return quantidadeVendida; }
    public void setQuantidadeVendida(Long quantidadeVendida) { this.quantidadeVendida = quantidadeVendida; }
    public Double getCustoMedio() { return custoMedio; }
    public void setCustoMedio(Double custoMedio) { this.custoMedio = custoMedio; }
    public Double getPrecoMedioVenda() { return precoMedioVenda; }
    public void setPrecoMedioVenda(Double precoMedioVenda) { this.precoMedioVenda = precoMedioVenda; }
    public Double getLucro() { return lucro; }
    public void setLucro(Double lucro) { this.lucro = lucro; }

    // ✅ NOVOS GETTERS E SETTERS
    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }
    public Double getComprimento() { return comprimento; }
    public void setComprimento(Double comprimento) { this.comprimento = comprimento; }
    public Double getLargura() { return largura; }
    public void setLargura(Double largura) { this.largura = largura; }
    public Double getAltura() { return altura; }
    public void setAltura(Double altura) { this.altura = altura; }
    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
}