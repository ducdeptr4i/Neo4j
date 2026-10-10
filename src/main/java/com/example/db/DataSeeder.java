package com.example.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Đọc file seed.cypher và thực thi từng câu lệnh Cypher vào Neo4j.
 * Chỉ seed khi DB chưa có node Person (idempotent).
 */
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final Neo4jClient client;

    public DataSeeder(Neo4jClient client) {
        this.client = client;
    }

    /**
     * Kiểm tra DB có rỗng không (chưa có Person nào).
     */
    public boolean isDatabaseEmpty() {
        try (var session = client.session()) {
            var result = session.executeRead(tx -> {
                var res = tx.run("MATCH (p:Person) RETURN count(p) AS cnt");
                return res.single().get("cnt").asLong();
            });
            return result == 0L;
        } catch (Exception e) {
            log.error("Lỗi kiểm tra DB: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Thực thi seed.cypher nếu DB rỗng.
     */
    public void seedIfEmpty() {
        if (!isDatabaseEmpty()) {
            log.info("DB đã có dữ liệu Person, bỏ qua seed.");
            return;
        }
        log.info("DB rỗng → bắt đầu seed dữ liệu từ seed.cypher...");
        runCypherFile("db/seed.cypher");
        runCypherFile("db/patch.cypher");
        log.info("Seed hoàn tất.");
    }

    /**
     * Đọc file Cypher từ classpath, tách theo ';', chạy từng câu.
     */
    public void runCypherFile(String classpathPath) {
        String content = readClasspathResource(classpathPath);
        if (content == null) {
            log.warn("Không tìm thấy resource: {}", classpathPath);
            return;
        }

        List<String> statements = splitStatements(content);
        log.info("Tìm thấy {} câu lệnh trong {}", statements.size(), classpathPath);

        try (var session = client.session()) {
            for (String stmt : statements) {
                String trimmed = stmt.strip();
                if (trimmed.isEmpty()) continue;

                // Bỏ qua câu lệnh MATCH ... RETURN dùng để xem trong Neo4j Browser
                if (trimmed.toUpperCase().startsWith("MATCH") && trimmed.toUpperCase().contains("RETURN")) {
                    // Chỉ bỏ nếu là câu SELECT-only (không có CREATE/MERGE/SET/DELETE)
                    String upper = trimmed.toUpperCase();
                    if (!upper.contains("CREATE") && !upper.contains("MERGE")
                            && !upper.contains("SET") && !upper.contains("DELETE")) {
                        log.debug("Bỏ qua câu MATCH/RETURN: {}", trimmed.substring(0, Math.min(60, trimmed.length())));
                        continue;
                    }
                }

                try {
                    session.executeWrite(tx -> {
                        tx.run(trimmed);
                        return null;
                    });
                    log.debug("OK: {}", trimmed.substring(0, Math.min(80, trimmed.length())));
                } catch (Exception e) {
                    log.warn("Lỗi câu lệnh '{}...': {}", trimmed.substring(0, Math.min(60, trimmed.length())), e.getMessage());
                }
            }
        }
    }

    /**
     * Tách nội dung Cypher thành danh sách câu lệnh theo ký tự ';'.
     * Bỏ qua comment '//' và dòng trống.
     */
    private List<String> splitStatements(String content) {
        List<String> result = new ArrayList<>();
        // Tách theo dấu ';'
        String[] parts = content.split(";");
        for (String part : parts) {
            // Loại bỏ dòng comment '//' nhưng giữ lại nội dung thực
            String cleaned = removeLineComments(part).strip();
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    /**
     * Xóa các dòng bắt đầu bằng '//' (comment Cypher).
     */
    private String removeLineComments(String text) {
        return text.lines()
                .filter(line -> !line.strip().startsWith("//"))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Đọc file từ classpath với encoding UTF-8.
     */
    private String readClasspathResource(String path) {
        try (InputStream in = DataSeeder.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("Lỗi đọc resource {}: {}", path, e.getMessage());
            return null;
        }
    }
}
