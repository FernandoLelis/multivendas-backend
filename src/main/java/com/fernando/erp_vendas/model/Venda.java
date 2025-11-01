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

    @Column(name = "id_pedido", nullable = false)
    private String idPedido; // 🆕 REMOVIDO unique=true (será único por usuário)

    @Column(name = "plataforma", nullable = false)
    private String plataforma;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Column(name = "preco_venda", nullable = false)
    private double precoVenda;

    @Column(name = "custo_produto_vendido")
    private double custoProdutoVendido;

    @Column(name = "frete_pago_pelo_cliente", nullable = false)
    private double fretePagoPeloCliente;

    @Column(name = "custo_envio", nullable = false)
    private double custoEnvio;

    @Column(name = "tarifa_plataforma", nullable = false)
    private double tarifaPlataforma;

    // ✅ NOVO CAMPO: Despesas operacionais (embalagens, material escritório, etc.)
    @Column(name = "despesas_operacionais", nullable = false)
    private double despesasOperacionais = 0.0;

    // 🆕 RELAÇÃO COM USUÁRIO - MULTI-TENANCY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private Produto produto;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private List<ItemVenda> itens = new ArrayList<>();

    // Construtor padrão
    public Venda() {
        this.custoProdutoVendido = 0.0;
        this.despesasOperacionais = 0.0;
    }

    // 🆕 CONSTRUTOR COM USER
    public Venda(String idPedido, String plataforma, int quantidade, double precoVenda,
                 User user, Produto produto) {
        this.idPedido = idPedido;
        this.plataforma = plataforma;
        this.quantidade = quantidade;
        this.precoVenda = precoVenda;
        this.user = user;
        this.produto = produto;
        this.custoProdutoVendido = 0.0;
        this.despesasOperacionais = 0.0;
    }

    // ✅ MÉTODOS DE CÁLCULO CORRIGIDOS

    /**
     * 💰 FATURAMENTO = (Preço de Venda ao Público + Frete pago pelo cliente) - Tarifa da plataforma
     */
    public double calcularFaturamento() {
        return (precoVenda + fretePagoPeloCliente) - tarifaPlataforma;
    }

    /**
     * 💸 CUSTO EFETIVO TOTAL = Custo dos produtos Vendidos + Custo Envio
     */
    public double calcularCustoEfetivoTotal() {
        return custoProdutoVendido + custoEnvio;
    }

    /**
     * 📊 LUCRO BRUTO = FATURAMENTO - CUSTO EFETIVO TOTAL
     */
    public double calcularLucroBruto() {
        return calcularFaturamento() - calcularCustoEfetivoTotal();
    }

    /**
     * 💵 LUCRO LÍQUIDO = LUCRO BRUTO - Despesas operacionais
     */
    public double calcularLucroLiquido() {
        return calcularLucroBruto() - despesasOperacionais;
    }

    /**
     * 📈 ROI (%) = (LUCRO LÍQUIDO / CUSTO EFETIVO TOTAL) × 100
     */
    public double calcularROI() {
        double cet = calcularCustoEfetivoTotal();
        if (cet == 0) return 0;
        return (calcularLucroLiquido() / cet) * 100;
    }

    // ✅ MÉTODOS DE COMPATIBILIDADE (para não quebrar o sistema existente)
    /**
     * @deprecated Use calcularFaturamento() em vez disso
     * Mantido para compatibilidade com código existente
     */
    @Deprecated
    public double getCustoEfetivoTotal() {
        return calcularCustoEfetivoTotal();
    }

    /**
     * @deprecated Use calcularLucroBruto() em vez disso
     * Mantido para compatibilidade com código existente
     */
    @Deprecated
    public double getLucroBruto() {
        return calcularLucroBruto();
    }

    /**
     * @deprecated Use calcularLucroLiquido() em vez disso
     * Mantido para compatibilidade com código existente
     */
    @Deprecated
    public double getLucroLiquido() {
        return calcularLucroLiquido();
    }

    /**
     * @deprecated Use calcularROI() em vez disso
     * Mantido para compatibilidade com código existente
     */
    @Deprecated
    public double getROI() {
        return calcularROI();
    }

    // ✅ MÉTODOS PARA GESTÃO DE ITENS
    public void adicionarItem(ItemVenda item) {
        itens.add(item);
        item.setVenda(this);
    }

    public void removerItem(ItemVenda item) {
        itens.remove(item);
        item.setVenda(null);
    }

    public double calcularCustoTotalDosItens() {
        return itens.stream()
                .mapToDouble(item -> item.getCustoTotal().doubleValue())
                .sum();
    }

    // GETTERS E SETTERS
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

    public String getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(String idPedido) {
        this.idPedido = idPedido;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(String plataforma) {
        this.plataforma = plataforma;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(double precoVenda) {
        this.precoVenda = precoVenda;
    }

    public double getCustoProdutoVendido() {
        return custoProdutoVendido;
    }

    public void setCustoProdutoVendido(double custoProdutoVendido) {
        this.custoProdutoVendido = custoProdutoVendido;
    }

    public double getFretePagoPeloCliente() {
        return fretePagoPeloCliente;
    }

    public void setFretePagoPeloCliente(double fretePagoPeloCliente) {
        this.fretePagoPeloCliente = fretePagoPeloCliente;
    }

    public double getCustoEnvio() {
        return custoEnvio;
    }

    public void setCustoEnvio(double custoEnvio) {
        this.custoEnvio = custoEnvio;
    }

    public double getTarifaPlataforma() {
        return tarifaPlataforma;
    }

    public void setTarifaPlataforma(double tarifaPlataforma) {
        this.tarifaPlataforma = tarifaPlataforma;
    }

    // ✅ NOVO GETTER/SETTER para despesas operacionais
    public double getDespesasOperacionais() {
        return despesasOperacionais;
    }

    public void setDespesasOperacionais(double despesasOperacionais) {
        this.despesasOperacionais = despesasOperacionais;
    }

    // 🆕 GETTER E SETTER PARA USER
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public List<ItemVenda> getItens() {
        return itens;
    }

    public void setItens(List<ItemVenda> itens) {
        this.itens = itens;
    }

    // 🆕 toString() ATUALIZADO
    @Override
    public String toString() {
        return "Venda{" +
                "id=" + id +
                ", idPedido='" + idPedido + '\'' +
                ", plataforma='" + plataforma + '\'' +
                ", quantidade=" + quantidade +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }
}