package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fernando.erp_vendas.dto.TopProdutoDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
// 🚨 REMOVIDO: @CrossOrigin(origins = "*") para usar apenas o do SecurityConfig
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
            results = vendaRepository.findLucroPorPlataformaAnualNative(
                    user.getId(),
                    hoje.getYear()
            );
        } else {
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

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProdutoDTO>> getTopProducts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "all") String period
    ) {
        LocalDateTime dataInicio = null;
        LocalDateTime agora = LocalDateTime.now();

        switch (period.toLowerCase()) {
            case "7d":
                dataInicio = agora.minusDays(7);
                break;
            case "30d":
                dataInicio = agora.minusDays(30);
                break;
            case "month":
                dataInicio = agora.withDayOfMonth(1).withHour(0).withMinute(0);
                break;
            case "year":
                dataInicio = agora.withDayOfYear(1).withHour(0).withMinute(0);
                break;
            case "all":
            default:
                dataInicio = null;
                break;
        }

        List<Object[]> resultadosNativos = vendaRepository.findTopProdutosPorPeriodoNative(user.getId(), dataInicio, limit);

        List<TopProdutoDTO> topProdutos = resultadosNativos.stream().map(row -> {
            TopProdutoDTO dto = new TopProdutoDTO();

            dto.setProdutoId(((Number) row[0]).longValue());
            dto.setProdutoNome((String) row[1]);
            dto.setImagemUrl((String) row[2]);
            dto.setQuantidadeVendida(((Number) row[3]).longValue());

            dto.setPrecoMedioVenda(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0);
            dto.setCustoMedio(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);
            dto.setLucroPorUnidade(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0);

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(topProdutos);
    }
}