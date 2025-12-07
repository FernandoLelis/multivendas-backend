package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.service.DataMigrationService;
import com.fernando.erp_vendas.service.DatabaseMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/migracao")
public class MigrationController {

    @Autowired
    private DataMigrationService dataMigrationService;

    @Autowired
    private DatabaseMigrationService databaseMigrationService;

    // ==================== MIGRAÇÃO INTERNA (PEPS) ====================

    @PostMapping("/peps/{userId}")
    public ResponseEntity<String> migrarEstoqueUsuario(@PathVariable Long userId) {
        try {
            String resultado = dataMigrationService.migrarEstoqueParaPEPS(userId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/peps/todos")
    public ResponseEntity<String> migrarEstoqueTodosUsuarios() {
        try {
            String resultado = dataMigrationService.migrarEstoqueParaTodosUsuarios();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> verificarStatus() {
        try {
            String status = dataMigrationService.verificarStatusMigracao();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/corrigir-produtos")
    public ResponseEntity<String> corrigirProdutosSemUsuario() {
        try {
            String resultado = dataMigrationService.corrigirProdutosSemUsuario();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/estoque-para-peps")
    public ResponseEntity<String> migrarEstoqueParaPEPS() {
        try {
            String resultado = dataMigrationService.migrarEstoqueParaTodosUsuarios();
            return ResponseEntity.ok("Migração geral iniciada: " + resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    // ==================== MIGRAÇÃO BANCO DE DADOS (RAILWAY → RENDER) ====================

    @GetMapping("/database/test")
    public ResponseEntity<String> testDatabaseConnections() {
        try {
            String resultado = databaseMigrationService.testConnections();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/database/test-one")
    public ResponseEntity<String> testMigrationWithOneRecord() {
        try {
            String resultado = databaseMigrationService.testMigrationWithOneRecord();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/database/table/{tableName}")
    public ResponseEntity<String> migrateTable(@PathVariable String tableName) {
        try {
            String resultado = databaseMigrationService.migrateTable(tableName);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/database/all")
    public ResponseEntity<String> migrateAllDatabases() {
        try {
            String resultado = databaseMigrationService.migrateAll();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }
}