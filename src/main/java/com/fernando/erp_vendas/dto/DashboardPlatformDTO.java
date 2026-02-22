package com.fernando.erp_vendas.dto;

import java.math.BigDecimal;

public class DashboardPlatformDTO {
    private String name;
    private BigDecimal value;

    // ✅ CONSTRUTOR 1: Aceita Double (que é o que o Hibernate geralmente retorna em somas)
    public DashboardPlatformDTO(String name, Double value) {
        this.name = name;
        this.value = value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    // ✅ CONSTRUTOR 2: Aceita BigDecimal (caso o banco retorne BigDecimal direto)
    public DashboardPlatformDTO(String name, BigDecimal value) {
        this.name = name;
        this.value = value != null ? value : BigDecimal.ZERO;
    }

    public DashboardPlatformDTO() {
    }

    // Getters e Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}