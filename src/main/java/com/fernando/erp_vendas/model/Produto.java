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

    // Métodos de negócio
    public Integer getQuantidadeEstoqueTotal() {
        return entradaEstoques.stream()
                .mapToInt(EntradaEstoque::getSaldo)
                .sum();
    }

    public boolean temEstoqueSuficiente(Integer quantidade) {
        return getQuantidadeEstoqueTotal() >= quantidade;
    }
}