package com.fernando.erp_vendas;

import java.sql.*;
import java.util.*;

public class MigradorRenderParaNeon {

    // CONFIGURAÇÕES RENDER (origem)
    private static final String RENDER_URL = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
    private static final String RENDER_USER = "multivendas_user";
    private static final String RENDER_PASSWORD = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

    // CONFIGURAÇÕES NEON (destino) - SUA CONNECTION STRING
    private static final String NEON_URL = "jdbc:postgresql://ep-winter-sky-ad2f0h2g-pooler.c-2.us-east-1.aws.neon.tech:5432/neondb?sslmode=require";
    private static final String NEON_USER = "neondb_owner";
    private static final String NEON_PASSWORD = "npg_4XBPYDqJaE7S";

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 MIGRADOR RENDER → NEON");
        System.out.println("=========================\n");

        // Testar conexões primeiro
        if (!testarConexoes()) {
            System.exit(1);
        }

        // Mostrar resumo
        mostrarResumo();

        // Perguntar confirmação
        System.out.print("\n⚠️  Continuar com migração? (s/n): ");
        Scanner scanner = new Scanner(System.in);
        String resposta = scanner.nextLine().trim().toLowerCase();

        if (!resposta.equals("s")) {
            System.out.println("❌ Migração cancelada.");
            return;
        }

        // Migrar
        migrarTudo();

        System.out.println("\n🎉 MIGRAÇÃO CONCLUÍDA!");
        System.out.println("\n👉 ATUALIZE SUA APLICAÇÃO:");
        System.out.println("   1. No Render Dashboard, vá em 'Environment'");
        System.out.println("   2. Altere DATABASE_URL para:");
        System.out.println("      postgresql://neondb_owner:npg_4XBPYDqJaE7S@ep-winter-sky-ad2f0h2g-pooler.c-2.us-east-1.aws.neon.tech/neondb?sslmode=require");
        System.out.println("   3. Reinicie a aplicação");
    }

    private static boolean testarConexoes() {
        System.out.println("🔍 Testando conexões...");

        // Testar Render
        System.out.print("   Render: ");
        try (Connection conn = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {
            System.out.println("✅ Conectado!");
            System.out.println("      Banco: " + conn.getCatalog());
            System.out.println("      PostgreSQL: " + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.out.println("❌ Erro: " + e.getMessage());
            return false;
        }

        // Testar Neon
        System.out.print("   Neon: ");
        try (Connection conn = DriverManager.getConnection(NEON_URL, NEON_USER, NEON_PASSWORD)) {
            System.out.println("✅ Conectado!");
            System.out.println("      Banco: " + conn.getCatalog());
            System.out.println("      Host: " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            System.out.println("❌ Erro: " + e.getMessage());
            System.out.println("\n👉 Problemas comuns no Neon:");
            System.out.println("   • SSL deve estar habilitado (sslmode=require)");
            System.out.println("   • Verifique se o IP está liberado no Neon Console");
            System.out.println("   • Teste a conexão via: psql '" + NEON_URL.replace("jdbc:", "") + "'");
            return false;
        }

        return true;
    }

    private static void mostrarResumo() {
        System.out.println("\n📊 RESUMO DOS DADOS:");
        System.out.println("────────────────────");

        try (Connection render = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD);
             Connection neon = DriverManager.getConnection(NEON_URL, NEON_USER, NEON_PASSWORD)) {

            String[] tabelas = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

            System.out.println("TABELA               RENDER     NEON       AÇÃO");
            System.out.println("───────────────────────────────────────────────");

            for (String tabela : tabelas) {
                int countRender = contarTabela(render, tabela);
                int countNeon = contarTabela(neon, tabela);

                String acao;
                if (countRender == 0) {
                    acao = "VAZIA";
                } else if (countNeon == 0) {
                    acao = "MIGRAR";
                } else if (countRender == countNeon) {
                    acao = "OK";
                } else {
                    acao = "ATUALIZAR";
                }

                System.out.printf("%-20s %6d     %6d     %s%n",
                        tabela, countRender, countNeon, acao);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erro ao mostrar resumo: " + e.getMessage());
        }
    }

    private static int contarTabela(Connection conn, String tabela) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return -1; // Tabela não existe
        }
    }

    private static void migrarTudo() throws SQLException {
        System.out.println("\n📦 Iniciando migração...");

        try (Connection render = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD);
             Connection neon = DriverManager.getConnection(NEON_URL, NEON_USER, NEON_PASSWORD)) {

            // Desabilitar foreign keys temporariamente no Neon
            try (Statement stmt = neon.createStatement()) {
                stmt.execute("SET session_replication_role = 'replica'");
            } catch (SQLException e) {
                System.out.println("   ⚠️  Não foi possível desabilitar foreign keys (normal no Neon)");
            }

            // Ordem CRÍTICA - migrar na ordem correta
            String[] tabelas = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

            int totalMigrado = 0;

            for (String tabela : tabelas) {
                System.out.print("\n   " + tabela + ": ");

                try {
                    // Verificar se tabela existe no Render
                    if (!tabelaExiste(render, tabela)) {
                        System.out.println("❌ Não existe no Render");
                        continue;
                    }

                    // Obter colunas
                    List<String> colunas = obterColunas(render, tabela);
                    if (colunas.isEmpty()) {
                        System.out.println("❌ Sem colunas");
                        continue;
                    }

                    // Verificar se precisa criar tabela no Neon
                    if (!tabelaExiste(neon, tabela)) {
                        System.out.print("(criando tabela...) ");
                        criarTabela(render, neon, tabela);
                    }

                    // Limpar dados existentes no Neon
                    limparTabela(neon, tabela);

                    // Migrar dados
                    int migrados = migrarTabelaDados(render, neon, tabela, colunas);

                    System.out.println("✅ " + migrados + " registros");
                    totalMigrado += migrados;

                } catch (SQLException e) {
                    System.out.println("❌ Erro: " + e.getMessage());
                }
            }

            // Reabilitar foreign keys
            try (Statement stmt = neon.createStatement()) {
                stmt.execute("SET session_replication_role = 'origin'");
            } catch (SQLException e) {
                // Ignorar erro
            }

            System.out.println("\n📊 TOTAL MIGRADO: " + totalMigrado + " registros");

            // Ajustar sequences
            System.out.println("\n🔧 Ajustando sequences...");
            ajustarSequences(neon);

        }
    }

    private static boolean tabelaExiste(Connection conn, String tabela) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_name = ? AND table_schema = 'public'";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tabela);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static List<String> obterColunas(Connection conn, String tabela) throws SQLException {
        List<String> colunas = new ArrayList<>();

        String sql = "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = ? AND table_schema = 'public' " +
                "ORDER BY ordinal_position";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tabela);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    colunas.add(rs.getString("column_name"));
                }
            }
        }

        return colunas;
    }

    private static void criarTabela(Connection fonte, Connection destino, String tabela) throws SQLException {
        // Obter DDL da tabela do Render
        String ddl = gerarDDLFromRender(fonte, tabela);

        if (ddl.isEmpty()) {
            // Fallback: criar tabela básica
            ddl = gerarDDLBasico(tabela);
        }

        try (Statement stmt = destino.createStatement()) {
            stmt.execute(ddl);
        }
    }

    private static String gerarDDLFromRender(Connection conn, String tabela) throws SQLException {
        // Método simplificado - na prática use pg_dump ou consulte information_schema
        StringBuilder ddl = new StringBuilder();

        // Obter colunas e tipos
        String sql = "SELECT column_name, data_type, is_nullable, column_default " +
                "FROM information_schema.columns " +
                "WHERE table_name = ? AND table_schema = 'public' " +
                "ORDER BY ordinal_position";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tabela);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<String> colunas = new ArrayList<>();
                while (rs.next()) {
                    String coluna = rs.getString("column_name");
                    String tipo = rs.getString("data_type");
                    String nullable = "NO".equals(rs.getString("is_nullable")) ? "NOT NULL" : "";
                    String defaultValue = rs.getString("column_default") != null ?
                            " DEFAULT " + rs.getString("column_default") : "";

                    colunas.add(coluna + " " + tipo + " " + nullable + defaultValue);
                }

                if (!colunas.isEmpty()) {
                    ddl.append("CREATE TABLE ").append(tabela).append(" (\n");
                    ddl.append("    ").append(String.join(",\n    ", colunas));
                    ddl.append("\n)");
                }
            }
        }

        return ddl.toString();
    }

    private static String gerarDDLBasico(String tabela) {
        // DDLs básicas baseadas no seu modelo
        Map<String, String> ddls = Map.of(
                "users", """
                CREATE TABLE users (
                    id BIGSERIAL PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    nome VARCHAR(255) NOT NULL,
                    ativo BOOLEAN NOT NULL DEFAULT true,
                    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """,

                "produto", """
                CREATE TABLE produto (
                    id BIGSERIAL PRIMARY KEY,
                    sku VARCHAR(255),
                    asin VARCHAR(255),
                    nome VARCHAR(255) NOT NULL,
                    descricao TEXT,
                    data_criacao TIMESTAMP,
                    estoque_minimo INTEGER,
                    user_id BIGINT
                )
                """,

                "entrada_estoque", """
                CREATE TABLE entrada_estoque (
                    id BIGSERIAL PRIMARY KEY,
                    custo_total NUMERIC(10,2) NOT NULL,
                    custo_unitario NUMERIC(10,2) NOT NULL,
                    data_entrada TIMESTAMP NOT NULL,
                    quantidade INTEGER NOT NULL,
                    saldo INTEGER NOT NULL,
                    produto_id BIGINT NOT NULL,
                    categoria VARCHAR(255),
                    fornecedor VARCHAR(255),
                    id_pedido_compra VARCHAR(255),
                    observacoes VARCHAR(1000),
                    user_id BIGINT NOT NULL
                )
                """,

                "venda", """
                CREATE TABLE venda (
                    id BIGSERIAL PRIMARY KEY,
                    custo_envio DOUBLE PRECISION,
                    custo_produto_vendido DOUBLE PRECISION,
                    data TIMESTAMP NOT NULL,
                    frete_pago_pelo_cliente DOUBLE PRECISION,
                    id_pedido VARCHAR(255),
                    plataforma VARCHAR(255),
                    preco_venda DOUBLE PRECISION,
                    quantidade INTEGER,
                    tarifa_plataforma DOUBLE PRECISION,
                    produto_id BIGINT,
                    despesas_operacionais DOUBLE PRECISION,
                    user_id BIGINT
                )
                """,

                "item_venda", """
                CREATE TABLE item_venda (
                    id BIGSERIAL PRIMARY KEY,
                    custo_unitario NUMERIC(10,2) NOT NULL,
                    quantidade INTEGER NOT NULL,
                    lote_id BIGINT NOT NULL,
                    venda_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL
                )
                """,

                "despesa", """
                CREATE TABLE despesa (
                    id BIGSERIAL PRIMARY KEY,
                    descricao VARCHAR(200) NOT NULL,
                    valor NUMERIC(10,2) NOT NULL,
                    data DATE NOT NULL,
                    categoria VARCHAR(50) NOT NULL,
                    observacoes VARCHAR(500),
                    recorrente BOOLEAN NOT NULL DEFAULT false,
                    user_id BIGINT NOT NULL
                )
                """
        );

        return ddls.getOrDefault(tabela, "");
    }

    private static void limparTabela(Connection conn, String tabela) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM " + tabela);
        }
    }

    private static int migrarTabelaDados(Connection fonte, Connection destino, String tabela, List<String> colunas)
            throws SQLException {

        String selectSql = "SELECT " + String.join(", ", colunas) +
                " FROM " + tabela + " ORDER BY id";

        String placeholders = String.join(", ", Collections.nCopies(colunas.size(), "?"));
        String insertSql = "INSERT INTO " + tabela + " (" +
                String.join(", ", colunas) + ") VALUES (" + placeholders + ")";

        try (Statement selectStmt = fonte.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = selectStmt.executeQuery(selectSql);
             PreparedStatement insertStmt = destino.prepareStatement(insertSql)) {

            selectStmt.setFetchSize(100);
            int inseridos = 0;

            while (rs.next()) {
                for (int i = 0; i < colunas.size(); i++) {
                    Object valor = rs.getObject(i + 1);
                    insertStmt.setObject(i + 1, valor);
                }

                insertStmt.addBatch();
                inseridos++;

                if (inseridos % 100 == 0) {
                    insertStmt.executeBatch();
                }
            }

            if (inseridos > 0) {
                insertStmt.executeBatch();
            }

            return inseridos;
        }
    }

    private static void ajustarSequences(Connection conn) throws SQLException {
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
                String checkSql = "SELECT COUNT(*) FROM pg_sequences WHERE sequencename = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setString(1, seqName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            String countSql = "SELECT COALESCE(MAX(id), 0) FROM " + tableName;
                            try (Statement stmt = conn.createStatement();
                                 ResultSet countRs = stmt.executeQuery(countSql)) {
                                if (countRs.next()) {
                                    long maxId = countRs.getLong(1);
                                    if (maxId > 0) {
                                        String setvalSql = "SELECT setval('" + seqName + "', " + maxId + ")";
                                        try (Statement setvalStmt = conn.createStatement()) {
                                            setvalStmt.execute(setvalSql);
                                        }
                                        System.out.println("   ✅ " + seqName + " → " + maxId);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("   ⚠️  " + seq[0] + ": " + e.getMessage());
            }
        }
    }
}