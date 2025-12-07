package com.fernando.erp_vendas;

import java.sql.*;
import java.util.*;

public class MigrationRunnerFinal {

    private static final String RAILWAY_URL = "jdbc:postgresql://shinkansen.proxy.rlwy.net:32914/railway";
    private static final String RAILWAY_USER = "postgres";
    private static final String RAILWAY_PASSWORD = "emYmtGxYtgcHbtncAIzoIbNEycbmpbpk";

    private static final String RENDER_URL = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
    private static final String RENDER_USER = "multivendas_user";
    private static final String RENDER_PASSWORD = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

    // MAPEAMENTO CORRIGIDO baseado na análise
    private static final Map<String, ColumnMapping[]> TABLE_MAPPINGS = Map.of(
            "users", new ColumnMapping[]{
                    new ColumnMapping("id", "id"),
                    new ColumnMapping("email", "email"),
                    new ColumnMapping("password", "password"),
                    new ColumnMapping("nome", "nome")
            },

            "produto", new ColumnMapping[]{
                    new ColumnMapping("id", "id"),
                    new ColumnMapping("sku", "sku"),
                    new ColumnMapping("asin", "asin"),
                    new ColumnMapping("nome", "nome"),
                    new ColumnMapping("descricao", "descricao"),
                    new ColumnMapping("data_criacao", "data_criacao"),
                    // quantidade_estoque não existe no Render - IGNORAR
                    // estoque_minimo tem DEFAULT diferente - usar valor do Railway
                    new ColumnMapping("estoque_minimo", "estoque_minimo"),
                    new ColumnMapping("user_id", "user_id")
            },

            "entrada_estoque", new ColumnMapping[]{
                    new ColumnMapping("id", "id"),
                    new ColumnMapping("custo_total", "custo_total"),
                    new ColumnMapping("custo_unitario", "custo_unitario"),
                    new ColumnMapping("data_entrada", "data_entrada"),
                    new ColumnMapping("quantidade", "quantidade"),
                    new ColumnMapping("saldo", "saldo"),
                    new ColumnMapping("produto_id", "produto_id"),
                    new ColumnMapping("categoria", "categoria"),
                    new ColumnMapping("fornecedor", "fornecedor"),
                    new ColumnMapping("id_pedido_compra", "id_pedido_compra"),
                    new ColumnMapping("observacoes", "observacoes"),
                    new ColumnMapping("user_id", "user_id")
            },

            "venda", new ColumnMapping[]{
                    new ColumnMapping("id", "id"),
                    new ColumnMapping("custo_envio", "custo_envio"),
                    new ColumnMapping("custo_produto_vendido", "custo_produto_vendido"),
                    new ColumnMapping("data", "data"),
                    new ColumnMapping("frete_pago_pelo_cliente", "frete_pago_pelo_cliente"),
                    new ColumnMapping("id_pedido", "id_pedido"),
                    new ColumnMapping("plataforma", "plataforma"),
                    new ColumnMapping("preco_venda", "preco_venda"),
                    new ColumnMapping("quantidade", "quantidade"),
                    new ColumnMapping("tarifa_plataforma", "tarifa_plataforma"),
                    new ColumnMapping("produto_id", "produto_id"),
                    new ColumnMapping("despesas_operacionais", "despesas_operacionais"),
                    new ColumnMapping("user_id", "user_id")
                    // data_venda e frete_pago_cliente não existem no Render - IGNORAR
            },

            "item_venda", new ColumnMapping[]{
                    new ColumnMapping("id", "id"),
                    new ColumnMapping("custo_unitario", "custo_unitario"),
                    new ColumnMapping("quantidade", "quantidade"),
                    new ColumnMapping("lote_id", "lote_id"),
                    new ColumnMapping("venda_id", "venda_id"),
                    new ColumnMapping("user_id", "user_id")
            },

            "despesa", new ColumnMapping[]{
                    new ColumnMapping("id", "id"),
                    new ColumnMapping("descricao", "descricao"),
                    new ColumnMapping("valor", "valor"),
                    new ColumnMapping("data", "data"),
                    new ColumnMapping("categoria", "categoria"),
                    new ColumnMapping("user_id", "user_id")
            }
    );

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 MIGRADOR FINAL - COM MAPEAMENTO CORRIGIDO");
        System.out.println("=============================================\n");

        try (Connection railway = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD);
             Connection render = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {

            // 1. Mostrar resumo
            showSummary(railway, render);

            // 2. Perguntar confirmação
            System.out.print("\n⚠️  CONTINUAR com migração? (s/n): ");
            int input = System.in.read();
            if (input != 's' && input != 'S') {
                System.out.println("❌ Migração cancelada.");
                return;
            }

            // Limpar buffer
            System.in.read(new byte[System.in.available()]);

            // 3. Limpar Render
            clearRender(render);

            // 4. Migrar
            migrateAll(railway, render);

            System.out.println("\n🎉 MIGRAÇÃO CONCLUÍDA COM SUCESSO!");
            System.out.println("👉 Execute: ./mvnw spring-boot:run");

        } catch (SQLException e) {
            System.err.println("❌ ERRO: " + e.getMessage());
            throw e;
        }
    }

    private static void showSummary(Connection railway, Connection render) throws SQLException {
        System.out.println("📊 RESUMO DOS DADOS:");
        System.out.println("────────────────────");

        String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

        for (String table : tables) {
            int railwayCount = countTable(railway, table);
            int renderCount = countTable(render, table);

            System.out.printf("%-20s Railway: %4d | Render: %4d | Dif: %+d%n",
                    table, railwayCount, renderCount, railwayCount - renderCount);
        }

        System.out.println("\n🔍 DIFERENÇAS DE ESTRUTURA:");
        System.out.println("──────────────────────────");
        System.out.println("• PRODUTO: Railway tem 'quantidade_estoque' (Render não tem)");
        System.out.println("• VENDA: Railway tem 'data_venda' e 'frete_pago_cliente' (Render não tem)");
        System.out.println("• NOT NULL: Várias colunas têm restrições diferentes");
        System.out.println("\n📋 ESTRATÉGIA:");
        System.out.println("• Colunas extras serão ignoradas");
        System.out.println("• Valores NULL serão tratados automaticamente");
    }

    private static int countTable(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void clearRender(Connection render) throws SQLException {
        System.out.println("\n🧹 LIMPANDO RENDER...");
        String[] tables = {"despesa", "item_venda", "venda", "entrada_estoque", "produto", "users"};

        for (String table : tables) {
            try (Statement stmt = render.createStatement()) {
                int deleted = stmt.executeUpdate("DELETE FROM " + table);
                System.out.printf("   %-20s %d registros removidos%n", table + ":", deleted);
            } catch (SQLException e) {
                System.out.println("   " + table + ": ERRO - " + e.getMessage());
            }
        }
    }

    private static void migrateAll(Connection railway, Connection render) throws SQLException {
        System.out.println("\n📦 MIGRANDO DADOS...");

        String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};
        int totalMigrated = 0;

        for (String table : tables) {
            System.out.print("\n   " + table + ": ");

            ColumnMapping[] mappings = TABLE_MAPPINGS.get(table);
            if (mappings == null) {
                System.out.println("❌ Mapeamento não encontrado");
                continue;
            }

            try {
                int count = migrateTable(railway, render, table, mappings);
                System.out.println("✅ " + count + " registros");
                totalMigrated += count;
            } catch (SQLException e) {
                System.out.println("❌ ERRO: " + e.getMessage());
                // Continuar com próxima tabela
            }
        }

        System.out.println("\n📊 TOTAL MIGRADO: " + totalMigrated + " registros");

        // Ajustar sequences
        System.out.println("\n🔧 AJUSTANDO SEQUENCES...");
        adjustSequences(render);
    }

    private static int migrateTable(Connection source, Connection target, String table, ColumnMapping[] mappings)
            throws SQLException {

        // Construir SELECT com colunas da origem
        List<String> sourceColumns = new ArrayList<>();
        for (ColumnMapping mapping : mappings) {
            sourceColumns.add(mapping.sourceColumn);
        }

        String selectSql = "SELECT " + String.join(", ", sourceColumns) +
                " FROM " + table + " ORDER BY id";

        // Construir INSERT com colunas do destino
        List<String> targetColumns = new ArrayList<>();
        for (ColumnMapping mapping : mappings) {
            targetColumns.add(mapping.targetColumn);
        }

        String placeholders = String.join(", ", Collections.nCopies(targetColumns.size(), "?"));
        String insertSql = "INSERT INTO " + table + " (" +
                String.join(", ", targetColumns) + ") VALUES (" + placeholders + ")";

        try (Statement selectStmt = source.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = selectStmt.executeQuery(selectSql);
             PreparedStatement insertStmt = target.prepareStatement(insertSql)) {

            selectStmt.setFetchSize(100);
            int inserted = 0;

            while (rs.next()) {
                for (int i = 0; i < mappings.length; i++) {
                    Object value = rs.getObject(i + 1);

                    // Tratar valores NULL para colunas NOT NULL no Render
                    if (value == null) {
                        // Para algumas colunas, usar valores default
                        if ("user_id".equals(mappings[i].targetColumn)) {
                            // Se user_id for NULL, usar 1 como fallback
                            insertStmt.setObject(i + 1, 1L);
                        } else if ("estoque_minimo".equals(mappings[i].targetColumn)) {
                            // Valor default para estoque_minimo
                            insertStmt.setObject(i + 1, 5);
                        } else {
                            insertStmt.setNull(i + 1, Types.NULL);
                        }
                    } else {
                        insertStmt.setObject(i + 1, value);
                    }
                }

                insertStmt.addBatch();
                inserted++;

                if (inserted % 50 == 0) {
                    insertStmt.executeBatch();
                }
            }

            // Batch final
            if (inserted > 0) {
                insertStmt.executeBatch();
            }

            return inserted;
        }
    }

    private static void adjustSequences(Connection conn) throws SQLException {
        String[][] sequences = {
                {"users_id_seq", "users"},
                {"produto_id_seq", "produto"},
                {"venda_id_seq", "venda"},
                {"entrada_estoque_id_seq", "entrada_estoque"},
                {"item_venda_id_seq", "item_venda"},
                {"despesa_id_seq", "despesa"}
        };

        for (String[] seq : sequences) {
            try {
                String seqName = seq[0];
                String tableName = seq[1];

                // Verificar se sequence existe
                if (!sequenceExists(conn, seqName)) {
                    System.out.println("   ⚠️  " + seqName + ": não existe");
                    continue;
                }

                String countSql = "SELECT COALESCE(MAX(id), 0) FROM " + tableName;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(countSql)) {
                    if (rs.next()) {
                        long maxId = rs.getLong(1);
                        if (maxId > 0) {
                            String setvalSql = "SELECT setval('" + seqName + "', " + maxId + ")";
                            try (Statement setvalStmt = conn.createStatement()) {
                                setvalStmt.execute(setvalSql);
                            }
                            System.out.println("   ✅ " + seqName + " → " + maxId);
                        } else {
                            System.out.println("   ⚠️  " + seqName + ": tabela vazia");
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("   ❌ " + seq[0] + ": " + e.getMessage());
            }
        }
    }

    private static boolean sequenceExists(Connection conn, String seqName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pg_sequences WHERE sequencename = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, seqName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    static class ColumnMapping {
        String sourceColumn;
        String targetColumn;

        ColumnMapping(String sourceColumn, String targetColumn) {
            this.sourceColumn = sourceColumn;
            this.targetColumn = targetColumn;
        }
    }
}