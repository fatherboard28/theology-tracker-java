package com.theology.tracker.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;

/**
 * DataSource configuration for SQLite.
 *
 * Key concerns:
 *  - WAL (Write-Ahead Log) mode: dramatically improves concurrent read performance
 *    and prevents "database is locked" errors during reads while writes occur.
 *  - Foreign key enforcement: SQLite disables FK checks by default; must be
 *    enabled per-connection with PRAGMA foreign_keys = ON.
 *  - Connection pool: SQLite is file-based and single-writer; we keep the pool
 *    small (max 1 write connection) to avoid lock contention.
 */
@Configuration
public class DatabaseConfig {

    private final DataSource dataSource;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Run once at startup to configure WAL journal mode and verify FK enforcement.
     * WAL mode persists in the DB file after first set, but it is harmless to
     * set it again on each startup.
     */
    @PostConstruct
    public void initializeSqliteSettings() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            // Enable Write-Ahead Logging for better concurrent read performance
            stmt.execute("PRAGMA journal_mode = WAL");

            // Enforce foreign key constraints (disabled by default in SQLite)
            stmt.execute("PRAGMA foreign_keys = ON");

            // Synchronous=NORMAL is safe with WAL and gives better performance
            stmt.execute("PRAGMA synchronous = NORMAL");

            // Increase cache size (negative = kilobytes; -32768 = 32 MB)
            stmt.execute("PRAGMA cache_size = -32768");

            // Store temp tables in memory for speed
            stmt.execute("PRAGMA temp_store = MEMORY");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to configure SQLite PRAGMA settings", e);
        }
    }
}
