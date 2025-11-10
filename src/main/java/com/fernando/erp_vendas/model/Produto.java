package com.fernando.erp_vendas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String sku;

    private String asin;
    private String descricao;

    @Column(name = "estoque_minimo")
    private Integer estoqueMinimo = 0;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao = LocalDateTime.now();

    // ✅ CORREÇÃO: Adicionar @JsonIgnore para evitar serialização circular
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    // ✅ CORREÇÃO: Adicionar @JsonIgnore na lista também
    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<EntradaEstoque> entradaEstoques = new ArrayList<>();

    // Construtores
    public Produto() {}

    public Produto(String nome, String sku, String asin, String descricao, Integer estoqueMinimo, User user) {
        this.nome = nome;
        this.sku = sku;
        this.asin = asin;
        this.descricao = descricao;
        this.estoqueMinimo = estoqueMinimo;
        this.user = user;
    }

    // Getters e Setters
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

    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<EntradaEstoque> getEntradaEstoques() { return entradaEstoques; }
    public void setEntradaEstoques(List<EntradaEstoque> entradaEstoques) { this.entradaEstoques = entradaEstoques; }

    // ✅ CORREÇÃO CRÍTICA: Método de estoque total com tratamento de null
    public Integer getQuantidadeEstoqueTotal() {
        if (entradaEstoques == null || entradaEstoques.isEmpty()) {
            System.out.println("⚠️  ESTOQUE VAZIO - Produto: " + this.nome + " (entradaEstoques is null or empty)");
            return 0;
        }

        Integer total = 0;
        int entradasComSaldo = 0;

        for (EntradaEstoque entrada : entradaEstoques) {
            Integer saldoEntrada = entrada.getSaldo();
            if (saldoEntrada != null) {
                total += saldoEntrada;
                entradasComSaldo++;
            } else {
                System.out.println("⚠️  Entrada com saldo NULL - Produto: " + this.nome + ", Entrada ID: " + entrada.getId());
            }
        }

        System.out.println("📊 ESTOQUE CALCULADO - Produto: " + this.nome +
                ", Total: " + total +
                ", Entradas com saldo: " + entradasComSaldo +
                ", Total entradas: " + entradaEstoques.size());

        return total;
    }

    // ✅ NOVO: Método para debug completo do estoque
    public void debugEstoque() {
        System.out.println("\n🔍 ===== DEBUG ESTOQUE COMPLETO =====");
        System.out.println("📦 Produto: " + this.nome + " (ID: " + this.id + ")");
        System.out.println("👤 Usuário: " + (this.user != null ? this.user.getUsername() : "NULL"));
        System.out.println("📁 Total de entradas: " + (entradaEstoques != null ? entradaEstoques.size() : 0));

        if (entradaEstoques == null || entradaEstoques.isEmpty()) {
            System.out.println("❌ NENHUMA ENTRADA DE ESTOQUE ENCONTRADA!");
            return;
        }

        int totalQuantidade = 0;
        int totalSaldo = 0;
        int entradasComUser = 0;

        for (int i = 0; i < entradaEstoques.size(); i++) {
            EntradaEstoque entrada = entradaEstoques.get(i);
            String userInfo = entrada.getUser() != null ? entrada.getUser().getUsername() : "❌ NULL";
            if (entrada.getUser() != null) entradasComUser++;

            System.out.println("   [" + i + "] Entrada ID: " + entrada.getId() +
                    " | Qtd: " + entrada.getQuantidade() +
                    " | Saldo: " + (entrada.getSaldo() != null ? entrada.getSaldo() : "❌ NULL") +
                    " | User: " + userInfo +
                    " | Custo: " + (entrada.getCustoUnitario() != null ? entrada.getCustoUnitario() : "NULL"));

            totalQuantidade += entrada.getQuantidade() != null ? entrada.getQuantidade() : 0;
            totalSaldo += entrada.getSaldo() != null ? entrada.getSaldo() : 0;
        }

        System.out.println("📊 RESUMO:");
        System.out.println("   - Quantidade total comprada: " + totalQuantidade);
        System.out.println("   - Saldo total disponível: " + totalSaldo);
        System.out.println("   - Entradas com usuário: " + entradasComUser + "/" + entradaEstoques.size());
        System.out.println("   - Estoque calculado: " + getQuantidadeEstoqueTotal());
        System.out.println("🔍 ===== FIM DEBUG ESTOQUE =====\n");
    }

    // ✅ NOVO: Método para verificar se há entradas sem usuário
    public boolean temEntradasSemUsuario() {
        if (entradaEstoques == null) return false;

        for (EntradaEstoque entrada : entradaEstoques) {
            if (entrada.getUser() == null) {
                return true;
            }
        }
        return false;
    }

    // ✅ NOVO: Método para contar entradas por usuário
    public void analisarUsuariosEntradas() {
        if (entradaEstoques == null) {
            System.out.println("❌ Nenhuma entrada para analisar");
            return;
        }

        System.out.println("\n👤 ==== ANÁLISE DE USUÁRIOS NAS ENTRADAS ====");
        System.out.println("📦 Produto: " + this.nome);

        java.util.Map<String, Integer> usuariosCount = new java.util.HashMap<>();

        for (EntradaEstoque entrada : entradaEstoques) {
            String usuario = entrada.getUser() != null ? entrada.getUser().getUsername() : "❌ SEM USUÁRIO";
            usuariosCount.put(usuario, usuariosCount.getOrDefault(usuario, 0) + 1);
        }

        for (java.util.Map.Entry<String, Integer> entry : usuariosCount.entrySet()) {
            System.out.println("   - " + entry.getKey() + ": " + entry.getValue() + " entradas");
        }
        System.out.println("👤 ==== FIM ANÁLISE ====\n");
    }

    public boolean temEstoqueSuficiente(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            return false;
        }

        Integer estoqueTotal = getQuantidadeEstoqueTotal();
        boolean suficiente = estoqueTotal >= quantidade;

        System.out.println("🔍 VERIFICAÇÃO ESTOQUE - Produto: " + this.nome +
                ", Necessário: " + quantidade +
                ", Disponível: " + estoqueTotal +
                ", Suficiente: " + suficiente);

        return suficiente;
    }
}