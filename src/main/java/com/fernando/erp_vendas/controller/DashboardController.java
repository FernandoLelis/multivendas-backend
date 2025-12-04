package com.fernando.erp_vendas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    // 🆕 ENDPOINT DE HEALTH CHECK SIMPLIFICADO
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        System.out.println("🎯 DASHBOARDCONTROLLER - Endpoint /health chamado!");

        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Multivendas Backend");
        response.put("controller", "DashboardController");

        return ResponseEntity.ok(response);
    }

    // 🆕 ENDPOINT DE TESTE SIMPLES
    @GetMapping("/api/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        System.out.println("🎯 DASHBOARDCONTROLLER - Endpoint /api/test chamado!");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Dashboard Controller funcionando!");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}