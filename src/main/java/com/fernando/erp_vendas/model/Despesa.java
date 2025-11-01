package com.fernando.erp_vendas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesa")
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "categoria", nullable = false, length = 50)
    private String categoria;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "recorrente", nullable = false)
    private boolean recorrente = false;

    // 🆕 RELAÇÃO COM USUÁRIO - MULTI-TENANCY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // ✅ ALTERADO: Substituído @JsonIgnoreProperties por @JsonIgnore
    private User user;

    // Construtores
    public Despesa() {
    }

    // 🆕 CONSTRUTOR ATUALIZADO COM USER
    public Despesa(String descricao, BigDecimal valor, LocalDate data,
                   String categoria, String observacoes, boolean recorrente, User user) {
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.categoria = categoria;
        this.observacoes = observacoes;
        this.recorrente = recorrente;
        this.user = user;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public boolean isRecorrente() { return recorrente; }
    public void setRecorrente(boolean recorrente) { this.recorrente = recorrente; }

    // 🆕 GETTER E SETTER PARA USER
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Despesa{" +
                "id=" + id +
                ", descricao='" + descricao + '\'' +
                ", valor=" + valor +
                ", data=" + data +
                ", categoria='" + categoria + '\'' +
                ", recorrente=" + recorrente +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }
}