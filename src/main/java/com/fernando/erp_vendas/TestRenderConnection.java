package com.fernando.erp_vendas;

import java.sql.*;

public class TestRenderConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
        String user = "multivendas_user";
        String password = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

        System.out.println("🔍 Testando conexão com Render...");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ Conectado ao Render!");
            System.out.println("URL: " + conn.getMetaData().getURL());

            // Contar tabelas
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'")) {
                if (rs.next()) {
                    System.out.println("📊 Tabelas: " + rs.getInt(1));
                }
            }

            // Listar dados atuais
            String[] tables = {"users", "produto", "venda", "despesa"};
            for (String table : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        System.out.println("   " + table + ": " + rs.getInt(1) + " registros");
                    }
                } catch (SQLException e) {
                    System.out.println("   " + table + ": ERRO - " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ ERRO: " + e.getMessage());
        }
    }
}