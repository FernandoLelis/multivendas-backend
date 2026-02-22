package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard") // Verifique se sua rota base é essa ou /api/vendas
public class DashboardController {

    @Autowired
    private VendaRepository vendaRepository;

    @GetMapping("/platform-data")
    public ResponseEntity<List<Map<String, Object>>> getPlatformData(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "month") String period
    ) {
        LocalDate hoje = LocalDate.now();
        List<Object[]> results;

        if ("year".equalsIgnoreCase(period)) {
            // Busca dados do ANO inteiro
            results = vendaRepository.findLucroPorPlataformaAnualNative(
                    user.getId(),
                    hoje.getYear()
            );
        } else {
            // Padrão: Busca dados do MÊS atual
            results = vendaRepository.findLucroPorPlataformaNative(
                    user.getId(),
                    hoje.getMonthValue(),
                    hoje.getYear()
            );
        }

        List<Map<String, Object>> response = results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", row[0]);
            map.put("value", row[1]);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}