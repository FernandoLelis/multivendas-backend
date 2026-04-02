package com.fernando.erp_vendas.dto;

import com.fernando.erp_vendas.model.Compra;
import com.fernando.erp_vendas.model.ItemCompra;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CompraDTO {

    private Long id;
    private LocalDateTime data;
    private String fornecedor;
    private String observacoes;
    private String idPedidoCompra;
    private BigDecimal totalCompra;
    private List<ItemCompraDTO> itens;
    private Boolean sistemaAntigo = false;

    public static class ItemCompraDTO {
        private Long produtoId;
        private String produtoNome;
        private String produtoSku;       // ✅ ADICIONADO
        private Integer quantidade;
        private BigDecimal custoUnitario;
        private BigDecimal total;
        private String imagemUrl;

        public ItemCompraDTO() {}

        public ItemCompraDTO(ItemCompra item) {
            this.produtoId = item.getProduto().getId();
            this.produtoNome = item.getProduto().getNome();
            this.produtoSku = item.getProduto().getSku();   // ✅ ADICIONADO
            this.quantidade = item.getQuantidade();
            this.custoUnitario = item.getCustoUnitario();
            this.total = item.getCustoTotal();
            if (item.getProduto() != null) {
                this.imagemUrl = item.getProduto().getImagemUrl();
            }
        }

        // Getters e Setters
        public Long getProdutoId() { return produtoId; }
        public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

        public String getProdutoNome() { return produtoNome; }
        public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }

        public String getProdutoSku() { return produtoSku; }
        public void setProdutoSku(String produtoSku) { this.produtoSku = produtoSku; }

        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

        public BigDecimal getCustoUnitario() { return custoUnitario; }
        public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }

        public String getImagemUrl() { return imagemUrl; }
        public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
    }

    public CompraDTO() {}

    public CompraDTO(Compra compra) {
        this.id = compra.getId();
        this.data = compra.getData();
        this.fornecedor = compra.getFornecedor();
        this.observacoes = compra.getObservacoes();
        this.idPedidoCompra = compra.getIdPedidoCompra();
        this.totalCompra = compra.getTotalCompra();

        if (compra.getItens() != null) {
            this.itens = compra.getItens().stream()
                    .map(ItemCompraDTO::new)
                    .collect(Collectors.toList());
        }
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getFornecedor() { return fornecedor; }
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getIdPedidoCompra() { return idPedidoCompra; }
    public void setIdPedidoCompra(String idPedidoCompra) { this.idPedidoCompra = idPedidoCompra; }

    public BigDecimal getTotalCompra() { return totalCompra; }
    public void setTotalCompra(BigDecimal totalCompra) { this.totalCompra = totalCompra; }

    public List<ItemCompraDTO> getItens() { return itens; }
    public void setItens(List<ItemCompraDTO> itens) { this.itens = itens; }

    public Boolean getSistemaAntigo() { return sistemaAntigo; }
    public void setSistemaAntigo(Boolean sistemaAntigo) { this.sistemaAntigo = sistemaAntigo; }

    @JsonProperty("dataCompra")
    public LocalDateTime getDataCompra() { return data; }

    @JsonProperty("dataCompra")
    public void setDataCompra(LocalDateTime dataCompra) { this.data = dataCompra; }
}