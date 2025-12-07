package com.fernando.erp_vendas;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MIGRADOR STANDALONE - Corrigido para Render
 */
public class MigrationRunner {

    // Configurações FIXAS
    private static final String RAILWAY_URL = "jdbc:postgresql://shinkansen.proxy.rlwy.net:32914/railway";
    private static final String RAILWAY_USER = "postgres";
    private static final String RAILWAY_PASSWORD = "emYmtGxYtgcHbtncAIzoIbNEycbmpbpk";

    private static final String RENDER_URL = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
    private static final String RENDER_USER = "multivendas_user";
    private static final String RENDER_PASSWORD = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

    public static void main(String[] args) {
        System.out.println("🚀 MIGRADOR RAILWAY → RENDER (VERSÃO CORRIGIDA)");
        System.out.println("================================================\n");

        try {
            // Testar conexões
            if (!testConnections()) {
                System.exit(1);
            }

            // Perguntar confirmação
            System.out.print("⚠️  ISSO VAI APAGAR TODOS DADOS ATUAIS DO RENDER! Continuar? (s/n): ");
            int input = System.in.read();
            if (input != 's' && input != 'S') {
                System.out.println("❌ Migração cancelada.");
                System.exit(0);
            }

            // Limpar console input buffer
            System.in.read(new byte[System.in.available()]);

            // Migrar tudo
            migrateAll();

            System.out.println("\n🎉 MIGRAÇÃO CONCLUÍDA COM SUCESSO!");
            System.out.println("👉 Agora execute seu sistema normal: ./mvnw spring-boot:run");

        } catch (Exception e) {
            System.err.println("❌ ERRO CRÍTICO: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean testConnections() {
        System.out.println("🔍 Testando conexões...");

        try (Connection railway = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD);
             Connection render = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {

            System.out.println("✅ Railway conectado: " + railway.getMetaData().getURL());
            System.out.println("✅ Render conectado: " + render.getMetaData().getURL());

            // Contar tabelas
            System.out.println("\n📊 Railway tem " + countTables(railway) + " tabelas");
            System.out.println("📊 Render tem " + countTables(render) + " tabelas");

            // Mostrar quantidade de dados
            showDataCount(railway, "Railway");
            showDataCount(render, "Render");

            return true;

        } catch (SQLException e) {
            System.err.println("❌ Erro de conexão: " + e.getMessage());
            return false;
        }
    }

    private static int countTables(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void showDataCount(Connection conn, String nome) throws SQLException {
        System.out.println("\n📈 " + nome + " - Contagem de registros:");
        String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

        for (String table : tables) {
            try {
                String sql = "SELECT COUNT(*) FROM " + table;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        System.out.println("   • " + table + ": " + rs.getInt(1) + " registros");
                    }
                }
            } catch (SQLException e) {
                System.out.println("   • " + table + ": ERRO - " + e.getMessage());
            }
        }
    }

    private static void migrateAll() throws SQLException {
        // ORDEM CRÍTICA - Render não permite desabilitar constraints, então precisamos ser cuidadosos
        String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

        System.out.println("\n🚀 Iniciando migração...");

        try (Connection railway = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD);
             Connection render = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {

            // 1. Primeiro, DELETAR dados do Render na ordem INVERSA (para respeitar foreign keys)
            System.out.println("\n🧹 Limpando dados do Render...");
            String[] reverseTables = {"despesa", "item_venda", "venda", "entrada_estoque", "produto", "users"};

            for (String table : reverseTables) {
                try {
                    System.out.print("   Limpando " + table + "... ");
                    try (Statement stmt = render.createStatement()) {
                        stmt.execute("DELETE FROM " + table);
                        System.out.println("✅");
                    }
                } catch (SQLException e) {
                    System.out.println("⚠️  " + e.getMessage());
                }
            }

            // 2. Migrar na ordem CORRETA
            System.out.println("\n📦 Migrando dados...");

            int totalMigrated = 0;
            int totalRecords = 0;

            for (String table : tables) {
                System.out.print("   " + table + ": ");

                MigrateResult result = migrateTable(railway, render, table);
                System.out.println(result.message);

                if (result.success) {
                    totalMigrated++;
                    totalRecords += result.recordsMigrated;
                }
            }

            // 3. Ajustar sequences
            System.out.println("\n🔧 Ajustando sequences...");
            adjustSequences(render);

            System.out.println("\n📊 RESUMO FINAL:");
            System.out.println("   • Tabelas migradas: " + totalMigrated + "/" + tables.length);
            System.out.println("   • Registros totais: " + totalRecords);

            // 4. Verificar integridade
            System.out.println("\n🔍 Verificando integridade...");
            verifyIntegrity(railway, render);

        }
    }

    private static MigrateResult migrateTable(Connection source, Connection target, String tableName) {
        try {
            // 1. Verificar se tabela existe
            if (!tableExists(source, tableName)) {
                return new MigrateResult(false, 0, "❌ Não existe no Railway");
            }

            // 2. Contar registros origem
            int countSource = countTable(source, tableName);
            if (countSource == 0) {
                return new MigrateResult(true, 0, "ℹ️  Vazia (0 registros)");
            }

            // 3. Pegar colunas
            List<String> columns = getColumns(source, tableName);

            // 4. Migrar dados (não precisa truncar, já deletamos)
            int migrated = copyData(source, target, tableName, columns);

            // 5. Verificar destino
            int countTarget = countTable(target, tableName);

            if (countSource == countTarget) {
                return new MigrateResult(true, migrated,
                        String.format("✅ %d registros", countTarget));
            } else {
                return new MigrateResult(false, migrated,
                        String.format("⚠️  %d → %d (diferente)", countSource, countTarget));
            }

        } catch (SQLException e) {
            return new MigrateResult(false, 0, "❌ Erro: " + e.getMessage());
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = LOWER(?) AND table_schema = 'public'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static int countTable(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static List<String> getColumns(Connection conn, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String sql = "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = LOWER(?) AND table_schema = 'public' " +
                "ORDER BY ordinal_position";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString(1));
                }
            }
        }
        return columns;
    }

    private static int copyData(Connection source, Connection target, String tableName, List<String> columns)
            throws SQLException {

        // SELECT da origem
        String selectSql = "SELECT * FROM " + tableName + " ORDER BY id";

        // INSERT no destino
        StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
        insertSql.append(String.join(", ", columns));
        insertSql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            insertSql.append("?");
            if (i < columns.size() - 1) insertSql.append(", ");
        }
        insertSql.append(")");

        try (Statement selectStmt = source.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = selectStmt.executeQuery(selectSql);
             PreparedStatement insertStmt = target.prepareStatement(insertSql.toString())) {

            selectStmt.setFetchSize(100);

            int batchSize = 0;
            int totalInserted = 0;

            while (rs.next()) {
                // Preencher parâmetros
                for (int i = 0; i < columns.size(); i++) {
                    insertStmt.setObject(i + 1, rs.getObject(i + 1));
                }

                insertStmt.addBatch();
                batchSize++;
                totalInserted++;

                // Executar batch a cada 50 registros (mais seguro)
                if (batchSize >= 50) {
                    insertStmt.executeBatch();
                    batchSize = 0;
                }
            }

            // Executar batch final
            if (batchSize > 0) {
                insertStmt.executeBatch();
            }

            return totalInserted;
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
                String checkSeqSql = "SELECT COUNT(*) FROM pg_sequences WHERE schemaname = 'public' AND sequencename = '" + seqName + "'";
                try (Statement checkStmt = conn.createStatement(); ResultSet checkRs = checkStmt.executeQuery(checkSeqSql)) {
                    if (checkRs.next() && checkRs.getInt(1) > 0) {
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
                    } else {
                        System.out.println("   ⚠️  " + seqName + ": sequence não existe");
                    }
                }
            } catch (SQLException e) {
                System.out.println("   ❌ " + seq[0] + ": " + e.getMessage());
            }
        }
    }

    private static void verifyIntegrity(Connection source, Connection target) throws SQLException {
        System.out.println("\n🔐 Verificando chaves estrangeiras...");

        // Verificar se há registros órfãos
        String[] checks = {
                "SELECT COUNT(*) as orfaos FROM produto p WHERE p.user_id NOT IN (SELECT id FROM users)",
                "SELECT COUNT(*) as orfaos FROM entrada_estoque e WHERE e.produto_id NOT IN (SELECT id FROM produto)",
                "SELECT COUNT(*) as orfaos FROM entrada_estoque e WHERE e.user_id NOT IN (SELECT id FROM users)",
                "SELECT COUNT(*) as orfaos FROM venda v WHERE v.produto_id NOT IN (SELECT id FROM produto)",
                "SELECT COUNT(*) as orfaos FROM venda v WHERE v.user_id NOT IN (SELECT id FROM users)",
                "SELECT COUNT(*) as orfaos FROM item_venda i WHERE i.venda_id NOT IN (SELECT id FROM venda)",
                "SELECT COUNT(*) as orfaos FROM item_venda i WHERE i.lote_id NOT IN (SELECT id FROM entrada_estoque)",
                "SELECT COUNT(*) as orfaos FROM item_venda i WHERE i.user_id NOT IN (SELECT id FROM users)",
                "SELECT COUNT(*) as orfaos FROM despesa d WHERE d.user_id NOT IN (SELECT id FROM users)"
        };

        int errors = 0;
        for (String check : checks) {
            try (Statement stmt = target.createStatement(); ResultSet rs = stmt.executeQuery(check)) {
                if (rs.next() && rs.getInt("orfaos") > 0) {
                    System.out.println("   ❌ Encontrados " + rs.getInt("orfaos") + " registros órfãos");
                    errors++;
                }
            } catch (SQLException e) {
                System.out.println("   ⚠️  Erro na verificação: " + e.getMessage());
            }
        }

        if (errors == 0) {
            System.out.println("   ✅ Todas chaves estrangeiras estão íntegras!");
        } else {
            System.out.println("   ⚠️  " + errors + " problemas de integridade encontrados");
        }
    }

    static class MigrateResult {
        boolean success;
        int recordsMigrated;
        String message;

        MigrateResult(boolean success, int recordsMigrated, String message) {
            this.success = success;
            this.recordsMigrated = recordsMigrated;
            this.message = message;
        }
    }
}