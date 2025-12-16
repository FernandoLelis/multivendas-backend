import java.sql.*;

public class TesteNeon {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-winter-sky-ad2f0h2g-pooler.c-2.us-east-1.aws.neon.tech:5432/neondb?user=neondb_owner&password=npg_4XBPYDqJaE7S&sslmode=require";

        try {
            System.out.println("Tentando conectar ao Neon...");
            Connection conn = DriverManager.getConnection(url);
            System.out.println("✅ Conexão bem-sucedida!");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}