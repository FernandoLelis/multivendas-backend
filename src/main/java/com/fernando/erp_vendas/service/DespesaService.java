// Arquivo: src/main/java/com/fernando/erp_vendas/service/DespesaService.java
package com.fernando.erp_vendas.service;

import com.fernando.erp_vendas.repository.DespesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DespesaService {

    @Autowired
    private DespesaRepository despesaRepository;

    public BigDecimal calcularTotalDespesasMesAtual() {
        LocalDate primeiroDiaMes = LocalDate.now().withDayOfMonth(1);
        LocalDate ultimoDiaMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        BigDecimal total = despesaRepository.calcularTotalDespesasPorPeriodo(primeiroDiaMes, ultimoDiaMes);
        return total != null ? total : BigDecimal.ZERO;
    }
}