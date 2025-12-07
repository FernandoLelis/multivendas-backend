package com.fernando.erp_vendas;

import java.sql.*;

public class TesteRender {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
        String user = "multivendas_user";
        String password = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

        System.out.println("🧪 TESTE DE CONEXÃO RENDER");
        System.out.println("===========================\n");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ CONECTADO AO RENDER POSTGRESQL!");
            System.out.println("📡 URL: " + conn.getMetaData().getURL());
            System.out.println("🧠 PostgreSQL: " + conn.getMetaData().getDatabaseProductVersion());

            // Verificar se está realmente no Render
            String host = conn.getMetaData().getURL();
            if (host.contains("render.com")) {
                System.out.println("🎯 Confirmado: Banco no RENDER");
            } else {
                System.out.println("⚠️  ATENÇÃO: Não parece ser o Render!");
            }

            System.out.println("\n📊 TABELAS EXISTENTES:");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT table_name FROM information_schema.tables " +
                                 "WHERE table_schema = 'public' ORDER BY table_name")) {
                int count = 0;
                while (rs.next()) {
                    System.out.println("   • " + rs.getString("table_name"));
                    count++;
                }
                System.out.println("\n   Total: " + count + " tabelas");
            }

            System.out.println("\n🔢 CONTAGEM DE DADOS:");
            String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};
            for (String table : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        System.out.println("   " + table + ": " + rs.getInt(1) + " registros");
                    }
                } catch (SQLException e) {
                    System.out.println("   " + table + ": ❌ " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.err.println("\n❌ FALHA NA CONEXÃO: " + e.getMessage());
            System.err.println("\n👉 Verifique:");
            System.err.println("   1. Credenciais corretas");
            System.err.println("   2. Internet funcionando");
            System.err.println("   3. Banco ativo no Render");
        }
    }
}