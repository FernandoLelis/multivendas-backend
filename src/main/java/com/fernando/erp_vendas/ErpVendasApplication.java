package com.fernando.erp_vendas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class ErpVendasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpVendasApplication.class, args);
    }

    // 🆕 ENDPOINT DE TESTE NA CLASSE PRINCIPAL
    @GetMapping("/app-test")
    public Map<String, String> appTest() {
        System.out.println("🎯 ERPVENDASAPPLICATION - Endpoint /app-test CHAMADO!");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Aplicação Spring Boot funcionando!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SUCCESS");
        return response;
    }

    // 🆕 HEALTH CHECK SIMPLES
    @GetMapping("/app-health")
    public Map<String, String> appHealth() {
        System.out.println("🎯 ERPVENDASAPPLICATION - Endpoint /app-health CHAMADO!");
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "ERP Vendas Backend");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("environment", System.getenv("SPRING_PROFILES_ACTIVE") != null ?
                System.getenv("SPRING_PROFILES_ACTIVE") : "default");
        return response;
    }

    // 🆕 ENDPOINT RAIZ
    @GetMapping("/")
    public Map<String, String> home() {
        System.out.println("🎯 ERPVENDASAPPLICATION - Endpoint / (raiz) CHAMADO!");
        Map<String, String> response = new HashMap<>();
        response.put("message", "ERP Vendas API - Sistema de gestão multivendas");
        response.put("version", "1.0.0");
        response.put("status", "online");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}