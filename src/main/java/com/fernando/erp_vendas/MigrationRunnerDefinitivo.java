package com.fernando.erp_vendas;

import java.sql.*;
import java.util.*;

public class MigrationRunnerDefinitivo {

    private static final String RAILWAY_URL = "jdbc:postgresql://shinkansen.proxy.rlwy.net:32914/railway";
    private static final String RAILWAY_USER = "postgres";
    private static final String RAILWAY_PASSWORD = "emYmtGxYtgcHbtncAIzoIbNEycbmpbpk";

    private static final String RENDER_URL = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
    private static final String RENDER_USER = "multivendas_user";
    private static final String RENDER_PASSWORD = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 MIGRADOR DEFINITIVO");
        System.out.println("======================\n");

        try (Connection railway = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD);
             Connection render = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {

            // 1. Verificar se estamos no PostgreSQL correto
            if (!isPostgreSQL(render)) {
                System.err.println("❌ ERRO: Não está conectado ao PostgreSQL Render!");
                System.err.println("   Execute com: ./mvnw spring-boot:run -Dspring.profiles.active=prod");
                return;
            }

            // 2. Mostrar estrutura REAL
            System.out.println("🔍 ESTRUTURA ATUAL DO RENDER:");
            showTableStructure(render);

            // 3. Perguntar
            System.out.print("\n⚠️  Migrar dados do Railway para esta estrutura? (s/n): ");
            int input = System.in.read();
            if (input != 's' && input != 'S') {
                System.out.println("Cancelado.");
                return;
            }

            // 4. Criar ALTER TABLE para adicionar colunas faltantes
            System.out.println("\n🔧 PREPARANDO ESTRUTURA...");
            prepareStructure(railway, render);

            // 5. Migrar
            System.out.println("\n📦 MIGRANDO DADOS...");
            migrateData(railway, render);

            System.out.println("\n🎉 MIGRAÇÃO CONCLUÍDA!");
            System.out.println("👉 Agora teste: ./mvnw spring-boot:run -Dspring.profiles.active=prod");

        } catch (SQLException e) {
            System.err.println("❌ ERRO SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isPostgreSQL(Connection conn) throws SQLException {
        return conn.getMetaData().getDatabaseProductName().equals("PostgreSQL");
    }

    private static void showTableStructure(Connection conn) throws SQLException {
        String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

        for (String table : tables) {
            System.out.println("\n📊 " + table.toUpperCase() + ":");

            String sql = "SELECT column_name, data_type, is_nullable " +
                    "FROM information_schema.columns " +
                    "WHERE table_name = ? AND table_schema = 'public' " +
                    "ORDER BY ordinal_position";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, table);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("   %-25s %-20s %s%n",
                                rs.getString("column_name"),
                                rs.getString("data_type"),
                                "NO".equals(rs.getString("is_nullable")) ? "NOT NULL" : "NULL");
                    }
                }
            }
        }
    }

    private static void prepareStructure(Connection railway, Connection render) throws SQLException {
        // Adicionar colunas faltantes baseado no Railway
        String[][] alterStatements = {
                // tabela, coluna, tipo, default
                {"users", "ativo", "BOOLEAN", "DEFAULT true"},
                {"users", "data_criacao", "TIMESTAMP", "DEFAULT CURRENT_TIMESTAMP"},
                {"despesa", "observacoes", "VARCHAR(500)", "DEFAULT ''"},
                {"despesa", "recorrente", "BOOLEAN", "DEFAULT false"}
        };

        for (String[] alter : alterStatements) {
            String table = alter[0];
            String column = alter[1];
            String type = alter[2];
            String defaultValue = alter[3];

            // Verificar se coluna já existe
            if (!columnExists(render, table, column)) {
                System.out.print("   Adicionando " + table + "." + column + "... ");
                try (Statement stmt = render.createStatement()) {
                    String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s %s",
                            table, column, type, defaultValue);
                    stmt.execute(sql);
                    System.out.println("✅");
                } catch (SQLException e) {
                    System.out.println("⚠️  " + e.getMessage());
                }
            }
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = ? AND column_name = ? AND table_schema = 'public'";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, table);
            pstmt.setString(2, column);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void migrateData(Connection railway, Connection render) throws SQLException {
        // ORDEM CRÍTICA
        String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

        // Primeiro limpar TUDO do Render
        System.out.println("🧹 Limpando Render...");
        String[] reverseTables = {"despesa", "item_venda", "venda", "entrada_estoque", "produto", "users"};
        for (String table : reverseTables) {
            try (Statement stmt = render.createStatement()) {
                stmt.execute("DELETE FROM " + table);
            } catch (SQLException e) {
                System.out.println("   " + table + ": " + e.getMessage());
            }
        }

        // Migrar tabela por tabela
        for (String table : tables) {
            System.out.print("\n   " + table + ": ");

            try {
                // Obter colunas COMUNS entre Railway e Render
                List<String> commonColumns = getCommonColumns(railway, render, table);

                if (commonColumns.isEmpty()) {
                    System.out.println("❌ Nenhuma coluna comum");
                    continue;
                }

                // Migrar
                int count = migrateTable(railway, render, table, commonColumns);
                System.out.println("✅ " + count + " registros");

            } catch (SQLException e) {
                System.out.println("❌ Erro: " + e.getMessage());
            }
        }

        // Ajustar sequences
        System.out.println("\n🔧 Ajustando sequences...");
        adjustSequences(render);
    }

    private static List<String> getCommonColumns(Connection conn1, Connection conn2, String table) throws SQLException {
        Set<String> cols1 = getColumnNames(conn1, table);
        Set<String> cols2 = getColumnNames(conn2, table);

        cols1.retainAll(cols2); // Intersecção
        List<String> result = new ArrayList<>(cols1);
        Collections.sort(result);
        return result;
    }

    private static Set<String> getColumnNames(Connection conn, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        String sql = "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = ? AND table_schema = 'public'";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, table);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString("column_name").toLowerCase());
                }
            }
        }
        return columns;
    }

    private static int migrateTable(Connection source, Connection target, String table, List<String> columns)
            throws SQLException {

        String selectSql = "SELECT " + String.join(", ", columns) + " FROM " + table + " ORDER BY id";
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String insertSql = "INSERT INTO " + table + " (" + String.join(", ", columns) +
                ") VALUES (" + placeholders + ")";

        try (Statement selectStmt = source.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = selectStmt.executeQuery(selectSql);
             PreparedStatement insertStmt = target.prepareStatement(insertSql)) {

            selectStmt.setFetchSize(100);
            int inserted = 0;

            while (rs.next()) {
                for (int i = 0; i < columns.size(); i++) {
                    Object value = rs.getObject(i + 1);

                    // Tratamento especial para colunas NOT NULL
                    if (value == null) {
                        String column = columns.get(i);
                        if (column.equals("ativo")) {
                            insertStmt.setBoolean(i + 1, true); // default true
                        } else if (column.equals("recorrente")) {
                            insertStmt.setBoolean(i + 1, false); // default false
                        } else if (column.equals("data_criacao")) {
                            insertStmt.setTimestamp(i + 1, new Timestamp(System.currentTimeMillis()));
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

                String sql = "SELECT COALESCE(MAX(id), 0) FROM " + tableName;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        long maxId = rs.getLong(1);
                        if (maxId > 0) {
                            String setvalSql = "SELECT setval('" + seqName + "', " + maxId + ")";
                            try (Statement setvalStmt = conn.createStatement()) {
                                setvalStmt.execute(setvalSql);
                            }
                            System.out.println("   ✅ " + seqName + " → " + maxId);
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("   ⚠️  " + seq[0] + ": " + e.getMessage());
            }
        }
    }
}