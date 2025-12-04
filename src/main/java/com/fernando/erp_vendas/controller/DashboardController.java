package com.fernando.erp_vendas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fernando.erp_vendas.repository.VendaRepository;
import com.fernando.erp_vendas.repository.DespesaRepository;
import com.fernando.erp_vendas.model.Venda;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.dto.DashboardData;

@RestController
public class DashboardController {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private DespesaRepository despesaRepository;

    // 🆕 DTO SIMPLES PARA HEALTH CHECK
    public static class HealthResponse {
        private String status;
        private String timestamp;
        private String service;

        public HealthResponse(String status, String timestamp, String service) {
            this.status = status;
            this.timestamp = timestamp;
            this.service = service;
        }

        // Getters e Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
    }

    // 🆕 MÉTODO PARA OBTER USUÁRIO LOGADO
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Usuário não autenticado");
    }

    // 🆕 ENDPOINT ESPECIAL PARA RENDER HEALTH CHECK
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> renderHealthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Multivendas Backend - Render Health");
        response.put("note", "Use /api/health para API health check");
        return ResponseEntity.ok(response);
    }

    // 🆕 ENDPOINT PÚBLICO DE TESTE
    @GetMapping("/api/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "API está funcionando!");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        try {
            User currentUser = getCurrentUser();

            // ✅ ATUALIZADO: Busca todas as vendas DO USUÁRIO no banco
            List<Venda> vendas = vendaRepository.findByUser(currentUser);

            // ✅ CALCULAR TOTAIS DAS VENDAS DO USUÁRIO
            double faturamentoTotal = 0;
            double custoEfetivoTotal = 0;
            double lucroBrutoTotal = 0;

            for (Venda venda : vendas) {
                faturamentoTotal += venda.calcularFaturamento();
                custoEfetivoTotal += venda.calcularCustoEfetivoTotal();
                lucroBrutoTotal += venda.calcularLucroBruto();
                // ❌ REMOVIDO: Não somar despesas operacionais das vendas aqui
                // (já estão consideradas no cálculo do lucro bruto de cada venda)
            }

            // ✅ CORRIGIDO: CALCULAR APENAS DESPESAS GERAIS DO MÊS ATUAL DO USUÁRIO
            LocalDate primeiroDiaMes = LocalDate.now().withDayOfMonth(1);
            LocalDate ultimoDiaMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            BigDecimal totalDespesasMes = despesaRepository.calcularTotalDespesasPorPeriodo(
                    currentUser, primeiroDiaMes, ultimoDiaMes);
            double despesasGeraisTotal = totalDespesasMes != null ? totalDespesasMes.doubleValue() : 0.0;

            // ✅ CORRIGIDO: LUCRO LÍQUIDO = LUCRO BRUTO - DESPESAS GERAIS
            double lucroLiquidoCorrigido = lucroBrutoTotal - despesasGeraisTotal;

            // ✅ CALCULAR ROI
            double roiTotal = (custoEfetivoTotal > 0) ? (lucroLiquidoCorrigido / custoEfetivoTotal) * 100 : 0;

            // 🆕 CONSULTAS ADICIONAIS
            Long totalVendas = vendaRepository.countTotalVendas(currentUser);
            Long vendasMesAtual = vendaRepository.countVendasDoMes(currentUser,
                    LocalDate.now().getYear(), LocalDate.now().getMonthValue());
            List<Object[]> faturamentoPorPlataforma = vendaRepository.findFaturamentoPorPlataforma(currentUser);
            List<Object[]> produtosMaisVendidos = vendaRepository.findProdutosMaisVendidos(currentUser);
            List<Object[]> topCategoriasDespesas = despesaRepository.findTopCategoriasComMaiorGasto(currentUser);

            // Monta o objeto de resposta CORRIGIDO
            DashboardData dashboardData = new DashboardData();
            dashboardData.setFaturamentoTotal(faturamentoTotal);
            dashboardData.setCustoEfetivoTotal(custoEfetivoTotal);
            dashboardData.setLucroBrutoTotal(lucroBrutoTotal);
            dashboardData.setLucroLiquidoTotal(lucroLiquidoCorrigido);

            // ✅ CORRIGIDO: Despesas operacionais totais = APENAS despesas gerais do mês
            dashboardData.setDespesasOperacionaisTotal(despesasGeraisTotal);

            // ✅ MANTIDOS para compatibilidade (se necessário no futuro)
            // dashboardData.setDespesasVendas(0.0); // Removido do cálculo
            // dashboardData.setDespesasGerais(despesasGeraisTotal); // Removido do cálculo

            dashboardData.setRoiTotal(roiTotal);
            dashboardData.setTotalVendas(totalVendas != null ? totalVendas.intValue() : 0);
            dashboardData.setVendasMesAtual(vendasMesAtual != null ? vendasMesAtual.intValue() : 0);
            dashboardData.setFaturamentoPorPlataforma(faturamentoPorPlataforma);
            dashboardData.setProdutosMaisVendidos(produtosMaisVendidos);
            dashboardData.setTopCategoriasDespesas(topCategoriasDespesas);

            // 🆕 LOG PARA DEBUG (remova depois de testar)
            System.out.println("📊 DASHBOARD DEBUG - User: " + currentUser.getUsername());
            System.out.println("💰 Faturamento: " + faturamentoTotal);
            System.out.println("💸 Custo: " + custoEfetivoTotal);
            System.out.println("📈 Lucro Bruto: " + lucroBrutoTotal);
            System.out.println("🧾 Despesas Gerais: " + despesasGeraisTotal);
            System.out.println("💵 Lucro Líquido: " + lucroLiquidoCorrigido);

            return ResponseEntity.ok(dashboardData);

        } catch (Exception e) {
            System.err.println("❌ ERRO NO DASHBOARD: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao carregar dashboard: " + e.getMessage());
        }
    }

    // 🆕 ENDPOINT PARA DADOS RESUMIDOS (mais rápido)
    @GetMapping("/dashboard/resumo")
    public ResponseEntity<?> getResumoDashboard() {
        try {
            User currentUser = getCurrentUser();

            // Dados básicos usando métodos otimizados do repository
            Long totalVendas = vendaRepository.countTotalVendas(currentUser);
            Double lucroLiquidoTotal = vendaRepository.findLucroLiquidoTotal(currentUser);
            Double custoEfetivoTotal = vendaRepository.findCustoEfetivoTotal(currentUser);

            // ✅ CORRIGIDO: Despesas do mês atual
            LocalDate primeiroDiaMes = LocalDate.now().withDayOfMonth(1);
            LocalDate ultimoDiaMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            BigDecimal totalDespesasMes = despesaRepository.calcularTotalDespesasPorPeriodo(
                    currentUser, primeiroDiaMes, ultimoDiaMes);
            double despesasTotais = totalDespesasMes != null ? totalDespesasMes.doubleValue() : 0.0;

            // ✅ CORRIGIDO: Calcular lucro líquido considerando despesas
            double lucroLiquidoCorrigido = 0.0;
            if (lucroLiquidoTotal != null) {
                lucroLiquidoCorrigido = lucroLiquidoTotal - despesasTotais;
            }

            // Calcular ROI
            double roiTotal = 0;
            if (custoEfetivoTotal != null && custoEfetivoTotal > 0) {
                roiTotal = (lucroLiquidoCorrigido / custoEfetivoTotal) * 100;
            }

            // Montar resposta resumida CORRIGIDA
            DashboardData resumo = new DashboardData();
            resumo.setTotalVendas(totalVendas != null ? totalVendas.intValue() : 0);
            resumo.setLucroLiquidoTotal(lucroLiquidoCorrigido);
            resumo.setRoiTotal(roiTotal);
            resumo.setDespesasOperacionaisTotal(despesasTotais);

            return ResponseEntity.ok(resumo);

        } catch (Exception e) {
            System.err.println("❌ ERRO NO RESUMO DO DASHBOARD: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao carregar resumo do dashboard: " + e.getMessage());
        }
    }
}