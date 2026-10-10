package com.example.service;

import com.example.engine.FamilyGraph;
import com.example.engine.KinshipTermResolver;
import com.example.model.*;
import com.example.model.KinshipResult.*;
import com.example.repository.PersonRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Chức năng 1: Tìm cách xưng hô giữa 2 người.
 * Ưu tiên: vợ/chồng → huyết thống → thông gia → không có quan hệ.
 */
public class KinshipService {

    private final PersonRepository repo;
    private final FamilyGraph graph;

    public KinshipService(PersonRepository repo, FamilyGraph graph) {
        this.repo = repo;
        this.graph = graph;
    }

    /**
     * Tìm cách xưng hô giữa A và B.
     *
     * @param idA            ID của A
     * @param idB            ID của B
     * @param includeDivorced tính cả hôn nhân đã ly hôn không
     * @param region          "COMMON" hoặc "NORTH"
     */
    public KinshipResult findKinship(String idA, String idB,
                                     boolean includeDivorced, String region) {
        // Kiểm tra tồn tại
        Person personA = graph.getPerson(idA);
        Person personB = graph.getPerson(idB);
        if (personA == null) throw new IllegalArgumentException("PERSON_NOT_FOUND:" + idA);
        if (personB == null) throw new IllegalArgumentException("PERSON_NOT_FOUND:" + idB);
        if (idA.equals(idB)) throw new IllegalArgumentException("SAME_PERSON");

        KinshipResult result = new KinshipResult();
        result.setPersonA(personA);
        result.setPersonB(personB);
        List<String> notes = new ArrayList<>();

        // -------------------------------------------------------
        // 1. Kiểm tra vợ/chồng trực tiếp
        // -------------------------------------------------------
        Optional<Marriage> directMarriage = graph.getDirectMarriage(idA, idB);
        if (directMarriage.isPresent()) {
            return buildSpouseResult(personA, personB, directMarriage.get(), result, notes);
        }

        // -------------------------------------------------------
        // 2. Huyết thống: dùng Cypher (Neo4j) làm nguồn chính
        // -------------------------------------------------------
        List<Map<String, Object>> commonAncestors = repo.findCommonAncestors(idA, idB);
        if (!commonAncestors.isEmpty()) {
            return buildBloodResult(personA, personB, commonAncestors, result, notes, region);
        }

        // -------------------------------------------------------
        // 3. Thông gia (qua hôn nhân)
        // -------------------------------------------------------
        KinshipResult inLawResult = tryInLaw(personA, personB, idA, idB, includeDivorced, region, notes);
        if (inLawResult != null) return inLawResult;

        // -------------------------------------------------------
        // 4. Không có quan hệ
        // -------------------------------------------------------
        result.setCategory("NONE");
        result.setSummary("Không tìm thấy quan hệ giữa " + personA.getFullName() + " và " + personB.getFullName() + " trong dữ liệu hiện có");
        result.setExplanation(List.of("Không có đường kết nối qua huyết thống hoặc hôn nhân giữa 2 người trong dữ liệu hiện có (20 người)."));
        result.setCommonAncestors(List.of());
        result.setPath(buildEmptyPath());
        notes.add("Giới hạn dữ liệu: chỉ có 20 người trong hệ thống. Một số người kết hôn vào (P004, P006, P008, P010, P017, P018) không có thông tin cha mẹ/anh chị em trong DB.");
        result.setNotes(notes);
        return result;
    }

    // -----------------------------------------------------------
    // Xây dựng kết quả vợ/chồng
    // -----------------------------------------------------------
    private KinshipResult buildSpouseResult(Person personA, Person personB,
                                            Marriage m, KinshipResult result,
                                            List<String> notes) {
        result.setCategory("SPOUSE");
        result.setCommonAncestors(List.of());
        result.setGenerationGap(0);

        String status;
        if (m.isActive()) {
            status = "Đang là vợ chồng (từ " + m.getStartDate() + ")";
        } else if (m.isEndedByDeath()) {
            status = "Góa (một người đã mất, hôn nhân kết thúc " + m.getEndDate() + ")";
        } else {
            status = "Đã ly hôn (" + m.getEndDate() + ")";
            notes.add("Hôn nhân P005–P006 đã ly hôn ngày " + m.getEndDate() + ".");
        }

        String aCallB, bCallA;
        if (personA.isMale()) {
            aCallB = "vợ"; bCallA = "chồng";
        } else {
            aCallB = "chồng"; bCallA = "vợ";
        }

        result.setSummary(status);
        result.setAToB(new Direction(aCallB, "Vợ/chồng — " + status, bCallA,
                personA.getFullName() + " gọi " + personB.getFullName() + " là \"" + aCallB + "\""));
        result.setBToA(new Direction(bCallA, "Vợ/chồng — " + status, aCallB,
                personB.getFullName() + " gọi " + personA.getFullName() + " là \"" + bCallA + "\""));
        result.setExplanation(List.of(
                "Bước 1: Kiểm tra cạnh MARRIED_TO trực tiếp giữa " + personA.getFullName() + " và " + personB.getFullName() + ".",
                "Bước 2: Phát hiện hôn nhân — " + status + "."
        ));

        var path = repo.findShortestPath(personA.getPersonId(), personB.getPersonId());
        result.setPath(mapPath(path));
        result.setNotes(notes);
        return result;
    }

    // -----------------------------------------------------------
    // Xây dựng kết quả huyết thống
    // -----------------------------------------------------------
    private KinshipResult buildBloodResult(Person personA, Person personB,
                                           List<Map<String, Object>> commonAncestors,
                                           KinshipResult result, List<String> notes,
                                           String region) {
        result.setCategory("BLOOD");

        Map<String, Object> best = commonAncestors.get(0);
        int d1 = (int) best.get("d1");
        int d2 = (int) best.get("d2");
        @SuppressWarnings("unchecked")
        List<String> pathA = (List<String>) best.get("pathA");
        @SuppressWarnings("unchecked")
        List<String> pathB = (List<String>) best.get("pathB");

        result.setGenerationGap(d1 - d2);

        List<CommonAncestor> caList = commonAncestors.stream()
                .map(m -> new CommonAncestor((String) m.get("ancestorId"), (int) m.get("d1"), (int) m.get("d2")))
                .collect(Collectors.toList());
        result.setCommonAncestors(caList);

        Direction aToB = KinshipTermResolver.resolve(personA, personB, d1, d2, pathA, pathB, graph, region);
        Direction bToA = KinshipTermResolver.resolve(personB, personA, d2, d1, pathB, pathA, graph, region);

        result.setAToB(aToB);
        result.setBToA(bToA);
        result.setSummary(personB.getFullName() + " là " + aToB.getLabel() + " của " + personA.getFullName());

        String ancestorName = graph.getPerson((String) best.get("ancestorId")) != null
                ? graph.getPerson((String) best.get("ancestorId")).getFullName()
                : (String) best.get("ancestorId");
        result.setExplanation(buildBloodExplanation(personA, personB, d1, d2, ancestorName, aToB));

        var path = repo.findShortestPath(personA.getPersonId(), personB.getPersonId());
        result.setPath(mapPath(path));
        result.setNotes(notes);
        return result;
    }

    // -----------------------------------------------------------
    // Thông gia (qua hôn nhân)
    // -----------------------------------------------------------
    private KinshipResult tryInLaw(Person personA, Person personB,
                                   String idA, String idB,
                                   boolean includeDivorced, String region,
                                   List<String> notes) {
        // Mẫu 1: Vợ/chồng của A (S) là họ hàng ruột của B
        for (String spouseId : graph.getEffectiveSpouses(idA, includeDivorced)) {
            Marriage mar = graph.getDirectMarriage(idA, spouseId).orElse(null);
            if (mar == null) continue;
            if (mar.isDivorced() && !includeDivorced) continue;

            List<Map<String, Object>> sToB = repo.findCommonAncestors(spouseId, idB);
            if (!sToB.isEmpty()) {
                Person spouse = graph.getPerson(spouseId);
                String[] inLawLabels = buildInLawLabel1(personA, spouse, personB, sToB, graph, region);
                if (inLawLabels != null) {
                    String aCallB = inLawLabels[0];
                    String bCallA = inLawLabels[1];
                    boolean isOld = mar.isDivorced();
                    String oldSuffix = isOld ? " (cũ)" : "";

                    KinshipResult res = new KinshipResult();
                    res.setPersonA(personA);
                    res.setPersonB(personB);
                    res.setCategory("IN_LAW");
                    res.setAToB(new Direction(aCallB + oldSuffix, aCallB + oldSuffix, bCallA + oldSuffix,
                            personA.getFullName() + " gọi " + personB.getFullName() + " là \"" + aCallB + oldSuffix + "\""));
                    res.setBToA(new Direction(bCallA + oldSuffix, bCallA + oldSuffix, aCallB + oldSuffix,
                            personB.getFullName() + " gọi " + personA.getFullName() + " là \"" + bCallA + oldSuffix + "\""));
                    res.setSummary("Quan hệ thông gia qua " + (spouse != null ? spouse.getFullName() : spouseId));
                    res.setCommonAncestors(List.of());
                    res.setExplanation(List.of(
                            "Bước 1: " + personA.getFullName() + " có hôn nhân với " + (spouse != null ? spouse.getFullName() : spouseId) + (isOld ? " (đã ly hôn)" : "") + ".",
                            "Bước 2: " + personB.getFullName() + " có quan hệ huyết thống với " + (spouse != null ? spouse.getFullName() : spouseId) + ".",
                            "Bước 3: Suy ra " + personA.getFullName() + " gọi " + personB.getFullName() + " là \"" + aCallB + oldSuffix + "\"; "
                                    + personB.getFullName() + " gọi " + personA.getFullName() + " là \"" + bCallA + oldSuffix + "\"."
                    ));
                    var path = repo.findShortestPath(idA, idB);
                    res.setPath(mapPath(path));
                    if (isOld) notes.add("Hôn nhân với người trung gian đã ly hôn nên tính là quan hệ cũ.");
                    res.setNotes(notes);
                    return res;
                }
            }
        }

        // Mẫu 2: B là vợ/chồng của họ hàng ruột R của A
        for (String spouseOfB : graph.getEffectiveSpouses(idB, includeDivorced)) {
            List<Map<String, Object>> rToA = repo.findCommonAncestors(spouseOfB, idA);
            if (!rToA.isEmpty()) {
                Marriage mar = graph.getDirectMarriage(idB, spouseOfB).orElse(null);
                if (mar == null) continue;
                if (mar.isDivorced() && !includeDivorced) continue;

                Person r = graph.getPerson(spouseOfB);
                Map<String, Object> bestR = rToA.get(0);
                int d1r = (int) bestR.get("d1"); // R lên LCA
                int d2r = (int) bestR.get("d2"); // A lên LCA
                @SuppressWarnings("unchecked")
                List<String> pathR = (List<String>) bestR.get("pathA");
                @SuppressWarnings("unchecked")
                List<String> pathA = (List<String>) bestR.get("pathB");

                Direction aToR = KinshipTermResolver.resolve(personA, r != null ? r : new Person(), d2r, d1r, pathA, pathR, graph, region);

                String[] inLawLabels = buildInLawLabel2(personA, r, personB, aToR.getCall());
                if (inLawLabels == null) continue;

                String callB = inLawLabels[0];
                String bCallA = inLawLabels[1];
                boolean divorceOld = mar.isDivorced();
                String oldSuffix = divorceOld ? " (cũ)" : "";

                KinshipResult res = new KinshipResult();
                res.setPersonA(personA);
                res.setPersonB(personB);
                res.setCategory("IN_LAW");
                res.setSummary("Quan hệ thông gia: " + personB.getFullName() + " là " + callB + oldSuffix + " của " + personA.getFullName());
                res.setAToB(new Direction(callB + oldSuffix, callB + oldSuffix, bCallA + oldSuffix,
                        personA.getFullName() + " gọi " + personB.getFullName() + " là \"" + callB + oldSuffix + "\""));
                res.setBToA(new Direction(bCallA + oldSuffix, bCallA + oldSuffix, callB + oldSuffix,
                        personB.getFullName() + " gọi " + personA.getFullName() + " là \"" + bCallA + oldSuffix + "\""));
                res.setCommonAncestors(List.of());
                res.setExplanation(List.of(
                        "Bước 1: " + personB.getFullName() + " là vợ/chồng của " + (r != null ? r.getFullName() : spouseOfB) + (divorceOld ? " (đã ly hôn)" : "") + ".",
                        "Bước 2: " + (r != null ? r.getFullName() : spouseOfB) + " là " + aToR.getLabel() + " của " + personA.getFullName() + ".",
                        "Bước 3: Vậy " + personA.getFullName() + " gọi " + personB.getFullName() + " là \"" + callB + oldSuffix + "\"; "
                                + personB.getFullName() + " gọi " + personA.getFullName() + " là \"" + bCallA + oldSuffix + "\"."
                ));
                var path = repo.findShortestPath(idA, idB);
                res.setPath(mapPath(path));
                if (divorceOld) notes.add("Hôn nhân của đối tượng đã ly hôn.");
                res.setNotes(notes);
                return res;
            }
        }

        // Mẫu 3: Sui gia / chị em dâu / anh em đồng hao
        return tryInLawPattern3(personA, personB, idA, idB, includeDivorced, notes);
    }

    private String[] buildInLawLabel1(Person personA, Person spouse, Person personB,
                                      List<Map<String, Object>> sToB,
                                      FamilyGraph graph, String region) {
        if (sToB.isEmpty() || spouse == null) return null;
        Map<String, Object> best = sToB.get(0);
        int d1 = (int) best.get("d1"); // spouse lên LCA
        int d2 = (int) best.get("d2"); // personB lên LCA

        boolean aIsFemale = personA.isFemale();

        // B là cha/mẹ của spouse (d2=0, d1=1)
        if (d2 == 0 && d1 == 1) {
            String aCallB;
            if (aIsFemale) {
                aCallB = personB.isMale() ? "bố chồng" : "mẹ chồng";
            } else {
                aCallB = personB.isMale() ? "bố vợ" : "mẹ vợ";
            }
            String bCallA = aIsFemale ? "con dâu" : "con rể";
            return new String[]{aCallB, bCallA};
        }

        // B và spouse là anh chị em ruột (d1=1, d2=1)
        if (d1 == 1 && d2 == 1) {
            boolean bIsOlder = personB.getDateOfBirth() != null && spouse.getDateOfBirth() != null
                    && personB.getDateOfBirth().isBefore(spouse.getDateOfBirth());
            String aCallB;
            String bCallA;
            if (aIsFemale) { // spouse là chồng
                if (personB.isMale()) {
                    aCallB = bIsOlder ? "anh chồng" : "em chồng (chú)";
                    bCallA = bIsOlder ? "em dâu" : "chị dâu";
                } else {
                    aCallB = bIsOlder ? "chị chồng" : "em chồng (cô)";
                    bCallA = bIsOlder ? "em dâu" : "chị dâu";
                }
            } else { // spouse là vợ
                if (personB.isMale()) {
                    aCallB = bIsOlder ? "anh vợ" : "em vợ";
                    bCallA = bIsOlder ? "em rể" : "anh rể";
                } else {
                    aCallB = bIsOlder ? "chị vợ" : "em vợ";
                    bCallA = bIsOlder ? "em rể" : "anh rể";
                }
            }
            return new String[]{aCallB, bCallA};
        }

        // B là con riêng của spouse (d1=0, d2=1)
        if (d1 == 0 && d2 == 1) {
            String aCallB = aIsFemale ? "con chồng (con riêng)" : "con vợ (con riêng)";
            String bCallA = aIsFemale ? "mẹ kế" : "dượng (cha dượng)";
            return new String[]{aCallB, bCallA};
        }

        // Các quan hệ họ hàng khác bên chồng/vợ
        @SuppressWarnings("unchecked")
        List<String> pA = (List<String>) best.get("pathA");
        @SuppressWarnings("unchecked")
        List<String> pB = (List<String>) best.get("pathB");
        Direction sToB_dir = KinshipTermResolver.resolve(spouse, personB, d1, d2, pA, pB, graph, region);
        String side = aIsFemale ? "bên chồng" : "bên vợ";
        String aCallB = sToB_dir.getCall() + " " + side;
        String bCallA = "họ hàng bên " + (spouse.isMale() ? "dâu" : "rể");
        return new String[]{aCallB, bCallA};
    }

    private String[] buildInLawLabel2(Person personA, Person r, Person personB, String aCallR) {
        if (r == null) return null;
        boolean bIsMale = personB.isMale();
        boolean aIsMale = personA.isMale();

        String callB = null;
        String bCallA = null;

        if (aCallR.startsWith("anh")) {
            callB = bIsMale ? null : "chị dâu";
            bCallA = "em chồng";
        } else if (aCallR.startsWith("em") && (r.isMale())) {
            callB = bIsMale ? null : "em dâu";
            bCallA = aIsMale ? "anh chồng" : "chị chồng";
        } else if (aCallR.startsWith("chị")) {
            callB = bIsMale ? "anh rể" : null;
            bCallA = "em vợ";
        } else if (aCallR.startsWith("em") && (r.isFemale())) {
            callB = bIsMale ? "em rể" : null;
            bCallA = aIsMale ? "anh vợ" : "chị vợ";
        } else if (aCallR.startsWith("con") && r.isMale()) {
            callB = bIsMale ? null : "con dâu";
            bCallA = aIsMale ? "bố chồng" : "mẹ chồng";
        } else if (aCallR.startsWith("con") && r.isFemale()) {
            callB = bIsMale ? "con rể" : null;
            bCallA = aIsMale ? "bố vợ" : "mẹ vợ";
        } else if (aCallR.startsWith("chú")) {
            callB = bIsMale ? null : "thím";
            bCallA = "cháu";
        } else if (aCallR.startsWith("bác")) {
            callB = bIsMale ? null : "bác gái";
            bCallA = "cháu";
        } else if (aCallR.startsWith("cậu")) {
            callB = bIsMale ? null : "mợ";
            bCallA = "cháu";
        } else if (aCallR.startsWith("cô") || aCallR.startsWith("dì")) {
            callB = bIsMale ? "dượng" : null;
            bCallA = "cháu";
        } else if (aCallR.startsWith("cháu") && r.isMale()) {
            callB = bIsMale ? null : "cháu dâu";
            bCallA = aCallR;
        } else if (aCallR.startsWith("cháu") && r.isFemale()) {
            callB = bIsMale ? "cháu rể" : null;
            bCallA = aCallR;
        }

        if (callB == null) return null;
        return new String[]{callB, bCallA != null ? bCallA : "họ hàng"};
    }

    private KinshipResult tryInLawPattern3(Person personA, Person personB,
                                           String idA, String idB,
                                           boolean includeDivorced, List<String> notes) {
        // Sui gia: con của A cưới con của B
        for (String childA : graph.getChildren(idA)) {
            for (String childB : graph.getChildren(idB)) {
                if (graph.getDirectMarriage(childA, childB).isPresent()) {
                    Person ca = graph.getPerson(childA);
                    Person cb = graph.getPerson(childB);
                    return buildGenericInLaw(personA, personB, "thông gia (sui gia)",
                            "Con " + (ca != null ? ca.getFullName() : childA) +
                                    " và con " + (cb != null ? cb.getFullName() : childB) + " đã kết hôn",
                            idA, idB, notes);
                }
            }
        }

        // Anh em đồng hao / cột chèo / chị em dâu
        for (String spouseA : graph.getEffectiveSpouses(idA, includeDivorced)) {
            for (String spouseB : graph.getEffectiveSpouses(idB, includeDivorced)) {
                if (!spouseA.equals(spouseB)) {
                    List<Map<String, Object>> common = repo.findCommonAncestors(spouseA, spouseB);
                    if (!common.isEmpty()) {
                        Map<String, Object> best = common.get(0);
                        if ((int) best.get("d1") == 1 && (int) best.get("d2") == 1) {
                            String call = personA.isMale() ? "anh em đồng hao (cột chèo)" : "chị em dâu";
                            Marriage mA = graph.getDirectMarriage(idA, spouseA).orElse(null);
                            Marriage mB = graph.getDirectMarriage(idB, spouseB).orElse(null);
                            boolean isOld = (mA != null && mA.isDivorced()) || (mB != null && mB.isDivorced());
                            String oldSuffix = isOld ? " (cũ)" : "";

                            return buildGenericInLaw(personA, personB,
                                    call + oldSuffix,
                                    "Vợ/chồng của A và vợ/chồng của B là anh chị em ruột",
                                    idA, idB, notes);
                        }
                    }
                }
            }
        }
        return null;
    }

    private KinshipResult buildGenericInLaw(Person personA, Person personB,
                                            String call, String explanation,
                                            String idA, String idB,
                                            List<String> notes) {
        KinshipResult res = new KinshipResult();
        res.setPersonA(personA);
        res.setPersonB(personB);
        res.setCategory("IN_LAW");
        res.setSummary(call);
        res.setAToB(new Direction(call, call, call,
                personA.getFullName() + " và " + personB.getFullName() + " là " + call));
        res.setBToA(new Direction(call, call, call,
                personB.getFullName() + " và " + personA.getFullName() + " là " + call));
        res.setCommonAncestors(List.of());
        res.setExplanation(List.of("Bước 1: " + explanation));
        var path = repo.findShortestPath(idA, idB);
        res.setPath(mapPath(path));
        res.setNotes(notes);
        return res;
    }

    private List<String> buildBloodExplanation(Person personA, Person personB,
                                               int d1, int d2, String ancestorName,
                                               Direction aToB) {
        List<String> steps = new ArrayList<>();
        steps.add("Bước 1: Tìm tổ tiên chung gần nhất bằng truy vấn Cypher trên Neo4j.");
        steps.add("Bước 2: Tổ tiên chung gần nhất là " + ancestorName
                + " (cách " + personA.getFullName() + " " + d1 + " bước, cách " + personB.getFullName() + " " + d2 + " bước).");
        steps.add("Bước 3: d1=" + d1 + ", d2=" + d2 + ", g=d1-d2=" + (d1 - d2) + ".");
        steps.add("Bước 4: Tra bảng thuật ngữ → " + personA.getFullName() + " gọi " + personB.getFullName()
                + " là \"" + aToB.getCall() + "\" (" + aToB.getLabel() + ").");
        return steps;
    }

    @SuppressWarnings("unchecked")
    private KinshipResult.PathInfo mapPath(Map<String, Object> path) {
        List<String> nodes = (List<String>) path.getOrDefault("nodes", List.of());
        List<Map<String, String>> edges = (List<Map<String, String>>) path.getOrDefault("edges", List.of());
        return new KinshipResult.PathInfo(nodes, edges);
    }

    private KinshipResult.PathInfo buildEmptyPath() {
        return new KinshipResult.PathInfo(List.of(), List.of());
    }
}
