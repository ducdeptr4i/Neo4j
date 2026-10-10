package com.example;

import com.example.engine.BloodRelationFinder;
import com.example.engine.FamilyGraph;
import com.example.model.Marriage;
import com.example.model.Person;
import com.example.repository.PersonRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Xây dựng FamilyGraph 20 người trong bộ nhớ để dùng trong unit test
 * (không cần kết nối Neo4j).
 */
public class TestFamilyGraphBuilder {

    /**
     * Tạo PersonRepository giả (mock) sử dụng BloodRelationFinder trong bộ nhớ
     * thay vì Cypher.
     */
    public static PersonRepository buildInMemoryRepo(FamilyGraph graph) {
        BloodRelationFinder finder = new BloodRelationFinder(graph);
        // Trả về anonymous class dùng BloodRelationFinder
        return new PersonRepository(null) {
            @Override
            public Optional<Person> findById(String personId) {
                return Optional.ofNullable(graph.getPerson(personId));
            }

            @Override
            public List<Map<String, Object>> findCommonAncestors(String idA, String idB) {
                return finder.findCommonAncestors(idA, idB);
            }

            @Override
            public Optional<Marriage> findDirectMarriage(String idA, String idB) {
                return graph.getDirectMarriage(idA, idB);
            }

            @Override
            public List<Marriage> findAllMarriages() {
                // Trả về tất cả marriages từ graph
                java.util.Set<String> seen = new java.util.HashSet<>();
                java.util.List<Marriage> all = new java.util.ArrayList<>();
                for (Person p : graph.getAllPersons()) {
                    for (Marriage m : graph.getMarriages(p.getPersonId())) {
                        String key = List.of(m.getPersonId1(), m.getPersonId2()).stream()
                                .sorted().reduce("", String::concat);
                        if (seen.add(key)) all.add(m);
                    }
                }
                return all;
            }

            @Override
            public Map<String, Object> findShortestPath(String idA, String idB) {
                return Map.of("nodes", List.of(), "edges", List.of());
            }

            @Override
            public java.util.List<Map<String, String>> findAllParentEdges() {
                return List.of();
            }

            // Các phương thức quality check không dùng trong unit test
            @Override public List<String> q1MissingRequiredFields() { return List.of(); }
            @Override public List<String> q2MoreThanTwoParents() { return List.of(); }
            @Override public List<String> q3SameGenderParents() { return List.of(); }
            @Override public List<String> q4AncestorCycle() { return List.of(); }
            @Override public List<Map<String, Object>> q5AgeDifference() { return List.of(); }
            @Override public List<String> q6MarriageErrors() { return List.of(); }
            @Override public List<String> q7DeathConsistency() { return List.of(); }
            @Override public List<String> q8OverlappingMarriages() { return List.of(); }
            @Override public List<Map<String, Object>> q10IsolatedOrNoParents() { return List.of(); }
            @Override public List<String> q11ChildBeforeMarriage() { return List.of(); }
        };
    }

    /** Tạo FamilyGraph 20 người đầy đủ */
    public static FamilyGraph build() {
        FamilyGraph g = new FamilyGraph();

        // Nạp 20 người
        g.addPerson(p("P001", "Nguyễn Văn Hòa",   "Nam", "1938-02-14", "Nông dân",              "2018-11-03"));
        g.addPerson(p("P002", "Lê Thị Mai",          "Nữ",  "1941-07-21", "Giáo viên nghỉ hưu",   null));
        g.addPerson(p("P003", "Nguyễn Văn Thành",   "Nam", "1963-05-10", "Kỹ sư xây dựng",        null));
        g.addPerson(p("P004", "Phạm Thị Hạnh",       "Nữ",  "1965-09-22", "Kế toán",               null));
        g.addPerson(p("P005", "Nguyễn Văn Bình",    "Nam", "1974-12-03", "Bác sĩ",                null));
        g.addPerson(p("P006", "Trần Thị Thu",        "Nữ",  "1975-04-18", "Dược sĩ",               null));
        g.addPerson(p("P007", "Nguyễn Thị Lan",     "Nữ",  "1971-06-16", "Giáo viên",             null));
        g.addPerson(p("P008", "Võ Văn Quang",       "Nam", "1969-01-30", "Kỹ thuật viên",         "2022-06-30"));
        g.addPerson(p("P009", "Nguyễn Thị Hương",   "Nữ",  "1977-08-09", "Điều dưỡng",            null));
        g.addPerson(p("P010", "Đặng Văn Phúc",      "Nam", "1975-01-30", "Kiến trúc sư",          null));
        g.addPerson(p("P011", "Nguyễn Minh Đức",    "Nam", "1988-03-15", "Kỹ sư phần mềm",        null));
        g.addPerson(p("P012", "Nguyễn Ngọc Anh",    "Nữ",  "1992-10-02", "Giảng viên",            null));
        g.addPerson(p("P013", "Nguyễn Hoàng Nam",   "Nam", "1999-07-11", "Nhân viên ngân hàng",   null));
        g.addPerson(p("P014", "Nguyễn Thu Hà",      "Nữ",  "2003-02-25", "Thiết kế đồ họa",       null));
        g.addPerson(p("P015", "Võ Minh Khang",      "Nam", "1996-06-05", "Nhân viên marketing",   null));
        g.addPerson(p("P016", "Đặng Thảo Vy",       "Nữ",  "2001-09-17", "Phân tích dữ liệu",     null));
        g.addPerson(p("P017", "Đỗ Thị Ngọc",        "Nữ",  "1990-12-08", "Dược sĩ",               null));
        g.addPerson(p("P018", "Lê Văn Sơn",         "Nam", "1990-01-19", "Luật sư",               null));
        g.addPerson(p("P019", "Nguyễn Gia Bảo",     "Nam", "2015-04-12", "Học sinh",              null));
        g.addPerson(p("P020", "Lê Minh Châu",       "Nữ",  "2018-09-01", "Học sinh",              null));

        // Quan hệ cha mẹ → con
        // P001+P002 → P003,P007,P005,P009
        pe(g, "P001","P003"); pe(g,"P002","P003");
        pe(g, "P001","P007"); pe(g,"P002","P007");
        pe(g, "P001","P005"); pe(g,"P002","P005");
        pe(g, "P001","P009"); pe(g,"P002","P009");
        // P003+P004 → P011,P012
        pe(g,"P003","P011"); pe(g,"P004","P011");
        pe(g,"P003","P012"); pe(g,"P004","P012");
        // P005+P006 → P013,P014
        pe(g,"P005","P013"); pe(g,"P006","P013");
        pe(g,"P005","P014"); pe(g,"P006","P014");
        // P007+P008 → P015
        pe(g,"P007","P015"); pe(g,"P008","P015");
        // P009+P010 → P016
        pe(g,"P009","P016"); pe(g,"P010","P016");
        // P011+P017 → P019
        pe(g,"P011","P019"); pe(g,"P017","P019");
        // P012+P018 → P020
        pe(g,"P012","P020"); pe(g,"P018","P020");

        // Hôn nhân
        g.addMarriage(m("P001","P002","1961-01-15","2018-11-03","DEATH"));
        g.addMarriage(m("P003","P004","1986-12-20",null,null));
        g.addMarriage(m("P005","P006","1997-03-16","2008-08-15","DIVORCE"));
        g.addMarriage(m("P007","P008","1994-01-25","2022-06-30","DEATH"));
        g.addMarriage(m("P009","P010","1999-04-18",null,null));
        g.addMarriage(m("P011","P017","2012-11-10",null,null));
        g.addMarriage(m("P012","P018","2015-05-24",null,null));

        return g;
    }

    private static Person p(String id, String name, String gender, String dob, String job, String dod) {
        return new Person(id, name, gender, LocalDate.parse(dob), job,
                dod != null ? LocalDate.parse(dod) : null);
    }

    private static void pe(FamilyGraph g, String parent, String child) {
        g.addParentEdge(parent, child);
    }

    private static Marriage m(String p1, String p2, String start, String end, String reason) {
        return new Marriage(p1, p2,
                LocalDate.parse(start),
                end != null ? LocalDate.parse(end) : null,
                reason);
    }
}
