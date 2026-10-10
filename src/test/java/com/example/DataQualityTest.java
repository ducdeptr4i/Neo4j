package com.example;

import com.example.engine.FamilyGraph;
import com.example.model.QualityIssue;
import com.example.repository.PersonRepository;
import com.example.service.DataQualityService;
import com.example.service.MarriageCheckService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test DataQualityService với dữ liệu 20 người trong bộ nhớ.
 * Kỳ vọng: 0 ERROR, 0 WARNING với dữ liệu hiện tại.
 */
@DisplayName("Kiểm thử chất lượng dữ liệu")
class DataQualityTest {

    private static DataQualityService service;

    @BeforeAll
    static void setUp() {
        FamilyGraph graph = TestFamilyGraphBuilder.build();
        PersonRepository repo = TestFamilyGraphBuilder.buildInMemoryRepo(graph);
        MarriageCheckService marriageCheck = new MarriageCheckService(repo, graph);
        service = new DataQualityService(repo, marriageCheck);
    }

    @Test
    @DisplayName("Với dữ liệu hiện tại: 0 ERROR, 0 WARNING")
    void noErrorsOrWarnings() {
        List<QualityIssue> issues = service.runAllChecks();
        assertNotNull(issues);

        long errorCount = issues.stream().filter(i -> "ERROR".equals(i.getSeverity())).count();
        long warningCount = issues.stream().filter(i -> "WARNING".equals(i.getSeverity())).count();

        // In ra để debug nếu thất bại
        issues.stream()
                .filter(i -> !"INFO".equals(i.getSeverity()))
                .forEach(i -> System.out.println("[" + i.getSeverity() + "] " + i.getCode() + ": " + i.getMessage() + " — " + i.getItems()));

        assertEquals(0, errorCount, "Không được có ERROR với dữ liệu hiện tại");
        assertEquals(0, warningCount, "Không được có WARNING với dữ liệu hiện tại");
    }
}
