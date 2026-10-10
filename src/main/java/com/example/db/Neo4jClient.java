package com.example.db;

import com.example.config.AppConfig;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý kết nối Neo4j Driver.
 * Dùng singleton, đóng khi tắt ứng dụng.
 */
public class Neo4jClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Neo4jClient.class);

    private final Driver driver;
    private final String database;

    public Neo4jClient(AppConfig config) {
        String uri = config.getNeo4jUri();
        String user = config.getNeo4jUser();
        String password = config.getNeo4jPassword();
        this.database = config.getNeo4jDatabase();

        log.info("Kết nối Neo4j: {} user={} database={}", uri, user, database);
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    /**
     * Mở một session với database được cấu hình.
     */
    public Session session() {
        return driver.session(SessionConfig.forDatabase(database));
    }

    /**
     * Kiểm tra kết nối Neo4j có hoạt động không.
     */
    public boolean isConnected() {
        try {
            driver.verifyConnectivity();
            return true;
        } catch (Exception e) {
            log.warn("Neo4j không kết nối được: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.close();
            log.info("Neo4j driver đã đóng.");
        }
    }
}
