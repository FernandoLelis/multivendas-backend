package com.fernando.erp_vendas.service;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseMigrationService {

    // Configurações FIXAS - não usar properties
    private static final String RAILWAY_URL = "jdbc:postgresql://shinkansen.proxy.rlwy.net:32914/railway";
    private static final String RAILWAY_USER = "postgres";
    private static final String RAILWAY_PASSWORD = "emYmtGxYtgcHbtncAIzoIbNEycbmpbpk";

    private static final String RENDER_URL = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
    private static final String RENDER_USER = "multivendas_user";
    private static final String RENDER_PASSWORD = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

    /**
     * Testa conexão com ambos bancos usando JDBC DIRETO
     */
    public String testConnections() {
        StringBuilder result = new StringBuilder();
        result.append("🔍 TESTANDO CONEXÕES DIRETAS (JDBC)\n");
        result.append("====================================\n");

        // Testar Railway
        result.append("\n🚂 RAILWAY:\n");
        try (Connection conn = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD)) {
            result.append("   ✅ Conectado: ").append(conn.getMetaData().getURL()).append("\n");

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'")) {
                if (rs.next()) {
                    result.append("   📊 Tabelas: ").append(rs.getInt(1)).append("\n");
                }
            }

            // Listar tabelas
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name")) {
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
                result.append("   📋 Tabelas encontradas: ").append(String.join(", ", tables)).append("\n");
            }

        } catch (Exception e) {
            result.append("   ❌ Erro: ").append(e.getMessage()).append("\n");
        }

        // Testar Render
        result.append("\n🎯 RENDER:\n");
        try (Connection conn = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {
            result.append("   ✅ Conectado: ").append(conn.getMetaData().getURL()).append("\n");

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'")) {
                if (rs.next()) {
                    result.append("   📊 Tabelas: ").append(rs.getInt(1)).append("\n");
                }
            }

            // Listar tabelas
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name")) {
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
                result.append("   📋 Tabelas encontradas: ").append(String.join(", ", tables)).append("\n");
            }

        } catch (Exception e) {
            result.append("   ❌ Erro: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }

    /**
     * Migra uma tabela usando JDBC DIRETO
     */
    public String migrateTable(String tableName) {
        try {
            // Conectar aos dois bancos
            Connection railwayConn = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD);
            Connection renderConn = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD);

            // 1. Verificar se tabela existe no Railway
            String checkSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = LOWER('" + tableName + "') AND table_schema = 'public'";

            Statement checkStmt = railwayConn.createStatement();
            ResultSet checkRs = checkStmt.executeQuery(checkSql);
            checkRs.next();
            int exists = checkRs.getInt(1);
            checkStmt.close();

            if (exists == 0) {
                railwayConn.close();
                renderConn.close();
                return "❌ Tabela '" + tableName + "' não existe no Railway";
            }

            // 2. Contar registros no Railway
            Statement countStmt = railwayConn.createStatement();
            ResultSet countRs = countStmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
            countRs.next();
            int countRailway = countRs.getInt(1);
            countStmt.close();

            if (countRailway == 0) {
                railwayConn.close();
                renderConn.close();
                return "ℹ️  Tabela '" + tableName + "' vazia no Railway";
            }

            // 3. Limpar tabela no Render
            Statement truncateStmt = renderConn.createStatement();
            truncateStmt.execute("TRUNCATE TABLE " + tableName + " CASCADE");
            truncateStmt.close();

            // 4. Pegar dados do Railway
            Statement selectStmt = railwayConn.createStatement();
            ResultSet dataRs = selectStmt.executeQuery("SELECT * FROM " + tableName + " ORDER BY id");

            // 5. Pegar metadados para saber colunas
            int columnCount = dataRs.getMetaData().getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(dataRs.getMetaData().getColumnName(i));
            }

            // 6. Preparar INSERT para Render
            StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
            insertSql.append(String.join(", ", columns));
            insertSql.append(") VALUES (");
            for (int i = 0; i < columnCount; i++) {
                insertSql.append("?");
                if (i < columnCount - 1) insertSql.append(", ");
            }
            insertSql.append(")");

            // 7. Inserir dados
            var insertStmt = renderConn.prepareStatement(insertSql.toString());
            int inserted = 0;

            while (dataRs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    insertStmt.setObject(i, dataRs.getObject(i));
                }
                insertStmt.addBatch();
                inserted++;

                // Executar batch a cada 100 registros
                if (inserted % 100 == 0) {
                    insertStmt.executeBatch();
                }
            }

            // Executar batch final
            insertStmt.executeBatch();

            // Fechar recursos
            dataRs.close();
            selectStmt.close();
            insertStmt.close();

            // 8. Contar no Render
            Statement finalCountStmt = renderConn.createStatement();
            ResultSet finalCountRs = finalCountStmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
            finalCountRs.next();
            int countRender = finalCountRs.getInt(1);
            finalCountStmt.close();

            railwayConn.close();
            renderConn.close();

            return String.format("✅ %s: %d → %d registros migrados", tableName, countRailway, countRender);

        } catch (Exception e) {
            return "❌ Erro em '" + tableName + "': " + e.getMessage();
        }
    }

    /**
     * Migra tabela por tabela na ordem correta
     */
    public String migrateAll() {
        StringBuilder result = new StringBuilder();
        result.append("🚀 MIGRAÇÃO RAILWAY → RENDER (JDBC DIRETO)\n");
        result.append("===========================================\n\n");

        String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

        int success = 0;
        int totalRecords = 0;

        for (String table : tables) {
            result.append("📦 ").append(table.toUpperCase()).append(": ");

            String tableResult = migrateTable(table);
            result.append(tableResult).append("\n");

            if (tableResult.startsWith("✅")) {
                success++;
                // Extrair número de registros
                try {
                    String[] parts = tableResult.split("→");
                    if (parts.length > 1) {
                        String numStr = parts[1].replaceAll("[^0-9]", "");
                        totalRecords += Integer.parseInt(numStr.trim());
                    }
                } catch (Exception e) {
                    // Ignorar erro de parse
                }
            }
        }

        // Ajustar sequences (opcional)
        result.append("\n🔧 AJUSTANDO SEQUENCES...\n");
        try (Connection renderConn = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {
            String[] sequences = {"users_id_seq", "produto_id_seq", "venda_id_seq",
                    "entrada_estoque_id_seq", "item_venda_id_seq", "despesa_id_seq"};

            for (String seq : sequences) {
                try {
                    String table = seq.replace("_id_seq", "");
                    Statement stmt = renderConn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) FROM " + table);
                    rs.next();
                    long maxId = rs.getLong(1);

                    if (maxId > 0) {
                        stmt.execute("SELECT setval('" + seq + "', " + maxId + ")");
                        result.append("   ✅ ").append(seq).append(" → ").append(maxId).append("\n");
                    }
                    stmt.close();
                } catch (Exception e) {
                    result.append("   ⚠️  ").append(seq).append(": ").append(e.getMessage()).append("\n");
                }
            }
        } catch (Exception e) {
            result.append("   ❌ Erro conectando para ajustar sequences: ").append(e.getMessage()).append("\n");
        }

        result.append("\n===========================================\n");
        result.append("🎉 MIGRAÇÃO CONCLUÍDA!\n");
        result.append("📊 RESUMO:\n");
        result.append("   • Tabelas migradas: ").append(success).append("/").append(tables.length).append("\n");
        result.append("   • Registros totais: ").append(totalRecords).append("\n");
        result.append("\n👉 Teste agora seu sistema com os dados reais!");

        return result.toString();
    }

    /**
     * Teste simples: migra apenas 1 registro
     */
    public String testMigrationWithOneRecord() {
        try (Connection railwayConn = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD);
             Connection renderConn = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {

            // Pegar primeiro usuário
            Statement stmt = railwayConn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users LIMIT 1");

            if (!rs.next()) {
                return "❌ Nenhum usuário encontrado no Railway";
            }

            // Pegar colunas
            int columnCount = rs.getMetaData().getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(rs.getMetaData().getColumnName(i));
            }

            // Verificar se já existe no Render
            String email = rs.getString("email");
            Statement checkStmt = renderConn.createStatement();
            ResultSet checkRs = checkStmt.executeQuery("SELECT COUNT(*) FROM users WHERE email = '" + email + "'");
            checkRs.next();
            int exists = checkRs.getInt(1);
            checkStmt.close();

            if (exists > 0) {
                return "ℹ️  Usuário '" + email + "' já existe no Render";
            }

            // Preparar INSERT
            StringBuilder insertSql = new StringBuilder("INSERT INTO users (");
            insertSql.append(String.join(", ", columns));
            insertSql.append(") VALUES (");
            for (int i = 0; i < columnCount; i++) {
                insertSql.append("?");
                if (i < columnCount - 1) insertSql.append(", ");
            }
            insertSql.append(")");

            // Executar INSERT
            var insertStmt = renderConn.prepareStatement(insertSql.toString());
            for (int i = 1; i <= columnCount; i++) {
                insertStmt.setObject(i, rs.getObject(i));
            }
            insertStmt.executeUpdate();

            insertStmt.close();
            stmt.close();

            return "✅ TESTE OK: Usuário '" + email + "' migrado com sucesso!";

        } catch (Exception e) {
            return "❌ Teste falhou: " + e.getMessage();
        }
    }
}