package com.mediturno.poc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "DB_URL")
public class DbConfig {

	@Bean
	DataSource dataSource(@Value("${DB_URL}") String url,
			@Value("${DB_USER}") String username,
			@Value("${DB_PASS}") String password) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setJdbcUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setMaximumPoolSize(5);
		dataSource.setInitializationFailTimeout(-1);
		return dataSource;
	}

	@Bean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

}