package com.fernando.erp_vendas.config;
/*

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MigrationDataSourceConfig {

    // DataSource para Railway (origem)
    @Bean(name = "railwayDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.railway")
    public DataSource railwayDataSource() {
        return DataSourceBuilder.create().build();
    }

    // DataSource para Render (destino) - PRIMARY
    @Bean(name = "renderDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.render")
    public DataSource renderDataSource() {
        return DataSourceBuilder.create().build();
    }

    // JdbcTemplate para Railway (facilita consultas SQL)
    @Bean(name = "railwayJdbcTemplate")
    public JdbcTemplate railwayJdbcTemplate(
            @Qualifier("railwayDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // JdbcTemplate para Render
    @Bean(name = "renderJdbcTemplate")
    public JdbcTemplate renderJdbcTemplate(
            @Qualifier("renderDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
*/