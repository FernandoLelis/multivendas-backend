package com.fernando.erp_vendas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleHealthController {

    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"UP\",\"service\":\"Multivendas Backend\"}";
    }
}