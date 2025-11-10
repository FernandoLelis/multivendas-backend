package com.fernando.erp_vendas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venda")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false)
    private LocalDateTime data = LocalDateTime.now();

    @Column(name = "id_pedido", unique = true)
    private String idPedido;

    private String plataforma;

    private Integer quantidade;

    // ✅ CORREÇÃO: Relação com Produto mantida
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    // Preços e custos
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

    // ✅ Multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    // ✅ Rastreamento PEPS
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ItemVenda> itens = new ArrayList<>();

    // Construtores
    public Venda() {}

    public Venda(String idPedido, String plataforma, Integer quantidade, Produto produto,
                 Double precoVenda, Double fretePagoPeloCliente, Double custoEnvio,
                 Double tarifaPlataforma, Double custoProdutoVendido, Double despesasOperacionais, User user) {
        this.idPedido = idPedido;
        this.plataforma = plataforma;
        this.quantidade = quantidade;
        this.produto = produto;
        this.precoVenda = precoVenda != null ? precoVenda : 0.0;
        this.fretePagoPeloCliente = fretePagoPeloCliente != null ? fretePagoPeloCliente : 0.0;
        this.custoEnvio = custoEnvio != null ? custoEnvio : 0.0;
        this.tarifaPlataforma = tarifaPlataforma != null ? tarifaPlataforma : 0.0;
        this.custoProdutoVendido = custoProdutoVendido != null ? custoProdutoVendido : 0.0;
        this.despesasOperacionais = despesasOperacionais != null ? despesasOperacionais : 0.0;
        this.user = user;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<ItemVenda> getItens() { return itens; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; }

    // ✅ CORREÇÃO CRÍTICA: NOVAS FÓRMULAS COM TRATAMENTO DE NULL

    // 💰 FATURAMENTO = Preço Venda + Frete
    public Double calcularFaturamento() {
        double preco = getPrecoVenda();
        double frete = getFretePagoPeloCliente();
        return preco + frete;
    }

    // 💸 CUSTO EFETIVO = Custo PEPS + Custo Envio + Tarifa
    public Double calcularCustoEfetivoTotal() {
        double custoProduto = getCustoProdutoVendido();
        double custoEnvioVal = getCustoEnvio();
        double tarifa = getTarifaPlataforma();
        return custoProduto + custoEnvioVal + tarifa;
    }

    // 📊 LUCRO BRUTO = FATURAMENTO - CUSTO EFETIVO
    public Double calcularLucroBruto() {
        return calcularFaturamento() - calcularCustoEfetivoTotal();
    }

    // 💵 LUCRO LÍQUIDO = LUCRO BRUTO - DESPESAS OPERACIONAIS
    public Double calcularLucroLiquido() {
        double despesas = getDespesasOperacionais();
        return calcularLucroBruto() - despesas;
    }

    // 🎯 ROI = (LUCRO LÍQUIDO / CUSTO EFETIVO) × 100
    public Double calcularROI() {
        Double custoEfetivo = calcularCustoEfetivoTotal();
        Double lucroLiquido = calcularLucroLiquido();
        return (custoEfetivo > 0) ? (lucroLiquido / custoEfetivo) * 100 : 0.0;
    }
}