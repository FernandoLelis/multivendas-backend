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

    // ⚠️ REMOVIDO: quantidade (agora está em ItemVenda)
    // private Integer quantidade;

    // ⚠️ REMOVIDO: produto (agora está em ItemVenda)
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "produto_id")
    // private Produto produto;

    // Preços e custos
    @Column(name = "preco_venda")
    private Double precoVenda = 0.0; // ⚠️ AGORA: PREÇO TOTAL da venda (soma de todos os produtos)

    @Column(name = "frete_pago_pelo_cliente")
    private Double fretePagoPeloCliente = 0.0;

    @Column(name = "custo_envio")
    private Double custoEnvio = 0.0;

    @Column(name = "tarifa_plataforma")
    private Double tarifaPlataforma = 0.0;

    @Column(name = "custo_produto_vendido")
    private Double custoProdutoVendido = 0.0; // ✅ SOMA dos custos de TODOS os produtos (PEPS)

    @Column(name = "despesas_operacionais")
    private Double despesasOperacionais = 0.0;

    // ✅ Multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    // ✅ Rastreamento PEPS - AGORA: Lista de produtos da venda
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ItemVenda> itens = new ArrayList<>();

    // Construtor padrão
    public Venda() {}

    // ⚠️ CONSTRUTOR ATUALIZADO: Removidos produto e quantidade
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

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    // ⚠️ REMOVIDO: getQuantidade() e setQuantidade()
    // public Integer getQuantidade() { return quantidade; }
    // public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    // ⚠️ REMOVIDO: getProduto() e setProduto()
    // public Produto getProduto() { return produto; }
    // public void setProduto(Produto produto) { this.produto = produto; }

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

    // ✅ MANTIDO: Lista de itens
    public List<ItemVenda> getItens() { return itens; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; }

    // ✅ NOVO: Método para adicionar item à venda
    public void adicionarItem(ItemVenda item) {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        item.setVenda(this);
        this.itens.add(item);
    }

    // ✅ NOVO: Método para calcular quantidade total (soma de todos os itens)
    public Integer getQuantidadeTotal() {
        if (itens == null || itens.isEmpty()) {
            return 0;
        }
        return itens.stream()
                .mapToInt(ItemVenda::getQuantidade)
                .sum();
    }

    // ✅ NOVO: Método para calcular custo total dos produtos (soma de todos os itens)
    public Double getCustoProdutosTotal() {
        if (itens == null || itens.isEmpty()) {
            return 0.0;
        }
        return itens.stream()
                .mapToDouble(item -> item.getCustoTotal().doubleValue())
                .sum();
    }

    // ✅ CORREÇÃO: FÓRMULAS ATUALIZADAS - precoVenda já é TOTAL (soma de todos os produtos)

    // 💰 FATURAMENTO = Preço Total da Venda + Frete
    // ⚠️ NÃO multiplicar por quantidade - precoVenda já é TOTAL
    public Double calcularFaturamento() {
        double precoTotal = getPrecoVenda(); // ✅ Já é total de todos os produtos
        double frete = getFretePagoPeloCliente();
        return precoTotal + frete;
    }

    // 💸 CUSTO EFETIVO = Custo PEPS (TOTAL) + Custo Envio + Tarifa
    // custoProdutoVendido JÁ É TOTAL (soma de todos os produtos)
    public Double calcularCustoEfetivoTotal() {
        double custoProduto = getCustoProdutoVendido(); // Já é total
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

    // ✅ NOVO: Método para verificar se venda tem produtos
    public boolean temProdutos() {
        return itens != null && !itens.isEmpty();
    }

    // ✅ NOVO: Método para obter lista de produtos distintos
    public List<Produto> getProdutosDistintos() {
        if (itens == null || itens.isEmpty()) {
            return new ArrayList<>();
        }
        return itens.stream()
                .map(ItemVenda::getLote)
                .filter(lote -> lote != null && lote.getProduto() != null)
                .map(EntradaEstoque::getProduto)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }
}