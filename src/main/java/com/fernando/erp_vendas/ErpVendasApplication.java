package com.fernando.erp_vendas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController  // ← ADICIONE ESTA LINHA
public class ErpVendasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpVendasApplication.class, args);
    }

    // 🆕 ADICIONE ESTE MÉTODO DE TESTE
    @GetMapping("/app-test")
    public String appTest() {
        System.out.println("🎯 ERPVENDASAPPLICATION - Endpoint /app-test CHAMADO!");
        return "Aplicação Spring Boot funcionando! - " + java.time.LocalDateTime.now();
    }

    // 🆕 ADICIONE TAMBÉM UM HEALTH SIMPLES
    @GetMapping("/app-health")
    public String appHealth() {
        System.out.println("🎯 ERPVENDASAPPLICATION - Endpoint /app-health CHAMADO!");
        return "{\"status\":\"OK\",\"app\":\"ErpVendas\",\"time\":\"" + java.time.LocalDateTime.now() + "\"}";
    }
}