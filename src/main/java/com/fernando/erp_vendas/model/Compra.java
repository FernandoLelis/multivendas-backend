package com.fernando.erp_vendas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime data; // ✅ REMOVIDO: valor padrão LocalDateTime.now()

    @Column(name = "id_pedido_compra")
    private String idPedidoCompra;

    private String fornecedor;

    @Column(length = 1000)
    private String observacoes;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCompra> itens = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ✅ CONSTRUTOR PADRÃO - NÃO inicializa data automaticamente
    public Compra() {
    }

    // ✅ NOVO CONSTRUTOR com data como parâmetro
    public Compra(LocalDateTime data, String idPedidoCompra, String fornecedor,
                  String observacoes, User user) {
        this.data = data;
        this.idPedidoCompra = idPedidoCompra;
        this.fornecedor = fornecedor;
        this.observacoes = observacoes;
        this.user = user;
    }

    // ✅ CONSTRUTOR mantido para compatibilidade (usa data atual)
    public Compra(String idPedidoCompra, String fornecedor, String observacoes, User user) {
        this(LocalDateTime.now(), idPedidoCompra, fornecedor, observacoes, user);
    }

    // Getters e Setters
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

    public String getIdPedidoCompra() {
        return idPedidoCompra;
    }

    public void setIdPedidoCompra(String idPedidoCompra) {
        this.idPedidoCompra = idPedidoCompra;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<ItemCompra> getItens() {
        return itens;
    }

    public void setItens(List<ItemCompra> itens) {
        this.itens = itens;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Método para adicionar item
    public void adicionarItem(ItemCompra item) {
        item.setCompra(this);
        this.itens.add(item);
    }

    // Método para calcular total da compra
    public BigDecimal getTotalCompra() {
        return itens.stream()
                .map(ItemCompra::getCustoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "Compra{" +
                "id=" + id +
                ", data=" + data +
                ", idPedidoCompra='" + idPedidoCompra + '\'' +
                ", fornecedor='" + fornecedor + '\'' +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }
}