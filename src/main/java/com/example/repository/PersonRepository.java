package com.example.repository;

import com.example.db.Neo4jClient;
import com.example.model.Marriage;
import com.example.model.Person;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Tất cả truy vấn Cypher liên quan đến Person đều nằm ở đây.
 * Luôn dùng tham số, không nối chuỗi Cypher.
 */
public class PersonRepository {

    private static final Logger log = LoggerFactory.getLogger(PersonRepository.class);

    private final Neo4jClient client;

    public PersonRepository(Neo4jClient client) {
        this.client = client;
    }

    // =========================================================
    // Chuyển đổi Record → Person
    // =========================================================

    public static Person mapToPerson(Record record, String alias) {
        Value v = record.get(alias);
        if (v == null || v.isNull()) return null;
        return mapNodeToPerson(v);
    }

    public static Person mapNodeToPerson(Value node) {
        Person p = new Person();
        p.setPersonId(node.get("personId").asString(null));
        p.setFullName(node.get("fullName").asString(null));
        p.setGender(node.get("gender").asString(null));
        p.setOccupation(node.get("occupation").asString(null));

        Value dob = node.get("dateOfBirth");
        if (!dob.isNull()) p.setDateOfBirth(dob.asLocalDate());

        Value dod = node.get("dateOfDeath");
        if (!dod.isNull()) p.setDateOfDeath(dod.asLocalDate());

        return p;
    }

    // =========================================================
    // Danh sách / Chi tiết
    // =========================================================

    /** Lấy tất cả 20 người */
    public List<Person> findAll() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Person) RETURN p ORDER BY p.dateOfBirth");
                List<Person> list = new ArrayList<>();
                while (result.hasNext()) {
                    list.add(mapNodeToPerson(result.next().get("p")));
                }
                return list;
            });
        }
    }

    /** Tìm người theo ID */
    public Optional<Person> findById(String personId) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Person {personId: $id}) RETURN p",
                    Map.of("id", personId));
                if (result.hasNext()) {
                    return Optional.of(mapNodeToPerson(result.next().get("p")));
                }
                return Optional.empty();
            });
        }
    }

    /** Lấy cha mẹ của một người */
    public List<Person> findParents(String personId) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (parent:Person)-[:BIOLOGICAL_PARENT_OF]->(child:Person {personId: $id}) RETURN parent",
                    Map.of("id", personId));
                List<Person> list = new ArrayList<>();
                while (result.hasNext()) list.add(mapNodeToPerson(result.next().get("parent")));
                return list;
            });
        }
    }

    /** Lấy con của một người */
    public List<Person> findChildren(String personId) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (parent:Person {personId: $id})-[:BIOLOGICAL_PARENT_OF]->(child:Person) RETURN child",
                    Map.of("id", personId));
                List<Person> list = new ArrayList<>();
                while (result.hasNext()) list.add(mapNodeToPerson(result.next().get("child")));
                return list;
            });
        }
    }

    /** Lấy anh chị em ruột */
    public List<Person> findSiblings(String personId) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (parent:Person)-[:BIOLOGICAL_PARENT_OF]->(me:Person {personId: $id})
                    MATCH (parent)-[:BIOLOGICAL_PARENT_OF]->(sibling:Person)
                    WHERE sibling.personId <> $id
                    RETURN DISTINCT sibling
                    """, Map.of("id", personId));
                List<Person> list = new ArrayList<>();
                while (result.hasNext()) list.add(mapNodeToPerson(result.next().get("sibling")));
                return list;
            });
        }
    }

    // =========================================================
    // Hôn nhân
    // =========================================================

    /** Lấy tất cả hôn nhân */
    public List<Marriage> findAllMarriages() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (a:Person)-[m:MARRIED_TO]->(b:Person) RETURN a.personId AS a, b.personId AS b, m");
                List<Marriage> list = new ArrayList<>();
                while (result.hasNext()) {
                    var rec = result.next();
                    Marriage m = mapRelToMarriage(rec.get("a").asString(), rec.get("b").asString(), rec.get("m"));
                    list.add(m);
                }
                return list;
            });
        }
    }

    /** Lấy hôn nhân của một người */
    public List<Marriage> findMarriagesByPerson(String personId) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (a:Person)-[m:MARRIED_TO]-(b:Person)
                    WHERE a.personId = $id OR b.personId = $id
                    RETURN a.personId AS a, b.personId AS b, m
                    """, Map.of("id", personId));
                List<Marriage> list = new ArrayList<>();
                while (result.hasNext()) {
                    var rec = result.next();
                    list.add(mapRelToMarriage(rec.get("a").asString(), rec.get("b").asString(), rec.get("m")));
                }
                return list;
            });
        }
    }

    /** Tìm hôn nhân trực tiếp giữa 2 người */
    public Optional<Marriage> findDirectMarriage(String idA, String idB) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (a:Person {personId: $a})-[m:MARRIED_TO]-(b:Person {personId: $b})
                    RETURN a.personId AS a, b.personId AS b, m
                    """, Map.of("a", idA, "b", idB));
                if (result.hasNext()) {
                    var rec = result.next();
                    return Optional.of(mapRelToMarriage(rec.get("a").asString(), rec.get("b").asString(), rec.get("m")));
                }
                return Optional.empty();
            });
        }
    }

    private Marriage mapRelToMarriage(String a, String b, Value m) {
        Marriage mar = new Marriage();
        mar.setPersonId1(a);
        mar.setPersonId2(b);
        Value sd = m.get("startDate");
        if (!sd.isNull()) mar.setStartDate(sd.asLocalDate());
        Value ed = m.get("endDate");
        if (!ed.isNull()) mar.setEndDate(ed.asLocalDate());
        Value er = m.get("endReason");
        if (!er.isNull()) mar.setEndReason(er.asString());
        return mar;
    }

    // =========================================================
    // Tổ tiên chung (dùng cho KinshipService và MarriageCheckService)
    // =========================================================

    /**
     * Tìm tất cả tổ tiên chung của A và B bằng Cypher.
     * Trả về danh sách: {ancestorId, d1, d2, pathA, pathB}
     */
    public List<Map<String, Object>> findCommonAncestors(String idA, String idB) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (a:Person {personId: $a}), (b:Person {personId: $b})
                    MATCH pa = (anc:Person)-[:BIOLOGICAL_PARENT_OF*0..10]->(a)
                    MATCH pb = (anc)-[:BIOLOGICAL_PARENT_OF*0..10]->(b)
                    RETURN anc.personId AS ancestorId,
                           length(pa) AS d1,
                           length(pb) AS d2,
                           [n IN nodes(pa) | n.personId] AS pathA,
                           [n IN nodes(pb) | n.personId] AS pathB
                    ORDER BY d1 + d2
                    """, Map.of("a", idA, "b", idB));

                List<Map<String, Object>> list = new ArrayList<>();
                while (result.hasNext()) {
                    var rec = result.next();
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("ancestorId", rec.get("ancestorId").asString());
                    entry.put("d1", rec.get("d1").asInt());
                    entry.put("d2", rec.get("d2").asInt());
                    entry.put("pathA", rec.get("pathA").asList(Value::asString));
                    entry.put("pathB", rec.get("pathB").asList(Value::asString));
                    list.add(entry);
                }
                return list;
            });
        }
    }

    /**
     * Tìm đường đi ngắn nhất giữa A và B (dùng để tô sáng đồ thị).
     */
    public Map<String, Object> findShortestPath(String idA, String idB) {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (a:Person {personId: $a}), (b:Person {personId: $b})
                    MATCH p = shortestPath((a)-[:BIOLOGICAL_PARENT_OF|MARRIED_TO*..12]-(b))
                    RETURN [n IN nodes(p) | n.personId] AS nodeIds,
                           [r IN relationships(p) | {
                               from: startNode(r).personId,
                               to: endNode(r).personId,
                               type: type(r)
                           }] AS edges
                    """, Map.of("a", idA, "b", idB));

                if (result.hasNext()) {
                    var rec = result.next();
                    Map<String, Object> path = new HashMap<>();
                    path.put("nodes", rec.get("nodeIds").asList(Value::asString));
                    List<Map<String, String>> edges = new ArrayList<>();
                    for (Value e : rec.get("edges").values()) {
                        Map<String, String> edge = new HashMap<>();
                        edge.put("from", e.get("from").asString());
                        edge.put("to", e.get("to").asString());
                        edge.put("type", e.get("type").asString());
                        edges.add(edge);
                    }
                    path.put("edges", edges);
                    return path;
                }
                return Map.of("nodes", List.of(), "edges", List.of());
            });
        } catch (Exception e) {
            log.warn("Không tìm thấy đường đi giữa {} và {}: {}", idA, idB, e.getMessage());
            return Map.of("nodes", List.of(), "edges", List.of());
        }
    }

    // =========================================================
    // Lấy toàn bộ đồ thị cho FamilyGraph và vis-network
    // =========================================================

    /** Lấy tất cả cạnh BIOLOGICAL_PARENT_OF: {parent, child} */
    public List<Map<String, String>> findAllParentEdges() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Person)-[:BIOLOGICAL_PARENT_OF]->(c:Person) RETURN p.personId AS parent, c.personId AS child");
                List<Map<String, String>> list = new ArrayList<>();
                while (result.hasNext()) {
                    var rec = result.next();
                    list.add(Map.of("parent", rec.get("parent").asString(), "child", rec.get("child").asString()));
                }
                return list;
            });
        }
    }

    // =========================================================
    // Kiểm tra chất lượng dữ liệu (Q1–Q11)
    // =========================================================

    /** Q1: Thiếu fullName, dateOfBirth hoặc gender không hợp lệ */
    public List<String> q1MissingRequiredFields() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (p:Person)
                    WHERE p.fullName IS NULL OR p.dateOfBirth IS NULL
                       OR NOT p.gender IN ['Nam', 'Nữ']
                    RETURN p.personId AS personId
                    """);
                List<String> list = new ArrayList<>();
                while (result.hasNext()) list.add(result.next().get("personId").asString());
                return list;
            });
        }
    }

    /** Q2: Người có > 2 cha mẹ ruột */
    public List<String> q2MoreThanTwoParents() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (c:Person)<-[:BIOLOGICAL_PARENT_OF]-(p:Person)
                    WITH c, collect(p.personId) AS parents WHERE size(parents) > 2
                    RETURN c.personId AS childId
                    """);
                List<String> list = new ArrayList<>();
                while (result.hasNext()) list.add(result.next().get("childId").asString());
                return list;
            });
        }
    }

    /** Q3: Hai cha mẹ cùng giới */
    public List<String> q3SameGenderParents() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (p1:Person)-[:BIOLOGICAL_PARENT_OF]->(c:Person)<-[:BIOLOGICAL_PARENT_OF]-(p2:Person)
                    WHERE p1.personId < p2.personId AND p1.gender = p2.gender
                    RETURN c.personId AS childId, p1.personId AS parent1, p2.personId AS parent2
                    """);
                List<String> list = new ArrayList<>();
                while (result.hasNext()) {
                    var r = result.next();
                    list.add(r.get("childId").asString() + " (cha/mẹ: " + r.get("parent1").asString() + ", " + r.get("parent2").asString() + ")");
                }
                return list;
            });
        }
    }

    /** Q4: Có vòng lặp tổ tiên */
    public List<String> q4AncestorCycle() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (x:Person)-[:BIOLOGICAL_PARENT_OF*1..]->(x) RETURN DISTINCT x.personId AS personId");
                List<String> list = new ArrayList<>();
                while (result.hasNext()) list.add(result.next().get("personId").asString());
                return list;
            });
        }
    }

    /** Q5: Chênh tuổi cha/mẹ-con < 15 năm (WARNING) hoặc cha/mẹ sinh sau con (ERROR) */
    public List<Map<String, Object>> q5AgeDifference() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (p:Person)-[:BIOLOGICAL_PARENT_OF]->(c:Person)
                    WITH p, c, duration.between(p.dateOfBirth, c.dateOfBirth).years AS gap
                    WHERE gap < 15
                    RETURN p.personId AS parentId, c.personId AS childId, gap
                    """);
                List<Map<String, Object>> list = new ArrayList<>();
                while (result.hasNext()) {
                    var r = result.next();
                    Map<String, Object> m = new HashMap<>();
                    m.put("parentId", r.get("parentId").asString());
                    m.put("childId", r.get("childId").asString());
                    m.put("gap", r.get("gap").asInt());
                    list.add(m);
                }
                return list;
            });
        }
    }

    /** Q6: Hôn nhân tự cưới mình, endDate < startDate, kết hôn trước khi sinh */
    public List<String> q6MarriageErrors() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (a:Person)-[m:MARRIED_TO]->(b:Person)
                    WHERE a.personId = b.personId
                       OR (m.endDate IS NOT NULL AND m.endDate < m.startDate)
                       OR m.startDate < a.dateOfBirth
                       OR m.startDate < b.dateOfBirth
                    RETURN a.personId AS a, b.personId AS b
                    """);
                List<String> list = new ArrayList<>();
                while (result.hasNext()) {
                    var r = result.next();
                    list.add(r.get("a").asString() + "-" + r.get("b").asString());
                }
                return list;
            });
        }
    }

    /** Q7: endReason='DEATH' không khớp dateOfDeath, hoặc còn hiệu lực nhưng một người đã mất */
    public List<String> q7DeathConsistency() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (a:Person)-[m:MARRIED_TO]->(b:Person)
                    WHERE (m.endReason = 'DEATH' AND NOT (a.dateOfDeath = m.endDate OR b.dateOfDeath = m.endDate))
                       OR (m.endDate IS NULL AND (a.dateOfDeath IS NOT NULL OR b.dateOfDeath IS NOT NULL))
                    RETURN a.personId AS a, b.personId AS b
                    """);
                List<String> list = new ArrayList<>();
                while (result.hasNext()) {
                    var r = result.next();
                    list.add(r.get("a").asString() + "-" + r.get("b").asString());
                }
                return list;
            });
        }
    }

    /** Q8: Một người có 2 hôn nhân chồng lấn thời gian */
    public List<String> q8OverlappingMarriages() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (p:Person)-[m1:MARRIED_TO]-(:Person), (p)-[m2:MARRIED_TO]-(:Person)
                    WHERE elementId(m1) < elementId(m2)
                      AND m1.startDate <= coalesce(m2.endDate, date('9999-12-31'))
                      AND m2.startDate <= coalesce(m1.endDate, date('9999-12-31'))
                    RETURN p.personId AS personId
                    """);
                List<String> list = new ArrayList<>();
                while (result.hasNext()) list.add(result.next().get("personId").asString());
                return list;
            });
        }
    }

    /** Q10: Người không có quan hệ nào; người kết hôn vào không có thông tin cha mẹ */
    public List<Map<String, Object>> q10IsolatedOrNoParents() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (p:Person)
                    OPTIONAL MATCH (p)-[r1:BIOLOGICAL_PARENT_OF]-()
                    OPTIONAL MATCH ()-[r2:BIOLOGICAL_PARENT_OF]->(p)
                    OPTIONAL MATCH (p)-[r3:MARRIED_TO]-()
                    WITH p, r1, r2, r3
                    WHERE r1 IS NULL AND r2 IS NULL AND r3 IS NULL
                    RETURN p.personId AS personId, 'isolated' AS type
                    UNION
                    MATCH (p:Person)
                    OPTIONAL MATCH ()-[:BIOLOGICAL_PARENT_OF]->(p)
                    WITH p, count(*) AS parentCount
                    WHERE parentCount = 0
                    MATCH (p)-[:MARRIED_TO]-()
                    RETURN p.personId AS personId, 'no_parents_but_married' AS type
                    """);
                List<Map<String, Object>> list = new ArrayList<>();
                while (result.hasNext()) {
                    var r = result.next();
                    list.add(Map.of("personId", r.get("personId").asString(), "type", r.get("type").asString()));
                }
                return list;
            });
        }
    }

    /** Q11: Con sinh trước ngày kết hôn của cha mẹ */
    public List<String> q11ChildBeforeMarriage() {
        try (var session = client.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("""
                    MATCH (a:Person)-[m:MARRIED_TO]-(b:Person)
                    MATCH (a)-[:BIOLOGICAL_PARENT_OF]->(c:Person)<-[:BIOLOGICAL_PARENT_OF]-(b)
                    WHERE c.dateOfBirth < m.startDate
                    RETURN c.personId AS childId, a.personId AS parent1, b.personId AS parent2
                    """);
                List<String> list = new ArrayList<>();
                while (result.hasNext()) {
                    var r = result.next();
                    list.add(r.get("childId").asString() + " (cha: " + r.get("parent1").asString() + ", mẹ: " + r.get("parent2").asString() + ")");
                }
                return list;
            });
        }
    }
}
