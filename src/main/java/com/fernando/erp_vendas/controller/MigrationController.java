package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.service.DataMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/migracao")
public class MigrationController {

    @Autowired
    private DataMigrationService dataMigrationService;

    // ✅ CORRIGIDO: Migrar para usuário específico
    @PostMapping("/peps/{userId}")
    public ResponseEntity<String> migrarEstoqueUsuario(@PathVariable Long userId) {
        try {
            String resultado = dataMigrationService.migrarEstoqueParaPEPS(userId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    // ✅ NOVO: Migrar para todos os usuários
    @PostMapping("/peps/todos")
    public ResponseEntity<String> migrarEstoqueTodosUsuarios() {
        try {
            String resultado = dataMigrationService.migrarEstoqueParaTodosUsuarios();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    // ✅ NOVO: Verificar status da migração
    @GetMapping("/status")
    public ResponseEntity<String> verificarStatus() {
        try {
            String status = dataMigrationService.verificarStatusMigracao();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    // ✅ NOVO: Corrigir produtos sem usuário
    @PostMapping("/corrigir-produtos")
    public ResponseEntity<String> corrigirProdutosSemUsuario() {
        try {
            String resultado = dataMigrationService.corrigirProdutosSemUsuario();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    // ✅ MANTIDO: Endpoint legado (agora migra para primeiro usuário)
    @PostMapping("/estoque-para-peps")
    public ResponseEntity<String> migrarEstoqueParaPEPS() {
        try {
            // Migrar para o primeiro usuário encontrado
            String resultado = dataMigrationService.migrarEstoqueParaTodosUsuarios();
            return ResponseEntity.ok("Migração geral iniciada: " + resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }
}