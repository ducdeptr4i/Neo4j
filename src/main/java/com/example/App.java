package com.example;

import com.example.config.AppConfig;
import com.example.db.DataSeeder;
import com.example.db.Neo4jClient;
import com.example.engine.FamilyGraph;
import com.example.model.Marriage;
import com.example.model.Person;
import com.example.repository.PersonRepository;
import com.example.service.*;
import com.example.web.Routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Lớp khởi động ứng dụng:
 * 1. Đọc cấu hình
 * 2. Kết nối Neo4j
 * 3. Seed dữ liệu nếu DB rỗng
 * 4. Nạp đồ thị vào bộ nhớ (FamilyGraph)
 * 5. Khởi động web server Javalin
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // --- Đọc cấu hình ---
        AppConfig config = new AppConfig();

        // --- Kiểm tra mật khẩu Neo4j ---
        if (config.getNeo4jPassword().isBlank()) {
            log.error("Chưa cấu hình mật khẩu Neo4j. Hãy đặt biến môi trường NEO4J_PASSWORD.");
            System.err.println("[ERROR] Biến môi trường NEO4J_PASSWORD chưa được thiết lập!");
            System.exit(1);
        }

        // --- Kết nối Neo4j ---
        Neo4jClient neo4jClient = new Neo4jClient(config);

        if (!neo4jClient.isConnected()) {
            log.error("Không thể kết nối Neo4j tại {}. Vui lòng kiểm tra Neo4j server.", config.getNeo4jUri());
            System.err.println("[ERROR] Không kết nối được Neo4j!");
            System.exit(1);
        }
        log.info("Kết nối Neo4j thành công.");

        // --- Seed dữ liệu nếu cần ---
        DataSeeder seeder = new DataSeeder(neo4jClient);
        if (config.isSeedIfEmpty()) {
            seeder.seedIfEmpty();
        }

        // --- Khởi tạo Repository ---
        PersonRepository personRepo = new PersonRepository(neo4jClient);

        // --- Nạp đồ thị vào bộ nhớ ---
        FamilyGraph graph = loadFamilyGraph(personRepo);
        log.info("Đã nạp FamilyGraph: {} người", graph.size());

        // --- Khởi tạo Services ---
        MarriageCheckService marriageCheckService = new MarriageCheckService(personRepo, graph);
        KinshipService kinshipService = new KinshipService(personRepo, graph);
        DataQualityService dataQualityService = new DataQualityService(personRepo, marriageCheckService);
        GraphViewService graphViewService = new GraphViewService(graph, personRepo);

        // --- Cấu hình Jackson ---
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // --- Khởi tạo Javalin ---
        Javalin app = Javalin.create(cfg -> {
            // Phục vụ file tĩnh từ classpath /public
            cfg.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
            // Jackson với hỗ trợ Java 8 date
            cfg.jsonMapper(new JavalinJackson(mapper, true));
            cfg.bundledPlugins.enableDevLogging();
        });

        // --- Đăng ký Routes ---
        Routes routes = new Routes(personRepo, kinshipService, marriageCheckService,
                dataQualityService, graphViewService, neo4jClient);
        routes.register(app);

        // --- Đóng kết nối khi tắt ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Đang dừng ứng dụng...");
            app.stop();
            neo4jClient.close();
        }));

        // --- Khởi động server ---
        int port = config.getServerPort();
        app.start(port);
        log.info("Ứng dụng đang chạy tại http://localhost:{}", port);
        log.info("Mở trình duyệt: http://localhost:{}", port);
    }

    /**
     * Nạp toàn bộ đồ thị gia phả vào FamilyGraph trong bộ nhớ.
     * Với ~20 nút, đây là cách tiếp cận hiệu quả và dễ test.
     */
    private static FamilyGraph loadFamilyGraph(PersonRepository repo) {
        FamilyGraph graph = new FamilyGraph();

        // Nạp tất cả người
        List<Person> persons = repo.findAll();
        for (Person p : persons) {
            graph.addPerson(p);
        }

        // Nạp cạnh cha mẹ → con
        for (var edge : repo.findAllParentEdges()) {
            graph.addParentEdge(edge.get("parent"), edge.get("child"));
        }

        // Nạp hôn nhân
        List<Marriage> marriages = repo.findAllMarriages();
        for (Marriage m : marriages) {
            graph.addMarriage(m);
        }

        return graph;
    }
}
