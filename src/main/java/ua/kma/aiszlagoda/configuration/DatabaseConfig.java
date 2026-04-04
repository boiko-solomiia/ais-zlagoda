package ua.kma.aiszlagoda.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration

public class DatabaseConfig {

    @Bean
    public Connection connection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/zlagoda",
                "root",
                "zlagoda"
        );
    }
}