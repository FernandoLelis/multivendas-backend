package com.fernando.erp_vendas;

import java.sql.*;
import java.util.*;

public class SchemaChecker {

    private static final String RAILWAY_URL = "jdbc:postgresql://shinkansen.proxy.rlwy.net:32914/railway";
    private static final String RAILWAY_USER = "postgres";
    private static final String RAILWAY_PASSWORD = "emYmtGxYtgcHbtncAIzoIbNEycbmpbpk";

    private static final String RENDER_URL = "jdbc:postgresql://dpg-d4o9ggggjchc73ci0vgg-a.oregon-postgres.render.com:5432/multivendas";
    private static final String RENDER_USER = "multivendas_user";
    private static final String RENDER_PASSWORD = "VqdZvz0FWzciqVF1cz3idd3N4Jl1ajPo";

    public static void main(String[] args) throws SQLException {
        System.out.println("🔍 COMPARADOR DE ESTRUTURAS DE TABELAS");
        System.out.println("========================================\n");

        try (Connection railway = DriverManager.getConnection(RAILWAY_URL, RAILWAY_USER, RAILWAY_PASSWORD);
             Connection render = DriverManager.getConnection(RENDER_URL, RENDER_USER, RENDER_PASSWORD)) {

            String[] tables = {"users", "produto", "entrada_estoque", "venda", "item_venda", "despesa"};

            for (String table : tables) {
                System.out.println("\n📊 TABELA: " + table.toUpperCase());
                System.out.println("─".repeat(50));

                Map<String, ColumnInfo> railwayCols = getColumns(railway, table);
                Map<String, ColumnInfo> renderCols = getColumns(render, table);

                System.out.println("Railway: " + railwayCols.size() + " colunas");
                System.out.println("Render:  " + renderCols.size() + " colunas");

                // Comparar
                compareColumns(table, railwayCols, renderCols);
            }

            System.out.println("\n✅ Análise concluída!");

        } catch (SQLException e) {
            System.err.println("❌ Erro: " + e.getMessage());
            throw e;
        }
    }

    private static Map<String, ColumnInfo> getColumns(Connection conn, String tableName) throws SQLException {
        Map<String, ColumnInfo> columns = new LinkedHashMap<>();

        String sql = "SELECT column_name, data_type, is_nullable, column_default " +
                "FROM information_schema.columns " +
                "WHERE table_name = LOWER(?) AND table_schema = 'public' " +
                "ORDER BY ordinal_position";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("column_name");
                    ColumnInfo info = new ColumnInfo(
                            name,
                            rs.getString("data_type"),
                            rs.getString("is_nullable"),
                            rs.getString("column_default")
                    );
                    columns.put(name.toLowerCase(), info);
                }
            }
        }

        return columns;
    }

    private static void compareColumns(String tableName, Map<String, ColumnInfo> railway, Map<String, ColumnInfo> render) {
        Set<String> allColumns = new TreeSet<>();
        allColumns.addAll(railway.keySet());
        allColumns.addAll(render.keySet());

        boolean hasDifferences = false;

        for (String col : allColumns) {
            ColumnInfo railwayInfo = railway.get(col);
            ColumnInfo renderInfo = render.get(col);

            if (railwayInfo == null) {
                System.out.println("❌ Apenas no RENDER: " + col + " " + renderInfo);
                hasDifferences = true;
            } else if (renderInfo == null) {
                System.out.println("❌ Apenas no RAILWAY: " + col + " " + railwayInfo);
                hasDifferences = true;
            } else if (!railwayInfo.equals(renderInfo)) {
                System.out.println("⚠️  Diferença em " + col + ":");
                System.out.println("   Railway: " + railwayInfo);
                System.out.println("   Render:  " + renderInfo);
                hasDifferences = true;
            }
        }

        if (!hasDifferences) {
            System.out.println("✅ Estruturas idênticas!");
        }
    }

    static class ColumnInfo {
        String name;
        String dataType;
        String nullable;
        String defaultValue;

        ColumnInfo(String name, String dataType, String nullable, String defaultValue) {
            this.name = name;
            this.dataType = dataType;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ColumnInfo other = (ColumnInfo) obj;
            return Objects.equals(dataType, other.dataType) &&
                    Objects.equals(nullable, other.nullable);
        }

        @Override
        public String toString() {
            return String.format("%s %s %s",
                    dataType,
                    "NO".equals(nullable) ? "NOT NULL" : "NULL",
                    defaultValue != null ? "DEFAULT " + defaultValue : ""
            ).trim();
        }
    }
}