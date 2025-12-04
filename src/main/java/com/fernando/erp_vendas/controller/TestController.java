package com.fernando.erp_vendas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/test-simple")
    public Map<String, String> testSimple() {
        System.out.println("✅ TEST CONTROLLER - Endpoint /test-simple chamado!");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Test controller funcionando!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/health-simple")
    public Map<String, String> healthSimple() {
        System.out.println("✅ TEST CONTROLLER - Endpoint /health-simple chamado!");
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
}