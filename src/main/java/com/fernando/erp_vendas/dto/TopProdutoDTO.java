package com.fernando.erp_vendas.dto;

public class TopProdutoDTO {
    private Long produtoId;
    private String produtoNome;
    private String imagemUrl;
    private Long quantidadeVendida;
    private Double precoMedioVenda;
    private Double custoMedio;
    private Double lucroPorUnidade;

    // Getters e Setters obrigatórios
    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }

    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }

    public Long getQuantidadeVendida() { return quantidadeVendida; }
    public void setQuantidadeVendida(Long quantidadeVendida) { this.quantidadeVendida = quantidadeVendida; }

    public Double getPrecoMedioVenda() { return precoMedioVenda; }
    public void setPrecoMedioVenda(Double precoMedioVenda) { this.precoMedioVenda = precoMedioVenda; }

    public Double getCustoMedio() { return custoMedio; }
    public void setCustoMedio(Double custoMedio) { this.custoMedio = custoMedio; }

    public Double getLucroPorUnidade() { return lucroPorUnidade; }
    public void setLucroPorUnidade(Double lucroPorUnidade) { this.lucroPorUnidade = lucroPorUnidade; }
}