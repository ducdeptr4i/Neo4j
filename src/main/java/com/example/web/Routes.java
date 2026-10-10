package com.example.web;

import com.example.model.*;
import com.example.repository.PersonRepository;
import com.example.service.*;
import com.example.db.Neo4jClient;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Đăng ký tất cả API routes và xử lý lỗi JSON thống nhất.
 */
public class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    private final PersonRepository personRepo;
    private final KinshipService kinshipService;
    private final MarriageCheckService marriageCheckService;
    private final DataQualityService dataQualityService;
    private final GraphViewService graphViewService;
    private final Neo4jClient neo4jClient;

    public Routes(PersonRepository personRepo,
                  KinshipService kinshipService,
                  MarriageCheckService marriageCheckService,
                  DataQualityService dataQualityService,
                  GraphViewService graphViewService,
                  Neo4jClient neo4jClient) {
        this.personRepo = personRepo;
        this.kinshipService = kinshipService;
        this.marriageCheckService = marriageCheckService;
        this.dataQualityService = dataQualityService;
        this.graphViewService = graphViewService;
        this.neo4jClient = neo4jClient;
    }

    public void register(Javalin app) {
        // Health check
        app.get("/api/health", this::health);

        // Persons
        app.get("/api/persons", this::listPersons);
        app.get("/api/persons/{id}", this::getPerson);

        // Graph
        app.get("/api/graph", this::getGraph);

        // Kinship
        app.get("/api/kinship", this::kinship);

        // Marriage check
        app.get("/api/marriage-check", this::marriageCheck);

        // Data quality
        app.get("/api/quality", this::quality);

        // Error handler
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("PERSON_NOT_FOUND:")) {
                ctx.status(404).json(Map.of("error", "PERSON_NOT_FOUND",
                        "message", "Không tìm thấy người: " + msg.substring(17)));
            } else if ("SAME_PERSON".equals(msg)) {
                ctx.status(400).json(Map.of("error", "SAME_PERSON",
                        "message", "A và B phải là 2 người khác nhau"));
            } else {
                ctx.status(400).json(Map.of("error", "BAD_REQUEST", "message", msg));
            }
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Lỗi server: {}", e.getMessage(), e);
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR",
                    "message", "Lỗi hệ thống. Vui lòng kiểm tra log."));
        });
    }

    // =========================================================
    // Handler: health
    // =========================================================
    private void health(Context ctx) {
        boolean connected = neo4jClient.isConnected();
        ctx.json(Map.of(
                "status", connected ? "OK" : "DEGRADED",
                "neo4j", connected ? "connected" : "disconnected",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    // =========================================================
    // Handler: listPersons
    // =========================================================
    private void listPersons(Context ctx) {
        var persons = personRepo.findAll();
        var today = java.time.LocalDate.now();
        var result = persons.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("personId", p.getPersonId());
            m.put("fullName", p.getFullName());
            m.put("gender", p.getGender());
            m.put("dateOfBirth", p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : null);
            m.put("dateOfDeath", p.getDateOfDeath() != null ? p.getDateOfDeath().toString() : null);
            m.put("occupation", p.getOccupation());
            m.put("deceased", p.isDeceased());
            if (p.getDateOfBirth() != null) {
                int age = java.time.Period.between(p.getDateOfBirth(),
                        p.isDeceased() ? p.getDateOfDeath() : today).getYears();
                m.put("age", age);
            }
            return m;
        }).collect(Collectors.toList());
        ctx.json(result);
    }

    // =========================================================
    // Handler: getPerson
    // =========================================================
    private void getPerson(Context ctx) {
        String id = ctx.pathParam("id");
        var person = personRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PERSON_NOT_FOUND:" + id));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("personId", person.getPersonId());
        result.put("fullName", person.getFullName());
        result.put("gender", person.getGender());
        result.put("dateOfBirth", person.getDateOfBirth() != null ? person.getDateOfBirth().toString() : null);
        result.put("dateOfDeath", person.getDateOfDeath() != null ? person.getDateOfDeath().toString() : null);
        result.put("occupation", person.getOccupation());
        result.put("deceased", person.isDeceased());

        // Cha mẹ
        var parents = personRepo.findParents(id).stream().map(p -> Map.of(
                "personId", p.getPersonId(), "fullName", p.getFullName(), "gender", p.getGender()
        )).collect(Collectors.toList());
        result.put("parents", parents);

        // Con
        var children = personRepo.findChildren(id).stream().map(p -> Map.of(
                "personId", p.getPersonId(), "fullName", p.getFullName(), "gender", p.getGender()
        )).collect(Collectors.toList());
        result.put("children", children);

        // Anh chị em
        var siblings = personRepo.findSiblings(id).stream().map(p -> Map.of(
                "personId", p.getPersonId(), "fullName", p.getFullName(), "gender", p.getGender()
        )).collect(Collectors.toList());
        result.put("siblings", siblings);

        // Vợ/chồng
        var marriages = personRepo.findMarriagesByPerson(id).stream().map(m -> {
            Map<String, Object> mm = new LinkedHashMap<>();
            String spouseId = m.getSpouseId(id);
            mm.put("personId", spouseId);
            personRepo.findById(spouseId).ifPresent(sp -> {
                mm.put("fullName", sp.getFullName());
                mm.put("gender", sp.getGender());
            });
            mm.put("startDate", m.getStartDate() != null ? m.getStartDate().toString() : null);
            mm.put("endDate", m.getEndDate() != null ? m.getEndDate().toString() : null);
            mm.put("endReason", m.getEndReason());
            mm.put("status", m.isActive() ? "ACTIVE" : (m.isEndedByDeath() ? "DEATH" : "DIVORCE"));
            return mm;
        }).collect(Collectors.toList());
        result.put("marriages", marriages);

        ctx.json(result);
    }

    // =========================================================
    // Handler: graph
    // =========================================================
    private void getGraph(Context ctx) {
        boolean includeEnded = !"false".equalsIgnoreCase(ctx.queryParam("includeEnded"));
        ctx.json(graphViewService.getGraphData(includeEnded));
    }

    // =========================================================
    // Handler: kinship
    // =========================================================
    private void kinship(Context ctx) {
        String idA = ctx.queryParam("a");
        String idB = ctx.queryParam("b");
        if (idA == null || idB == null) {
            ctx.status(400).json(Map.of("error", "MISSING_PARAMS", "message", "Cần tham số a và b"));
            return;
        }
        boolean includeDivorced = "true".equalsIgnoreCase(ctx.queryParam("includeDivorced"));
        String region = Optional.ofNullable(ctx.queryParam("region")).orElse("COMMON");

        KinshipResult result = kinshipService.findKinship(idA, idB, includeDivorced, region);
        ctx.json(result);
    }

    // =========================================================
    // Handler: marriageCheck
    // =========================================================
    private void marriageCheck(Context ctx) {
        String idA = ctx.queryParam("a");
        String idB = ctx.queryParam("b");
        if (idA == null || idB == null) {
            ctx.status(400).json(Map.of("error", "MISSING_PARAMS", "message", "Cần tham số a và b"));
            return;
        }
        MarriageCheckResult result = marriageCheckService.check(idA, idB);
        ctx.json(result);
    }

    // =========================================================
    // Handler: quality
    // =========================================================
    private void quality(Context ctx) {
        ctx.json(dataQualityService.runAllChecks());
    }
}
