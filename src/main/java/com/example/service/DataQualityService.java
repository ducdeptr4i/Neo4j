package com.example.service;

import com.example.model.QualityIssue;
import com.example.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kiểm tra chất lượng dữ liệu theo Q1–Q11.
 * Với dữ liệu hiện tại kỳ vọng: 0 ERROR, 0 WARNING, một vài INFO.
 */
public class DataQualityService {

    private static final Logger log = LoggerFactory.getLogger(DataQualityService.class);

    private final PersonRepository repo;
    private final MarriageCheckService marriageCheckService;

    public DataQualityService(PersonRepository repo, MarriageCheckService marriageCheckService) {
        this.repo = repo;
        this.marriageCheckService = marriageCheckService;
    }

    public List<QualityIssue> runAllChecks() {
        List<QualityIssue> issues = new ArrayList<>();

        issues.add(checkQ1());
        issues.add(checkQ2());
        issues.add(checkQ3());
        issues.add(checkQ4());
        issues.addAll(checkQ5());
        issues.add(checkQ6());
        issues.add(checkQ7());
        issues.add(checkQ8());
        issues.add(checkQ9());
        issues.add(checkQ10());
        issues.add(checkQ11());

        log.info("Kiểm tra chất lượng dữ liệu: {} mục", issues.size());
        return issues;
    }

    private QualityIssue checkQ1() {
        List<String> items = repo.q1MissingRequiredFields();
        return new QualityIssue(
            "Q1", items.isEmpty() ? "INFO" : "ERROR",
            "Thiếu fullName/dateOfBirth hoặc gender không hợp lệ",
            items
        );
    }

    private QualityIssue checkQ2() {
        List<String> items = repo.q2MoreThanTwoParents();
        return new QualityIssue(
            "Q2", items.isEmpty() ? "INFO" : "ERROR",
            "Người có hơn 2 cha mẹ ruột",
            items
        );
    }

    private QualityIssue checkQ3() {
        List<String> items = repo.q3SameGenderParents();
        return new QualityIssue(
            "Q3", items.isEmpty() ? "INFO" : "ERROR",
            "Hai cha mẹ cùng giới",
            items
        );
    }

    private QualityIssue checkQ4() {
        List<String> items = repo.q4AncestorCycle();
        return new QualityIssue(
            "Q4", items.isEmpty() ? "INFO" : "ERROR",
            "Có vòng lặp tổ tiên (không phải DAG)",
            items
        );
    }

    private List<QualityIssue> checkQ5() {
        List<Map<String, Object>> records = repo.q5AgeDifference();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (Map<String, Object> r : records) {
            int gap = (int) r.get("gap");
            String desc = r.get("parentId") + " → " + r.get("childId") + " (chênh " + gap + " năm)";
            if (gap < 0) errors.add(desc);
            else warnings.add(desc);
        }

        List<QualityIssue> result = new ArrayList<>();
        result.add(new QualityIssue("Q5a", errors.isEmpty() ? "INFO" : "ERROR",
            "Cha/mẹ sinh sau con", errors));
        result.add(new QualityIssue("Q5b", warnings.isEmpty() ? "INFO" : "WARNING",
            "Chênh tuổi cha/mẹ – con < 15 năm", warnings));
        return result;
    }

    private QualityIssue checkQ6() {
        List<String> items = repo.q6MarriageErrors();
        return new QualityIssue(
            "Q6", items.isEmpty() ? "INFO" : "ERROR",
            "Hôn nhân lỗi: tự cưới mình / endDate < startDate / kết hôn trước khi sinh",
            items
        );
    }

    private QualityIssue checkQ7() {
        List<String> items = repo.q7DeathConsistency();
        return new QualityIssue(
            "Q7", items.isEmpty() ? "INFO" : "ERROR",
            "endReason=DEATH không khớp dateOfDeath, hoặc hôn nhân còn hiệu lực nhưng một người đã mất",
            items
        );
    }

    private QualityIssue checkQ8() {
        List<String> items = repo.q8OverlappingMarriages();
        return new QualityIssue(
            "Q8", items.isEmpty() ? "INFO" : "ERROR",
            "Một người có 2 hôn nhân chồng lấn thời gian",
            items
        );
    }

    private QualityIssue checkQ9() {
        // Kiểm toán: các cặp hôn nhân hiện có xem có vi phạm huyết thống không
        List<String> violations = new ArrayList<>();
        try {
            var marriages = repo.findAllMarriages();
            for (var m : marriages) {
                try {
                    var checkResult = marriageCheckService.check(m.getPersonId1(), m.getPersonId2());
                    if ("VIOLATION".equals(checkResult.getVerdict())) {
                        violations.add(m.getPersonId1() + "-" + m.getPersonId2());
                    }
                } catch (Exception e) {
                    log.warn("Q9: lỗi kiểm tra {} - {}: {}", m.getPersonId1(), m.getPersonId2(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Q9: lỗi truy vấn hôn nhân: {}", e.getMessage());
        }
        return new QualityIssue(
            "Q9", violations.isEmpty() ? "INFO" : "ERROR",
            "Kiểm toán: cặp vợ chồng hiện có vi phạm huyết thống",
            violations
        );
    }

    private QualityIssue checkQ10() {
        List<Map<String, Object>> items = repo.q10IsolatedOrNoParents();
        List<String> desc = items.stream()
            .map(m -> m.get("personId") + " (" + m.get("type") + ")")
            .collect(Collectors.toList());
        return new QualityIssue(
            "Q10", "INFO",
            "Người không có quan hệ nào hoặc kết hôn vào nhưng không có cha mẹ trong DB",
            desc
        );
    }

    private QualityIssue checkQ11() {
        List<String> items = repo.q11ChildBeforeMarriage();
        return new QualityIssue(
            "Q11", "INFO",
            "Con sinh trước ngày kết hôn của cha mẹ",
            items
        );
    }
}
