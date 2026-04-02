package com.fernando.erp_vendas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "venda")
public class Venda {

    public enum StatusVenda {
        ATIVA,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Column(name = "id_pedido", unique = true)
    private String idPedido;

    private String plataforma;

    // --- Preços e Custos ---
    @Column(name = "preco_venda")
    private Double precoVenda = 0.0;

    @Column(name = "frete_pago_pelo_cliente")
    private Double fretePagoPeloCliente = 0.0;

    @Column(name = "custo_envio")
    private Double custoEnvio = 0.0;

    @Column(name = "tarifa_plataforma")
    private Double tarifaPlataforma = 0.0;

    @Column(name = "custo_produto_vendido")
    private Double custoProdutoVendido = 0.0;

    @Column(name = "despesas_operacionais")
    private Double despesasOperacionais = 0.0;

    // --- Campos de Cancelamento ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusVenda status = StatusVenda.ATIVA;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "custo_retorno")
    private Double custoRetorno = 0.0;

    @Column(name = "retornou_estoque")
    private Boolean retornouEstoque = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private List<ItemVenda> itens = new ArrayList<>();

    // --- Construtores ---
    public Venda() {}

    public Venda(String idPedido, String plataforma,
                 Double precoVenda, Double fretePagoPeloCliente, Double custoEnvio,
                 Double tarifaPlataforma, Double custoProdutoVendido,
                 Double despesasOperacionais, User user) {
        this.idPedido = idPedido;
        this.plataforma = plataforma;
        this.precoVenda = precoVenda != null ? precoVenda : 0.0;
        this.fretePagoPeloCliente = fretePagoPeloCliente != null ? fretePagoPeloCliente : 0.0;
        this.custoEnvio = custoEnvio != null ? custoEnvio : 0.0;
        this.tarifaPlataforma = tarifaPlataforma != null ? tarifaPlataforma : 0.0;
        this.custoProdutoVendido = custoProdutoVendido != null ? custoProdutoVendido : 0.0;
        this.despesasOperacionais = despesasOperacionais != null ? despesasOperacionais : 0.0;
        this.user = user;
    }

    // --- Getters e Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    public Double getPrecoVenda() { return precoVenda != null ? precoVenda : 0.0; }
    public void setPrecoVenda(Double precoVenda) { this.precoVenda = precoVenda != null ? precoVenda : 0.0; }

    public Double getFretePagoPeloCliente() { return fretePagoPeloCliente != null ? fretePagoPeloCliente : 0.0; }
    public void setFretePagoPeloCliente(Double fretePagoPeloCliente) { this.fretePagoPeloCliente = fretePagoPeloCliente != null ? fretePagoPeloCliente : 0.0; }

    public Double getCustoEnvio() { return custoEnvio != null ? custoEnvio : 0.0; }
    public void setCustoEnvio(Double custoEnvio) { this.custoEnvio = custoEnvio != null ? custoEnvio : 0.0; }

    public Double getTarifaPlataforma() { return tarifaPlataforma != null ? tarifaPlataforma : 0.0; }
    public void setTarifaPlataforma(Double tarifaPlataforma) { this.tarifaPlataforma = tarifaPlataforma != null ? tarifaPlataforma : 0.0; }

    public Double getCustoProdutoVendido() { return custoProdutoVendido != null ? custoProdutoVendido : 0.0; }
    public void setCustoProdutoVendido(Double custoProdutoVendido) { this.custoProdutoVendido = custoProdutoVendido != null ? custoProdutoVendido : 0.0; }

    public Double getDespesasOperacionais() { return despesasOperacionais != null ? despesasOperacionais : 0.0; }
    public void setDespesasOperacionais(Double despesasOperacionais) { this.despesasOperacionais = despesasOperacionais != null ? despesasOperacionais : 0.0; }

    public StatusVenda getStatus() { return status; }
    public void setStatus(StatusVenda status) { this.status = status; }

    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }

    public Double getCustoRetorno() { return custoRetorno != null ? custoRetorno : 0.0; }
    public void setCustoRetorno(Double custoRetorno) { this.custoRetorno = custoRetorno != null ? custoRetorno : 0.0; }

    public Boolean getRetornouEstoque() { return retornouEstoque != null ? retornouEstoque : false; }
    public void setRetornouEstoque(Boolean retornouEstoque) { this.retornouEstoque = retornouEstoque; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<ItemVenda> getItens() { return itens; }

    public void setItens(List<ItemVenda> novosItens) {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        this.itens.clear();
        if (novosItens != null) {
            novosItens.forEach(item -> item.setVenda(this));
            this.itens.addAll(novosItens);
        }
    }

    public void adicionarItem(ItemVenda item) {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        item.setVenda(this);
        this.itens.add(item);
    }

    // --- Métodos Auxiliares e de Cálculo ---
    public Integer getQuantidadeTotal() {
        if (itens == null || itens.isEmpty()) return 0;
        return itens.stream().filter(i -> i.getQuantidade() != null).mapToInt(ItemVenda::getQuantidade).sum();
    }

    public Double getCustoProdutosTotal() {
        if (itens == null || itens.isEmpty()) return 0.0;
        return itens.stream().filter(i -> i.getCustoTotal() != null).mapToDouble(item -> item.getCustoTotal().doubleValue()).sum();
    }

    // Faturamento bruto para exibição (mesmo se cancelada)
    public Double calcularFaturamento() {
        return getPrecoVenda() + getFretePagoPeloCliente();
    }

    // Faturamento efetivo para cálculo de lucro (zero se cancelada)
    public Double calcularFaturamentoEfetivo() {
        if (status == StatusVenda.CANCELADA) {
            return 0.0;
        }
        return calcularFaturamento();
    }

    public Double calcularCustoEfetivoTotal() {
        if (status == StatusVenda.CANCELADA) {
            if (Boolean.TRUE.equals(retornouEstoque)) {
                // Produtos voltaram ao estoque: custo = envio + retorno
                return getCustoEnvio() + getCustoRetorno();
            } else {
                // Produtos não voltaram: custo = produto + envio + retorno
                return getCustoProdutoVendido() + getCustoEnvio() + getCustoRetorno();
            }
        }
        // Venda ativa: custo = produto + envio + tarifa
        return getCustoProdutoVendido() + getCustoEnvio() + getTarifaPlataforma();
    }

    public Double calcularLucroBruto() {
        return calcularFaturamentoEfetivo() - calcularCustoEfetivoTotal();
    }

    public Double calcularLucroLiquido() {
        return calcularLucroBruto() - getDespesasOperacionais();
    }

    public Double calcularROI() {
        Double custoEfetivo = calcularCustoEfetivoTotal();
        Double lucroLiquido = calcularLucroLiquido();
        return (custoEfetivo > 0) ? (lucroLiquido / custoEfetivo) * 100 : 0.0;
    }

    public boolean temProdutos() {
        return itens != null && !itens.isEmpty();
    }

    public List<Produto> getProdutosDistintos() {
        if (itens == null || itens.isEmpty()) return new ArrayList<>();
        return itens.stream().map(ItemVenda::getProduto).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }
}