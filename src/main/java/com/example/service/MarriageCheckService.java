package com.example.service;

import com.example.engine.FamilyGraph;
import com.example.model.MarriageCheckResult;
import com.example.model.MarriageCheckResult.*;
import com.example.model.Person;
import com.example.repository.PersonRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chức năng 2: Kiểm tra kết hôn có vi phạm quan hệ huyết thống không.
 * Căn cứ: Luật HN&GĐ 2014, Điều 5 khoản 2 điểm d; Điều 3.
 */
public class MarriageCheckService {

    private final PersonRepository repo;
    private final FamilyGraph graph;

    private static final String LEGAL_BASIS = "Luật HN&GĐ 2014, Điều 5 khoản 2 điểm d; Điều 3";

    public MarriageCheckService(PersonRepository repo, FamilyGraph graph) {
        this.repo = repo;
        this.graph = graph;
    }

    /**
     * Kiểm tra A và B có thể kết hôn không (về mặt huyết thống).
     */
    public MarriageCheckResult check(String idA, String idB) {
        Person personA = graph.getPerson(idA);
        Person personB = graph.getPerson(idB);
        if (personA == null) throw new IllegalArgumentException("PERSON_NOT_FOUND:" + idA);
        if (personB == null) throw new IllegalArgumentException("PERSON_NOT_FOUND:" + idB);
        if (idA.equals(idB)) throw new IllegalArgumentException("SAME_PERSON");

        MarriageCheckResult result = new MarriageCheckResult();
        result.setLegalBasis(LEGAL_BASIS);

        // Tìm tổ tiên chung bằng Cypher (nguồn chính)
        List<Map<String, Object>> ancestors = repo.findCommonAncestors(idA, idB);

        // Phân loại huyết thống
        BloodRelation blood = classifyBloodRelation(personA, personB, ancestors, idA, idB);
        result.setBloodRelation(blood);

        // Xác định verdict
        String verdict;
        List<String> explanation = new ArrayList<>();
        explanation.add("Bước 1: Tìm tổ tiên chung giữa " + personA.getFullName() + " và " + personB.getFullName() + " bằng truy vấn Cypher.");

        if ("DIRECT_LINE".equals(blood.getType())) {
            verdict = "VIOLATION";
            explanation.add("Bước 2: Phát hiện quan hệ trực hệ (cha mẹ–con, ông bà–cháu...) — vi phạm tuyệt đối.");
            explanation.add("Bước 3: Theo Điều 5 khoản 2 điểm d, cấm kết hôn giữa những người cùng dòng máu về trực hệ.");
        } else if ("WITHIN_THREE_GENERATIONS".equals(blood.getType())) {
            verdict = "VIOLATION";
            explanation.add("Bước 2: Phát hiện huyết thống trong phạm vi 3 đời (đời " + blood.getDoi() + ").");
            explanation.add("Bước 3: Theo Điều 3 và Điều 5 khoản 2 điểm d, trong phạm vi 3 đời: cha mẹ là đời 1, anh chị em ruột là đời 2, anh chị em họ là đời 3.");
        } else if ("BEYOND_THREE_GENERATIONS".equals(blood.getType())) {
            verdict = "ALLOWED";
            explanation.add("Bước 2: Phát hiện huyết thống nhưng đã ngoài 3 đời (đời " + blood.getDoi() + ").");
            explanation.add("Bước 3: Theo pháp luật, huyết thống ngoài 3 đời không bị cấm kết hôn.");
        } else {
            verdict = "ALLOWED";
            explanation.add("Bước 2: Không tìm thấy tổ tiên chung trong dữ liệu hiện có (20 người).");
            explanation.add("Bước 3: Kết luận: không phát hiện quan hệ huyết thống. Lưu ý: kết quả chỉ có giá trị trong phạm vi dữ liệu hiện có.");
        }

        result.setVerdict(verdict);
        result.setExplanation(explanation);

        // Cảnh báo bổ sung
        result.setAdditionalWarnings(buildWarnings(personA, personB, idA, idB));

        // Đường đi
        var path = repo.findShortestPath(idA, idB);
        result.setPath(mapPath(path));

        return result;
    }

    private BloodRelation classifyBloodRelation(Person personA, Person personB,
                                                 List<Map<String, Object>> ancestors,
                                                 String idA, String idB) {
        BloodRelation blood = new BloodRelation();

        if (ancestors.isEmpty()) {
            blood.setType("NONE_FOUND");
            blood.setDoi(0); blood.setD1(0); blood.setD2(0);
            blood.setLabel("Không phát hiện huyết thống trong dữ liệu hiện có (20 người). " +
                    "Một số người kết hôn vào không có cha mẹ trong DB.");
            blood.setCommonAncestors(List.of());
            return blood;
        }

        // Lấy tổ tiên chung gần nhất
        Map<String, Object> best = ancestors.get(0);
        int d1 = (int) best.get("d1");
        int d2 = (int) best.get("d2");
        int doi = Math.max(d1, d2) + 1;  // đời = max(d1,d2)+1

        blood.setD1(d1);
        blood.setD2(d2);
        blood.setDoi(doi);
        blood.setCommonAncestors(ancestors.stream()
                .map(m -> (String) m.get("ancestorId"))
                .collect(Collectors.toList()));

        // Phân loại theo điều kiện
        if (d1 == 0 || d2 == 0) {
            // Trực hệ: một người là tổ tiên của người kia
            blood.setType("DIRECT_LINE");
            blood.setLabel("Cùng dòng máu trực hệ (cha mẹ–con, ông bà–cháu hoặc xa hơn)");
        } else if (doi <= 3) {
            blood.setType("WITHIN_THREE_GENERATIONS");
            blood.setLabel(buildGenerationLabel(d1, d2, doi));
        } else {
            blood.setType("BEYOND_THREE_GENERATIONS");
            blood.setLabel("Có huyết thống nhưng ngoài 3 đời (đời thứ " + doi + ")");
        }

        return blood;
    }

    private String buildGenerationLabel(int d1, int d2, int doi) {
        if (d1 == 1 && d2 == 1) return "Anh chị em ruột (cùng cha mẹ) — đời 2";
        if ((d1 == 1 && d2 == 0) || (d1 == 0 && d2 == 1)) return "Cha mẹ – con — đời 1";
        if (doi == 3) {
            if (d1 == 1 || d2 == 1) return "Chú/bác/cô/cậu/dì – cháu ruột — đời 3";
            return "Anh chị em họ (con của anh chị em ruột) — đời 3";
        }
        return "Trong phạm vi " + doi + " đời";
    }

    private List<Warning> buildWarnings(Person personA, Person personB, String idA, String idB) {
        List<Warning> warnings = new ArrayList<>();

        // SAME_SEX
        if (personA.getGender() != null && personA.getGender().equals(personB.getGender())) {
            warnings.add(new Warning("SAME_SEX", "WARNING",
                    "Cùng giới tính: Nhà nước không thừa nhận hôn nhân giữa những người cùng giới (Điều 8 khoản 2)"));
        }

        // UNDERAGE
        LocalDate today = LocalDate.now();
        if (personA.getDateOfBirth() != null) {
            int ageA = Period.between(personA.getDateOfBirth(), today).getYears();
            int minAgeA = personA.isMale() ? 20 : 18;
            if (ageA < minAgeA) {
                warnings.add(new Warning("UNDERAGE", "WARNING",
                        personA.getFullName() + " chưa đủ tuổi kết hôn (hiện " + ageA + " tuổi, tối thiểu " + minAgeA + ")"));
            }
        }
        if (personB.getDateOfBirth() != null) {
            int ageB = Period.between(personB.getDateOfBirth(), today).getYears();
            int minAgeB = personB.isMale() ? 20 : 18;
            if (ageB < minAgeB) {
                warnings.add(new Warning("UNDERAGE", "WARNING",
                        personB.getFullName() + " chưa đủ tuổi kết hôn (hiện " + ageB + " tuổi, tối thiểu " + minAgeB + ")"));
            }
        }

        // DECEASED
        if (personA.isDeceased()) {
            warnings.add(new Warning("DECEASED", "WARNING",
                    personA.getFullName() + " đã mất (" + personA.getDateOfDeath() + ")"));
        }
        if (personB.isDeceased()) {
            warnings.add(new Warning("DECEASED", "WARNING",
                    personB.getFullName() + " đã mất (" + personB.getDateOfDeath() + ")"));
        }

        // ALREADY_MARRIED: kiểm tra có đang có hôn nhân với người khác không
        boolean aHasActiveMarriage = graph.getMarriages(idA).stream()
                .anyMatch(m -> m.isActive() && !m.getSpouseId(idA).equals(idB));
        boolean bHasActiveMarriage = graph.getMarriages(idB).stream()
                .anyMatch(m -> m.isActive() && !m.getSpouseId(idB).equals(idA));
        if (aHasActiveMarriage) {
            warnings.add(new Warning("ALREADY_MARRIED", "WARNING",
                    personA.getFullName() + " hiện đang có hôn nhân hợp lệ với người khác"));
        }
        if (bHasActiveMarriage) {
            warnings.add(new Warning("ALREADY_MARRIED", "WARNING",
                    personB.getFullName() + " hiện đang có hôn nhân hợp lệ với người khác"));
        }

        // FORBIDDEN_IN_LAW: cha/mẹ chồng–con dâu, cha/mẹ vợ–con rể, cha dượng–con riêng, mẹ kế–con riêng
        checkForbiddenInLaw(personA, personB, idA, idB, warnings);

        // GENERATION_MISMATCH: huyết thống nhưng lệch thế hệ
        List<Map<String, Object>> ancs = repo.findCommonAncestors(idA, idB);
        if (!ancs.isEmpty()) {
            int d1 = (int) ancs.get(0).get("d1");
            int d2 = (int) ancs.get(0).get("d2");
            if (d1 != d2) {
                warnings.add(new Warning("GENERATION_MISMATCH", "INFO",
                        "Có huyết thống nhưng lệch thế hệ (g=" + (d1 - d2) + "), chỉ mang tính phong tục"));
            }
        }

        return warnings;
    }

    private void checkForbiddenInLaw(Person personA, Person personB,
                                     String idA, String idB, List<Warning> warnings) {
        // A là cha/mẹ vợ hoặc cha/mẹ chồng của B?
        // Kiểm tra: con của A (dù từ hôn nhân đã kết thúc) đã/đang kết hôn với B
        for (String childA : graph.getChildren(idA)) {
            if (graph.getDirectMarriage(childA, idB).isPresent()) {
                warnings.add(new Warning("FORBIDDEN_IN_LAW", "WARNING",
                        personA.getFullName() + " là cha/mẹ của người đã/đang kết hôn với " + personB.getFullName()
                                + " (Điều 5 khoản 2 điểm d)"));
                return;
            }
        }
        // B là cha/mẹ của người đã/đang kết hôn với A
        for (String childB : graph.getChildren(idB)) {
            if (graph.getDirectMarriage(childB, idA).isPresent()) {
                warnings.add(new Warning("FORBIDDEN_IN_LAW", "WARNING",
                        personB.getFullName() + " là cha/mẹ của người đã/đang kết hôn với " + personA.getFullName()
                                + " (Điều 5 khoản 2 điểm d)"));
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private PathInfo mapPath(Map<String, Object> path) {
        List<String> nodes = (List<String>) path.getOrDefault("nodes", List.of());
        List<Map<String, String>> edges = (List<Map<String, String>>) path.getOrDefault("edges", List.of());
        return new PathInfo(nodes, edges);
    }
}
