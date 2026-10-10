package com.example;

import com.example.engine.FamilyGraph;
import com.example.model.MarriageCheckResult;
import com.example.repository.PersonRepository;
import com.example.service.MarriageCheckService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test MarriageCheckService với dữ liệu 20 người trong bộ nhớ.
 * Bao phủ M1–M11 theo bảng ca kiểm thử ở mục 7.4.
 * KHÔNG cần kết nối Neo4j.
 */
@DisplayName("Kiểm thử hôn nhân M1–M11")
class MarriageCheckServiceTest {

    private static MarriageCheckService service;

    @BeforeAll
    static void setUp() {
        FamilyGraph graph = TestFamilyGraphBuilder.build();
        PersonRepository repo = TestFamilyGraphBuilder.buildInMemoryRepo(graph);
        service = new MarriageCheckService(repo, graph);
    }

    private void assertViolation(String idA, String idB, String caseLabel) {
        MarriageCheckResult r = service.check(idA, idB);
        assertEquals("VIOLATION", r.getVerdict(),
                caseLabel + ": kỳ vọng VIOLATION, thực tế " + r.getVerdict()
                        + " — " + (r.getBloodRelation() != null ? r.getBloodRelation().getLabel() : "null"));
    }

    // M1: P003 (Thành) – P012 (Ngọc Anh): ❌ VIOLATION, trực hệ cha–con
    @Test @DisplayName("M1: Thành–Ngọc Anh: VIOLATION (trực hệ cha–con)")
    void m1_cha_con() { assertViolation("P003", "P012", "M1"); }

    // M2: P001 (Hòa) – P011 (Đức): ❌ VIOLATION, trực hệ ông–cháu
    @Test @DisplayName("M2: Hòa–Đức: VIOLATION (trực hệ ông–cháu)")
    void m2_ong_chau() { assertViolation("P001", "P011", "M2"); }

    // M3: P011 (Đức) – P012 (Ngọc Anh): ❌ VIOLATION, anh chị em ruột (đời 2)
    @Test @DisplayName("M3: Đức–Ngọc Anh: VIOLATION (anh chị em ruột, đời 2)")
    void m3_anh_chi_em_ruot() { assertViolation("P011", "P012", "M3"); }

    // M4: P005 (Bình) – P012 (Ngọc Anh): ❌ VIOLATION, chú–cháu ruột (đời 3)
    @Test @DisplayName("M4: Bình–Ngọc Anh: VIOLATION (chú–cháu ruột, đời 3)")
    void m4_chu_chau() { assertViolation("P005", "P012", "M4"); }

    // M5: P011 (Đức) – P016 (Vy): ❌ VIOLATION, anh chị em họ (đời 3)
    @Test @DisplayName("M5: Đức–Vy: VIOLATION (anh chị em họ, đời 3)")
    void m5_anh_em_ho() { assertViolation("P011", "P016", "M5"); }

    // M6: P015 (Khang) – P012 (Ngọc Anh): ❌ VIOLATION, anh chị em họ (đời 3)
    @Test @DisplayName("M6: Khang–Ngọc Anh: VIOLATION (anh chị em họ, đời 3)")
    void m6_khang_ngoc_anh() { assertViolation("P015", "P012", "M6"); }

    // M7: P019 (Bảo) – P020 (Châu): ❌ VIOLATION, anh chị em họ (đời 3)
    @Test @DisplayName("M7: Bảo–Châu: VIOLATION (anh chị em họ, đời 3)")
    void m7_bao_chau() { assertViolation("P019", "P020", "M7"); }

    // M8: P014 (Hà) – P019 (Bảo): ✅ ALLOWED, huyết thống đời 4
    @Test @DisplayName("M8: Hà–Bảo: ALLOWED (huyết thống đời 4)")
    void m8_doi4() {
        MarriageCheckResult r = service.check("P014", "P019");
        assertEquals("ALLOWED", r.getVerdict(), "M8: kỳ vọng ALLOWED");
        // Kiểm tra có cảnh báo UNDERAGE hoặc GENERATION_MISMATCH
        assertNotNull(r.getAdditionalWarnings(), "M8: cần có cảnh báo bổ sung");
    }

    // M9: P004 (Hạnh) – P018 (Sơn): ✅ ALLOWED, không huyết thống; cảnh báo FORBIDDEN_IN_LAW
    @Test @DisplayName("M9: Hạnh–Sơn: ALLOWED (không huyết thống); cảnh báo FORBIDDEN_IN_LAW")
    void m9_forbidden_in_law() {
        MarriageCheckResult r = service.check("P004", "P018");
        assertEquals("ALLOWED", r.getVerdict(), "M9: kỳ vọng ALLOWED");
        boolean hasForbiddenInLaw = r.getAdditionalWarnings().stream()
                .anyMatch(w -> "FORBIDDEN_IN_LAW".equals(w.getCode()));
        assertTrue(hasForbiddenInLaw, "M9: cần có cảnh báo FORBIDDEN_IN_LAW (mẹ vợ–con rể)");
    }

    // M10: P015 (Khang) – P017 (Ngọc): ✅ ALLOWED; cảnh báo ALREADY_MARRIED
    @Test @DisplayName("M10: Khang–Ngọc: ALLOWED; cảnh báo ALREADY_MARRIED")
    void m10_already_married() {
        MarriageCheckResult r = service.check("P015", "P017");
        assertEquals("ALLOWED", r.getVerdict(), "M10: kỳ vọng ALLOWED");
        boolean hasAlreadyMarried = r.getAdditionalWarnings().stream()
                .anyMatch(w -> "ALREADY_MARRIED".equals(w.getCode()));
        assertTrue(hasAlreadyMarried, "M10: cần có cảnh báo ALREADY_MARRIED");
    }

    // M11: P002 (Mai) – P018 (Sơn): ✅ ALLOWED; cùng họ Lê nhưng không có quan hệ
    @Test @DisplayName("M11: Mai–Sơn: ALLOWED (cùng họ Lê nhưng không có quan hệ huyết thống trong DB)")
    void m11_same_surname() {
        MarriageCheckResult r = service.check("P002", "P018");
        assertEquals("ALLOWED", r.getVerdict(), "M11: kỳ vọng ALLOWED");
        assertEquals("NONE_FOUND", r.getBloodRelation().getType(),
                "M11: không có huyết thống trong DB");
    }

    // Kiểm tra đối xứng: check(A,B) và check(B,A) phải có cùng verdict
    @Test @DisplayName("Đối xứng: check(A,B) == check(B,A) cho M1-M7")
    void symmetryViolations() {
        String[][] pairs = {{"P003","P012"},{"P001","P011"},{"P011","P012"},{"P005","P012"},{"P011","P016"},{"P015","P012"},{"P019","P020"}};
        for (String[] pair : pairs) {
            MarriageCheckResult ab = service.check(pair[0], pair[1]);
            MarriageCheckResult ba = service.check(pair[1], pair[0]);
            assertEquals(ab.getVerdict(), ba.getVerdict(),
                    "Đối xứng: " + pair[0] + "-" + pair[1]);
        }
    }
}
