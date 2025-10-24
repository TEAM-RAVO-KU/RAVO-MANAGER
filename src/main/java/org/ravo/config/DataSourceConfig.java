package org.ravo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

// 두 DataSource 빈 설정
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.live")
    public DataSource liveDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.standby")
    public DataSource standbyDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.batch")
    public DataSource batchDataSource() {
        return DataSourceBuilder.create().build();
    }
    @Bean
    public JdbcTemplate liveJdbcTemplate(@Qualifier("liveDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate standbyJdbcTemplate(@Qualifier("standbyDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate batchJdbcTemplate(@Qualifier("batchDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}

