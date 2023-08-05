package com.eu.habbo.database;

import com.eu.habbo.core.ConfigurationManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DatabasePool {
    private static final String DB_POOL_MAX_SIZE = "db.pool.maxsize";
    private static final String DB_POOL_MIN_SIZE = "db.pool.minsize";
    private HikariDataSource database;
    private static DatabasePool instance;

    DatabasePool() {
        // Private constructor for singleton pattern
    }
    public static synchronized DatabasePool getInstance() {
        if (instance == null) {
            instance = new DatabasePool();
        }
        return instance;
    }

    public boolean getStoragePooling(ConfigurationManager config) {
        try {
            HikariConfig databaseConfiguration = new HikariConfig();
            databaseConfiguration.setMaximumPoolSize(config.getInt(DB_POOL_MAX_SIZE, 50));
            databaseConfiguration.setMinimumIdle(config.getInt(DB_POOL_MIN_SIZE, 10));
            databaseConfiguration.setJdbcUrl("jdbc:mariadb://" + config.getValue("db.hostname", "localhost") + ":" + config.getValue("db.port", "3306") + "/" + config.getValue("db.database", "habbo") + config.getValue("db.params"));
            databaseConfiguration.addDataSourceProperty("serverName", config.getValue("db.hostname", "localhost"));
            databaseConfiguration.addDataSourceProperty("port", config.getValue("db.port", "3306"));
            databaseConfiguration.addDataSourceProperty("databaseName", config.getValue("db.database", "habbo"));
            databaseConfiguration.addDataSourceProperty("user", config.getValue("db.username"));
            databaseConfiguration.addDataSourceProperty("password", config.getValue("db.password"));
            databaseConfiguration.addDataSourceProperty("dataSource.logger", "org.mariadb.jdbc.Driver");
            databaseConfiguration.addDataSourceProperty("dataSource.logSlowQueries", "true");
            databaseConfiguration.addDataSourceProperty("dataSource.dumpQueriesOnException", "true");
            databaseConfiguration.addDataSourceProperty("prepStmtCacheSize", "500");
            databaseConfiguration.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            // databaseConfiguration.addDataSourceProperty("dataSource.logWriter", Logging.getErrorsSQLWriter());
            databaseConfiguration.addDataSourceProperty("cachePrepStmts", "true");
            databaseConfiguration.addDataSourceProperty("useServerPrepStmts", "true");
            databaseConfiguration.addDataSourceProperty("rewriteBatchedStatements", "true");
            databaseConfiguration.addDataSourceProperty("useUnicode", "true");
            databaseConfiguration.setAutoCommit(true);
            databaseConfiguration.setConnectionTimeout(300000L);
            databaseConfiguration.setValidationTimeout(5000L);
            databaseConfiguration.setLeakDetectionThreshold(20000L);
            databaseConfiguration.setMaxLifetime(1800000L);
            databaseConfiguration.setIdleTimeout(600000L);
            //databaseConfiguration.setDriverClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
            this.database = new HikariDataSource(databaseConfiguration);
        } catch (Exception e) {
            log.error("Error initializing database connection pool: {}", e.getMessage());
            return false;
        }
        return true;
    }

    public HikariDataSource getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database connection pool is not initialized.");
        }
        return database;
    }
}