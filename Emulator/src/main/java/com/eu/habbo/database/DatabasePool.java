package com.eu.habbo.database;

import com.eu.habbo.core.ConfigurationManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DatabasePool {
    private final Logger log = LoggerFactory.getLogger(DatabasePool.class);
    private static final String DB_POOL_MAX_SIZE = "db.pool.maxsize";
    private static final String DB_POOL_MIN_SIZE = "db.pool.minsize";
    private static final String DB_HOSTNAME_KEY = "db.hostname";
    private static final String DB_PORT_KEY = "db.port";
    private static final String DB_PASSWORD_KEY = "db.password";
    private static final String DB_NAME_KEY = "db.database";
    private static final String DB_USER_KEY = "db.username";
    private static final String DB_PARAMS_KEY = "db.params";
    private HikariDataSource database;
    private static DatabasePool instance;

    DatabasePool() {
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
            databaseConfiguration.setJdbcUrl("jdbc:mysql://" + config.getValue(DB_HOSTNAME_KEY, "localhost") + ":" + config.getValue(DB_PORT_KEY, "3306") + "/" + config.getValue(DB_NAME_KEY) + config.getValue(DB_PARAMS_KEY));
            databaseConfiguration.addDataSourceProperty("serverName", config.getValue(DB_HOSTNAME_KEY, "localhost"));
            databaseConfiguration.addDataSourceProperty("port", config.getValue(DB_PORT_KEY, "3306"));
            databaseConfiguration.addDataSourceProperty("databaseName", config.getValue(DB_NAME_KEY));
            databaseConfiguration.addDataSourceProperty("user", config.getValue(DB_USER_KEY));
            databaseConfiguration.addDataSourceProperty("password", config.getValue(DB_PASSWORD_KEY));
            log.info("INITIALIZING DATABASE SERVER: " + config.getValue(DB_HOSTNAME_KEY));
            log.info("ON PORT: " + config.getValue(DB_PORT_KEY));
            log.info("HABBO DATABASE: " + config.getValue(DB_NAME_KEY));

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