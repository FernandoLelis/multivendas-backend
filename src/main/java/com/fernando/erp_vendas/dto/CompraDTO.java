package com.fernando.erp_vendas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompraDTO {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime data;

    private String idPedidoCompra;
    private String fornecedor;
    private String observacoes;

    // ✅ Lista de itens da compra
    private List<ItemCompraDTO> itens = new ArrayList<>();

    // Cálculos automáticos
    private BigDecimal totalCompra;
    private Long userId;

    // Construtor padrão (para criação)
    public CompraDTO() {
    }

    // Construtor a partir da entidade Compra (para consulta)
    public CompraDTO(com.fernando.erp_vendas.model.Compra compra) {
        this.id = compra.getId();
        this.data = compra.getData() != null ? compra.getData() : LocalDateTime.now();
        this.idPedidoCompra = compra.getIdPedidoCompra();
        this.fornecedor = compra.getFornecedor();
        this.observacoes = compra.getObservacoes();
        this.totalCompra = compra.getTotalCompra();
        this.userId = compra.getUser() != null ? compra.getUser().getId() : null;

        // Converter itens da compra para ItemCompraDTO
        if (compra.getItens() != null && !compra.getItens().isEmpty()) {
            this.itens = compra.getItens().stream()
                    .map(item -> new ItemCompraDTO(
                            item.getId(),
                            item.getProduto() != null ? item.getProduto().getId() : null,
                            item.getProduto() != null ? item.getProduto().getNome() : "Produto não encontrado",
                            item.getProduto() != null ? item.getProduto().getSku() : "",
                            item.getQuantidade(),
                            item.getCustoUnitario(),
                            item.getCustoTotal(),
                            item.getLote() != null ? item.getLote().getId() : null,
                            item.getUser() != null ? item.getUser().getId() : null
                    ))
                    .collect(Collectors.toList());
        }
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getIdPedidoCompra() { return idPedidoCompra; }
    public void setIdPedidoCompra(String idPedidoCompra) { this.idPedidoCompra = idPedidoCompra; }

    public String getFornecedor() { return fornecedor; }
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public List<ItemCompraDTO> getItens() { return itens; }
    public void setItens(List<ItemCompraDTO> itens) { this.itens = itens; }

    public BigDecimal getTotalCompra() {
        return totalCompra != null ? totalCompra : calcularTotalCompra();
    }

    public void setTotalCompra(BigDecimal totalCompra) { this.totalCompra = totalCompra; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    // Métodos auxiliares
    public BigDecimal calcularTotalCompra() {
        if (itens == null || itens.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return itens.stream()
                .map(ItemCompraDTO::getCustoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Integer getQuantidadeTotal() {
        if (itens == null || itens.isEmpty()) {
            return 0;
        }
        return itens.stream()
                .mapToInt(ItemCompraDTO::getQuantidade)
                .sum();
    }

    public void adicionarItem(ItemCompraDTO item) {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        this.itens.add(item);
    }

    @Override
    public String toString() {
        return "CompraDTO{" +
                "id=" + id +
                ", data=" + data +
                ", idPedidoCompra='" + idPedidoCompra + '\'' +
                ", fornecedor='" + fornecedor + '\'' +
                ", totalCompra=" + totalCompra +
                ", userId=" + userId +
                ", quantidadeItens=" + (itens != null ? itens.size() : 0) +
                '}';
    }
}