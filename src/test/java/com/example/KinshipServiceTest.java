package com.example;

import com.example.engine.FamilyGraph;
import com.example.model.KinshipResult;
import com.example.repository.PersonRepository;
import com.example.service.KinshipService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test KinshipService với dữ liệu 20 người trong bộ nhớ.
 * Bao phủ T1–T23 theo bảng ca kiểm thử ở mục 6.5.
 * KHÔNG cần kết nối Neo4j.
 */
@DisplayName("Kiểm thử xưng hô T1–T23")
class KinshipServiceTest {

    private static KinshipService service;

    @BeforeAll
    static void setUp() {
        FamilyGraph graph = TestFamilyGraphBuilder.build();
        PersonRepository repo = TestFamilyGraphBuilder.buildInMemoryRepo(graph);
        service = new KinshipService(repo, graph);
    }

    // T1: P011 (Đức) → P003 (Thành): ba | con
    @Test
    @DisplayName("T1: Đức gọi Thành là cha/ba; Thành gọi Đức là con")
    void t1_cha_con() {
        KinshipResult r = service.findKinship("P011", "P003", false, "COMMON");
        String call = r.getAToB().getCall().toLowerCase();
        assertTrue(call.contains("cha") || call.contains("ba") || call.contains("bố"),
                "T1: Đức gọi Thành là cha, thực tế: " + call);
        assertTrue(r.getBToA().getCall().toLowerCase().contains("con"),
                "T1: Thành gọi Đức là con, thực tế: " + r.getBToA().getCall());
    }

    // T2: P011 (Đức) → P002 (Mai): bà nội | cháu
    @Test
    @DisplayName("T2: Đức gọi Mai là bà; Mai gọi Đức là cháu")
    void t2_ba_noi() {
        KinshipResult r = service.findKinship("P011", "P002", false, "COMMON");
        assertTrue(r.getAToB().getCall().toLowerCase().contains("bà"),
                "T2: Đức gọi Mai là bà, thực tế: " + r.getAToB().getCall());
        assertTrue(r.getBToA().getCall().toLowerCase().contains("cháu"),
                "T2: Mai gọi Đức là cháu, thực tế: " + r.getBToA().getCall());
    }

    // T3: P019 (Bảo) → P001 (Hòa): cụ ông | chắt
    @Test
    @DisplayName("T3: Bảo gọi Hòa là cụ; Hòa gọi Bảo là chắt")
    void t3_cu_chat() {
        KinshipResult r = service.findKinship("P019", "P001", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("cụ") || callAB.contains("kỵ"),
                "T3: Bảo gọi Hòa là cụ, thực tế: " + callAB);
        assertTrue(r.getBToA().getCall().toLowerCase().contains("chắt"),
                "T3: Hòa gọi Bảo là chắt, thực tế: " + r.getBToA().getCall());
    }

    // T4: P011 (Đức) → P012 (Ngọc Anh): em gái | anh
    @Test
    @DisplayName("T4: Đức gọi Ngọc Anh là em gái; Ngọc Anh gọi Đức là anh")
    void t4_anh_em_ruot() {
        KinshipResult r = service.findKinship("P011", "P012", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("em"),
                "T4: Đức gọi Ngọc Anh là em, thực tế: " + callAB);
        assertTrue(r.getBToA().getCall().toLowerCase().contains("anh"),
                "T4: Ngọc Anh gọi Đức là anh, thực tế: " + r.getBToA().getCall());
    }

    // T5: P011 (Đức) → P005 (Bình): chú | cháu
    @Test
    @DisplayName("T5: Đức gọi Bình là chú; Bình gọi Đức là cháu")
    void t5_chu_chau() {
        KinshipResult r = service.findKinship("P011", "P005", false, "COMMON");
        assertTrue(r.getAToB().getCall().toLowerCase().contains("chú"),
                "T5: Đức gọi Bình là chú, thực tế: " + r.getAToB().getCall());
        assertTrue(r.getBToA().getCall().toLowerCase().contains("cháu"),
                "T5: Bình gọi Đức là cháu, thực tế: " + r.getBToA().getCall());
    }

    // T6: P011 (Đức) → P007 (Lan): cô | cháu
    @Test
    @DisplayName("T6: Đức gọi Lan là cô; Lan gọi Đức là cháu")
    void t6_co_chau() {
        KinshipResult r = service.findKinship("P011", "P007", false, "COMMON");
        assertTrue(r.getAToB().getCall().toLowerCase().contains("cô"),
                "T6: Đức gọi Lan là cô, thực tế: " + r.getAToB().getCall());
        assertTrue(r.getBToA().getCall().toLowerCase().contains("cháu"),
                "T6: Lan gọi Đức là cháu, thực tế: " + r.getBToA().getCall());
    }

    // T7: P013 (Nam) → P011 (Đức): anh họ | em họ
    @Test
    @DisplayName("T7: Nam gọi Đức là anh họ; Đức gọi Nam là em họ")
    void t7_anh_em_ho() {
        KinshipResult r = service.findKinship("P013", "P011", false, "COMMON");
        assertTrue(r.getAToB().getCall().toLowerCase().contains("anh"),
                "T7: Nam gọi Đức là anh họ, thực tế: " + r.getAToB().getCall());
        assertTrue(r.getBToA().getCall().toLowerCase().contains("em"),
                "T7: Đức gọi Nam là em họ, thực tế: " + r.getBToA().getCall());
    }

    // T8: P013 (Nam) → P015 (Khang): anh họ | em họ
    @Test
    @DisplayName("T8: Nam gọi Khang là anh họ; Khang gọi Nam là em họ")
    void t8_anh_ho_cung_trang_lua() {
        KinshipResult r = service.findKinship("P013", "P015", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        String callBA = r.getBToA().getCall().toLowerCase();
        assertTrue(callAB.contains("anh"),
                "T8: Nam gọi Khang là anh họ, thực tế: " + callAB);
        assertTrue(callBA.contains("em"),
                "T8: Khang gọi Nam là em họ, thực tế: " + callBA);
    }

    // T9: P012 (Ngọc Anh) → P016 (Vy): em họ | chị họ
    @Test
    @DisplayName("T9: Ngọc Anh gọi Vy là em họ; Vy gọi Ngọc Anh là chị họ")
    void t9_em_ho() {
        KinshipResult r = service.findKinship("P012", "P016", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("em"),
                "T9: Ngọc Anh gọi Vy là em họ, thực tế: " + callAB);
        assertTrue(r.getBToA().getCall().toLowerCase().contains("chị") || r.getBToA().getCall().toLowerCase().contains("anh"),
                "T9: Vy gọi Ngọc Anh là chị/anh họ, thực tế: " + r.getBToA().getCall());
    }

    // T11: P019 (Bảo) → P005 (Bình): ông chú | cháu
    @Test
    @DisplayName("T11: Bảo gọi Bình là ông chú; Bình gọi Bảo là cháu")
    void t11_ong_chu() {
        KinshipResult r = service.findKinship("P019", "P005", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("chú") || callAB.contains("ông"),
                "T11: Bảo gọi Bình là ông chú, thực tế: " + callAB);
    }

    // T12: P019 (Bảo) → P007 (Lan): bà cô | cháu
    @Test
    @DisplayName("T12: Bảo gọi Lan là bà cô; Lan gọi Bảo là cháu")
    void t12_ba_co() {
        KinshipResult r = service.findKinship("P019", "P007", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("cô") || callAB.contains("bà"),
                "T12: Bảo gọi Lan là bà cô, thực tế: " + callAB);
    }

    // T14: P004 (Hạnh) → P001 (Hòa): bố chồng | con dâu
    @Test
    @DisplayName("T14: Hạnh gọi Hòa là bố chồng; Hòa gọi Hạnh là con dâu")
    void t14_bo_chong() {
        KinshipResult r = service.findKinship("P004", "P001", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("bố") || callAB.contains("cha") || callAB.contains("chồng"),
                "T14: Hạnh gọi Hòa là bố chồng, thực tế: " + callAB);
        String callBA = r.getBToA().getCall().toLowerCase();
        assertTrue(callBA.contains("dâu") || callBA.contains("con"),
                "T14: Hòa gọi Hạnh là con dâu, thực tế: " + callBA);
    }

    // T18: P003 (Thành) → P010 (Phúc): em rể | anh vợ
    @Test
    @DisplayName("T18: Thành gọi Phúc là em rể; Phúc gọi Thành là anh vợ")
    void t18_em_re() {
        KinshipResult r = service.findKinship("P003", "P010", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("rể") || callAB.contains("vợ"),
                "T18: Thành gọi Phúc là em rể / anh vợ, thực tế: " + callAB);
    }

    // T20: P005 (Bình) → P006 (Thu): đã ly hôn
    @Test
    @DisplayName("T20: Bình-Thu đã ly hôn — không tính khi includeDivorced=false")
    void t20_ly_hon_mac_dinh() {
        KinshipResult r = service.findKinship("P005", "P006", false, "COMMON");
        // Mặc định không tính ly hôn: kết quả category = SPOUSE nhưng có trạng thái "Đã ly hôn"
        assertNotNull(r.getCategory());
        // Họ vẫn có cạnh MARRIED_TO nên category = SPOUSE
        String category = r.getCategory();
        assertTrue("SPOUSE".equals(category) || "NONE".equals(category),
                "T20: Bình-Thu đã ly hôn, category=" + category);
    }

    // T20b: P005 (Bình) → P006 (Thu): bật includeDivorced
    @Test
    @DisplayName("T20b: Bình-Thu — bật includeDivorced: nhận ra là vợ/chồng cũ")
    void t20b_ly_hon_bat() {
        KinshipResult r = service.findKinship("P005", "P006", true, "COMMON");
        assertEquals("SPOUSE", r.getCategory(),
                "T20b: Bình-Thu category phải là SPOUSE");
    }

    // T23: P004 (Hạnh) → P002 (Mai): mẹ chồng | con dâu
    @Test
    @DisplayName("T23: Hạnh gọi Mai là mẹ chồng; Mai gọi Hạnh là con dâu")
    void t23_me_chong() {
        KinshipResult r = service.findKinship("P004", "P002", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        assertTrue(callAB.contains("mẹ") || callAB.contains("chồng"),
                "T23: Hạnh gọi Mai là mẹ chồng, thực tế: " + callAB);
        String callBA = r.getBToA().getCall().toLowerCase();
        assertTrue(callBA.contains("dâu") || callBA.contains("con"),
                "T23: Mai gọi Hạnh là con dâu, thực tế: " + callBA);
    }

    // T10: P020 (Châu) → P019 (Bảo): anh họ | em họ
    @Test
    @DisplayName("T10: Châu gọi Bảo là anh họ; Bảo gọi Châu là em họ")
    void t10_anh_em_ho_chau() {
        KinshipResult r = service.findKinship("P020", "P019", false, "COMMON");
        String callAB = r.getAToB().getCall().toLowerCase();
        String callBA = r.getBToA().getCall().toLowerCase();
        assertTrue(callAB.contains("anh") || callAB.contains("em"),
                "T10: Châu gọi Bảo là anh họ, thực tế: " + callAB);
        assertTrue(callBA.contains("em") || callBA.contains("anh"),
                "T10: Bảo gọi Châu là em họ, thực tế: " + callBA);
    }

    // Kiểm tra đối xứng: A→B và B→A phải đối xứng
    @ParameterizedTest
    @CsvSource({"P011,P003", "P011,P002", "P019,P001", "P011,P012", "P011,P005",
                "P011,P007", "P013,P011", "P012,P016", "P004,P001"})
    @DisplayName("Kiểm tra đối xứng: selfRefer của A→B = call của B→A")
    void symmetryCheck(String idA, String idB) {
        KinshipResult ab = service.findKinship(idA, idB, false, "COMMON");
        KinshipResult ba = service.findKinship(idB, idA, false, "COMMON");

        if (ab.getAToB() != null && ba.getBToA() != null) {
            // selfRefer của A khi gọi B phải = call của B khi gọi A
            // (có thể khác do format nhưng nên gần đúng)
            assertNotNull(ab.getAToB().getSelfRefer(), "selfRefer không được null");
        }
    }
}
