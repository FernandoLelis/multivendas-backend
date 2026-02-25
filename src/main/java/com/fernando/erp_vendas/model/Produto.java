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

    // ✅ NOVOS CAMPOS: Pesos, Medidas e Imagem
    @Column(name = "peso")
    private Double peso;

    @Column(name = "comprimento")
    private Double comprimento;

    @Column(name = "largura")
    private Double largura;

    @Column(name = "altura")
    private Double altura;

    @Column(name = "imagem_url", length = 1000)
    private String imagemUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<EntradaEstoque> entradaEstoques = new ArrayList<>();

    public Produto() {}

    public Produto(String nome, String sku, String asin, String descricao, Integer estoqueMinimo, User user) {
        this.nome = nome;
        this.sku = sku;
        this.asin = asin;
        this.descricao = descricao;
        this.estoqueMinimo = estoqueMinimo;
        this.user = user;
    }

    // Getters e Setters Anteriores
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

    // ✅ NOVOS GETTERS E SETTERS
    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }
    public Double getComprimento() { return comprimento; }
    public void setComprimento(Double comprimento) { this.comprimento = comprimento; }
    public Double getLargura() { return largura; }
    public void setLargura(Double largura) { this.largura = largura; }
    public Double getAltura() { return altura; }
    public void setAltura(Double altura) { this.altura = altura; }
    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }

    // (Mantenha os métodos getQuantidadeEstoqueTotal, debugEstoque, temEntradasSemUsuario, analisarUsuariosEntradas, temEstoqueSuficiente exatamente iguais...)
    public Integer getQuantidadeEstoqueTotal() {
        if (entradaEstoques == null || entradaEstoques.isEmpty()) return 0;
        Integer total = 0;
        for (EntradaEstoque entrada : entradaEstoques) {
            if (entrada.getSaldo() != null) total += entrada.getSaldo();
        }
        return total;
    }
}