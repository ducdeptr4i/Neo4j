package com.example.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Đọc cấu hình từ config.properties và biến môi trường.
 * Biến môi trường có độ ưu tiên cao hơn file properties.
 */
public class AppConfig {

    private final Properties props = new Properties();

    public AppConfig() {
        // Nạp file config.properties từ classpath
        try (InputStream in = AppConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("[AppConfig] Không thể đọc config.properties: " + e.getMessage());
        }
    }

    /** Lấy giá trị, ưu tiên biến môi trường */
    private String get(String key, String envKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env.trim();
        return props.getProperty(key, defaultValue);
    }

    public String getNeo4jUri() {
        return get("neo4j.uri", "NEO4J_URI", "bolt://localhost:7687");
    }

    public String getNeo4jUser() {
        return get("neo4j.user", "NEO4J_USER", "neo4j");
    }

    /** Mật khẩu bắt buộc phải được cung cấp qua biến môi trường NEO4J_PASSWORD */
    public String getNeo4jPassword() {
        String env = System.getenv("NEO4J_PASSWORD");
        if (env != null && !env.isBlank()) return env.trim();
        return props.getProperty("neo4j.password", "");
    }

    public String getNeo4jDatabase() {
        return get("neo4j.database", "NEO4J_DATABASE", "neo4j");
    }

    public int getServerPort() {
        String portStr = get("server.port", "PORT", "8080");
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    public boolean isSeedIfEmpty() {
        String val = get("app.seed.ifEmpty", "APP_SEED_IF_EMPTY", "true");
        return "true".equalsIgnoreCase(val.trim());
    }
}
