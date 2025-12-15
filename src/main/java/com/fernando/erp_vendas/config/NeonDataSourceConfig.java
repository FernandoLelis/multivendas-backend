package com.fernando.erp_vendas.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class NeonDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Configuração básica do Neon
        dataSource.setJdbcUrl("jdbc:postgresql://ep-winter-sky-ad2f0h2g-pooler.c-2.us-east-1.aws.neon.tech:5432/neondb");
        dataSource.setUsername("neondb_owner");
        dataSource.setPassword("npg_4XBPYDqJaE7S");
        dataSource.setDriverClassName("org.postgresql.Driver");

        // Parâmetros SSL para Neon
        dataSource.addDataSourceProperty("sslmode", "require");
        dataSource.addDataSourceProperty("ssl", "true");
        dataSource.addDataSourceProperty("sslfactory", "org.postgresql.ssl.DefaultSSLFactory");

        // Otimizações para serverless
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(30000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setPoolName("NeonHikariPool");

        return dataSource;
    }
}